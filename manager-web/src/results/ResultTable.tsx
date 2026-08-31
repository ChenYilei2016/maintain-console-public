import {useMemo, useState} from 'react';
import type {ResultBlock} from '../types';
import './results.css';

const PAGE_SIZE = 20;
const collator = new Intl.Collator('zh-CN', {numeric: true, sensitivity: 'base'});
const text = (value: unknown) => value == null ? '' : typeof value === 'object' ? JSON.stringify(value) : String(value);

/** CSV 与剪贴板共用转义规则；字符串公式被加前缀，数值类型保持数值。 */
export function tableCsv(columns: string[], rows: unknown[][]): string {
    return [columns, ...rows].map(row => row.map(value => {
        const cell = text(value);
        const safe = typeof value !== 'number' && /^[\s\uFEFF]*[=+@-]/u.test(cell) ? "'" + cell : cell;
        return '"' + safe.replaceAll('"', '""') + '"';
    }).join(',')).join('\r\n');
}

export default function ResultTable({block}: { block: ResultBlock }) {
    const table = block.data as {
        columns?: unknown[];
        rows?: unknown[];
        truncated?: boolean;
        returnedRowCount?: number
    };
    const [filter, setFilter] = useState('');
    const [sort, setSort] = useState<{ column: number; ascending: boolean }>();
    const [page, setPage] = useState(0);
    const [notice, setNotice] = useState('');
    const columns = useMemo(() => Array.isArray(table?.columns) ? table.columns.map(String) : [], [table]);
    const rows = useMemo(() => Array.isArray(table?.rows) ? table.rows.map(row => columns.map((column, index) =>
        Array.isArray(row) ? row[index] : (row as Record<string, unknown>)?.[column])) : [], [columns, table]);
    const visible = useMemo(() => {
        const matches = rows.filter(row => row.some(value => text(value).toLocaleLowerCase().includes(filter.toLocaleLowerCase())));
        if (sort) matches.sort((left, right) => {
            const a = left[sort.column], b = right[sort.column];
            const comparison = typeof a === 'number' && typeof b === 'number' ? a - b : collator.compare(text(a), text(b));
            return sort.ascending ? comparison : -comparison;
        });
        return matches;
    }, [rows, filter, sort]);
    const lastPage = Math.max(0, Math.ceil(visible.length / PAGE_SIZE) - 1);
    const currentPage = Math.min(page, lastPage);
    if (!columns.length) return <pre className="console-output">{JSON.stringify(block.data, null, 2)}</pre>;
    return <div className="result-table">
        <div className="result-table-tools"><input aria-label="筛选当前返回数据" placeholder="筛选当前返回数据…"
                                                   value={filter} onChange={event => {
            setFilter(event.target.value);
            setPage(0);
        }}/>
            <button type="button" onClick={async () => {
                try {
                    await navigator.clipboard.writeText(tableCsv(columns, visible));
                    setNotice(`已复制当前筛选的 ${visible.length} 行`);
                } catch {
                    setNotice('剪贴板不可用，请使用 CSV 导出');
                }
            }}>复制
            </button>
            <button type="button" onClick={() => {
                const url = URL.createObjectURL(new Blob(['\uFEFF', tableCsv(columns, visible)], {type: 'text/csv;charset=utf-8'}));
                const link = document.createElement('a');
                link.href = url;
                link.download = 'maintain-console-result.csv';
                link.click();
                window.setTimeout(() => URL.revokeObjectURL(url), 1000);
            }}>导出当前筛选 CSV
            </button>
        </div>
        <p className="result-data-scope">当前返回 {rows.length} 行 ·
            筛选后 {visible.length} 行；筛选、排序和导出仅针对本次返回，不代表完整查询数据。</p>
        {table.truncated && <p className="safety-note">结果已截断：Client
            返回 {table.returnedRowCount ?? '更多'} 行，当前最多保留 {rows.length} 行。</p>}
        {notice && <p role="status" className="result-data-scope">{notice}</p>}
        <div className="table-scroll">
            <table>
                <thead>
                <tr>{columns.map((column, index) => <th key={index}
                                                        aria-sort={sort?.column === index ? sort.ascending ? 'ascending' : 'descending' : 'none'}>
                    <button type="button" onClick={() => {
                        setSort({column: index, ascending: sort?.column !== index || !sort.ascending});
                        setPage(0);
                    }}>{column}{sort?.column === index ? sort.ascending ? ' ↑' : ' ↓' : ' ↕'}</button>
                </th>)}</tr>
                </thead>
                <tbody>{visible.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE).map((row, rowIndex) => <tr
                    key={rowIndex}>
                    {row.map((value, index) => <td key={index}>{text(value)}</td>)}</tr>)}
                {!visible.length && <tr>
                    <td colSpan={columns.length}>{rows.length ? '没有符合当前筛选的记录' : '本次查询没有返回数据'}</td>
                </tr>}
                </tbody>
            </table>
        </div>
        <footer className="result-table-pages">
            <button type="button" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>上一页</button>
            <span>{currentPage + 1} / {lastPage + 1} · 每页 {PAGE_SIZE} 行</span>
            <button type="button" disabled={currentPage === lastPage} onClick={() => setPage(currentPage + 1)}>下一页
            </button>
        </footer>
    </div>;
}
