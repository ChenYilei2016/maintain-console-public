import {useEffect, useState} from 'react';
import {api} from '../api';
import Modal from '../Modal';
import ResultRenderer from '../ResultRenderer';
import type {ExecutionHistory, ScriptExecutionResult} from '../types';
import {type ExecutionReport, OUTCOME_LABELS} from './execution';

export default function ExecutionHistoryModal({scriptId, onRestore, onClose}: {
    scriptId: string; onRestore: (values: Record<string, unknown>) => void; onClose: () => void;
}) {
    const [page, setPage] = useState(1);
    const [items, setItems] = useState<ExecutionHistory[]>([]);
    const [total, setTotal] = useState(0);
    const [selected, setSelected] = useState<ExecutionHistory>();
    const [error, setError] = useState('');
    useEffect(() => {
        let active = true;
        api.getHistory(scriptId, page, 10).then(response => {
            if (active) {
                setItems(response.data);
                setTotal(response.totalElements);
            }
        }).catch(failure => {
            if (active) setError(failure.message);
        });
        return () => {
            active = false;
        };
    }, [scriptId, page]);
    let result: ScriptExecutionResult | string = selected?.result || selected?.errorMessage || '无结果';
    try {
        if (selected?.resultPayload) result = JSON.parse(selected.resultPayload);
    } catch { /* 兼容旧文本结果。 */
    }
    let targets: ExecutionReport['targets'] = [];
    try {
        const stored = JSON.parse(selected?.targetsJson || '[]');
        if (Array.isArray(stored)) targets = stored.filter(item => item && typeof item.host === 'string');
    } catch { /* 旧记录没有结构化目标时仍展示原结果。 */
    }
    return <Modal title="执行历史" wide onClose={onClose}>
        <p>默认只展示自己的记录；创建者和管理员可查看其管理工具的记录。回填参数不会再次执行。</p>
        {error && <p role="alert" className="safety-note">{error}</p>}
        {selected ? <div className="history-detail">
            <div className="panel-actions">
                <button onClick={() => setSelected(undefined)}>← 返回列表</button>
                <button onClick={() => {
                    try {
                        onRestore(JSON.parse(selected.parameters || '{}'));
                        onClose();
                    } catch {
                        setError('该记录的参数格式无法回填');
                    }
                }}>回填参数，不执行
                </button>
            </div>
            <p>{selected.executorName}（{selected.executorId}） · {selected.environment || '旧记录未记录环境'} ·
                v{selected.scriptVersion ?? '—'} · {selected.duration} ms</p>
            <p>{selected.startTime.replace('T', ' ')} · {selected.draft ? '草稿调试' : '工具运行'} · {selected.outcome || selected.status}</p>
            <h3>参数（敏感值已脱敏）</h3>
            <pre>{selected.parameters || '无参数'}</pre>
            {targets.length ? targets.map(target => <section key={target.instanceId}>
                <h3>{target.host}:{target.port} · {OUTCOME_LABELS[target.outcome] || target.outcome} · {target.duration} ms</h3>
                {target.message && <p className="safety-note">{target.message}</p>}
                {target.result && <ResultRenderer result={target.result}/>}
            </section>) : <ResultRenderer result={result}/>}
        </div> : <>
            <div className="history-list">{items.map(item => <button type="button" key={item.id} className="history-row"
                                                                     onClick={() => setSelected(item)}>
                <strong>{item.executorName} · {item.outcome || item.status}</strong><span>{item.environment || '旧记录'} · v{item.scriptVersion ?? '—'} · {item.duration} ms</span>
                <small>{item.startTime.replace('T', ' ')}</small>
            </button>)}</div>
            {!items.length && <p className="inline-empty">暂无可查看的执行记录</p>}
            <footer className="catalog-pagination">
                <button disabled={page === 1} onClick={() => setPage(page - 1)}>上一页</button>
                <span>{page} / {Math.max(1, Math.ceil(total / 10))}</span>
                <button disabled={page * 10 >= total} onClick={() => setPage(page + 1)}>下一页</button>
            </footer>
        </>}
    </Modal>;
}
