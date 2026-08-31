import {useState} from 'react';
import ResultTable from './results/ResultTable';
import type {ResultBlock, ScriptExecutionResult} from './types';

function json(value: unknown) {
    return JSON.stringify(value, null, 2);
}

function JsonBlock({value}: { value: unknown }) {
    const [notice, setNotice] = useState('');
    return <div className="json-result">
        <button type="button" onClick={async () => {
            try {
                await navigator.clipboard.writeText(json(value));
                setNotice('已复制 JSON');
            } catch {
                setNotice('剪贴板不可用');
            }
        }}>复制 JSON
        </button>
        {notice && <small role="status">{notice}</small>}
        <pre className="console-output">{json(value)}</pre>
    </div>;
}

function MetricBlock({block}: { block: ResultBlock }) {
    const values: Array<[string, unknown]> = block.data && typeof block.data === 'object'
        ? Object.entries(block.data)
        : [['value', block.data]];
    return <div className="metric-grid">{values.map(([label, value]) => <div key={label}>
        <small>{label}</small><strong>{String(value ?? '—')}</strong>
    </div>)}</div>;
}

const CHART_COLORS = ['#526df5', '#20a66a', '#f59e0b', '#e05273', '#7c5ce0'];

function ChartBlock({block}: { block: ResultBlock }) {
    const chart = block.data as {
        chartType?: string;
        labels?: unknown[];
        series?: Array<{ name?: string; data?: unknown[] }>;
        items?: Array<{ name?: string; value?: unknown }>;
    };
    const chartType = chart?.chartType?.toLowerCase();
    const labels = Array.isArray(chart?.labels) ? chart.labels.map(String) : [];
    const series = Array.isArray(chart?.series) ? chart.series.map((item) => ({
        name: item.name || '数据',
        data: Array.isArray(item.data) ? item.data : [],
    })) : [];
    const pieItems = Array.isArray(chart?.items) ? chart.items.map((item) => ({
        name: String(item.name || '未命名'),
        value: Number(item.value) || 0,
    })).filter((item) => item.value >= 0) : [];
    if (chartType === 'pie' && pieItems.length) {
        const total = pieItems.reduce((sum, item) => sum + item.value, 0);
        let offset = 0;
        const stops = pieItems.map((item, index) => {
            const start = offset;
            offset += total ? item.value / total * 100 : 0;
            return `${CHART_COLORS[index % CHART_COLORS.length]} ${start}% ${offset}%`;
        });
        return <div className="pie-chart">
            <div className="pie-visual"
                 style={{background: `conic-gradient(${stops.join(',')})`}}/>
            <div className="chart-legend">{pieItems.map((item, index) => <span key={`${item.name}-${index}`}>
                <i style={{background: CHART_COLORS[index % CHART_COLORS.length]}}/>{item.name}
                <b>{item.value}</b></span>)}</div>
        </div>;
    }
    if (!series.length || !series.some((item) => item.data.length)) {
        return <pre className="console-output">{json(block.data)}</pre>;
    }

    const width = 640;
    const height = 240;
    const padding = 34;
    const numericSeries = series.map((item) => ({
        ...item,
        data: item.data.map(Number).map((value) => Number.isFinite(value) ? value : 0)
    }));
    const values = numericSeries.flatMap((item) => item.data);
    const min = Math.min(0, ...values);
    const max = Math.max(1, ...values);
    const y = (value: number) => height - padding - (value - min) / (max - min || 1) * (height - padding * 2);
    const x = (index: number, count: number) => padding + index * (width - padding * 2) / Math.max(1, count - 1);

    return <div className="chart-wrap">
        <svg viewBox={`0 0 ${width} ${height}`} role="img"
             aria-label={`${chartType || 'line'} chart`}>
            <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} className="chart-axis"/>
            {chartType === 'bar' ? numericSeries.flatMap((item, seriesIndex) => item.data.map((value, index) => {
                const groupWidth = (width - padding * 2) / Math.max(1, item.data.length);
                const barWidth = Math.max(2, groupWidth / Math.max(1, numericSeries.length) - 4);
                return <rect key={`${seriesIndex}-${index}`}
                             x={padding + index * groupWidth + seriesIndex * (barWidth + 2)}
                             y={y(value)} width={barWidth} height={Math.max(1, height - padding - y(value))}
                             fill={CHART_COLORS[seriesIndex % CHART_COLORS.length]}/>;
            })) : numericSeries.map((item, seriesIndex) => {
                const points = item.data.map((value, index) => `${x(index, item.data.length)},${y(value)}`).join(' ');
                return chartType === 'scatter'
                    ? <g key={seriesIndex}>{item.data.map((value, index) => <circle key={index}
                                                                                    cx={x(index, item.data.length)}
                                                                                    cy={y(value)} r="4"
                                                                                    fill={CHART_COLORS[seriesIndex % CHART_COLORS.length]}/>)}</g>
                    : <g key={seriesIndex}>{chartType === 'area' && <polygon
                        points={`${padding},${height - padding} ${points} ${width - padding},${height - padding}`}
                        fill={CHART_COLORS[seriesIndex % CHART_COLORS.length]} opacity="0.16"/>}
                        <polyline points={points} fill="none" stroke={CHART_COLORS[seriesIndex % CHART_COLORS.length]}
                                  strokeWidth="3" strokeLinejoin="round" strokeLinecap="round"/>
                    </g>;
            })}
            {labels.slice(0, 8).map((label, index) => <text key={index}
                                                            x={x(index, Math.min(labels.length, 8))} y={height - 10}
                                                            textAnchor="middle">{label}</text>)}
        </svg>
        <div className="chart-legend">{numericSeries.map((item, index) => <span key={`${item.name}-${index}`}>
        <i style={{background: CHART_COLORS[index % CHART_COLORS.length]}}/>{item.name}</span>)}</div>
    </div>;
}

function FileBlock({block}: { block: ResultBlock }) {
    const file = block.data as {
        name?: string;
        url?: string;
        size?: unknown;
        mimeType?: string;
        contentBase64?: string
    };
    const inlineUrl = file?.contentBase64 && file.mimeType
        ? `data:${file.mimeType};base64,${file.contentBase64}`
        : undefined;
    const url = file?.url?.startsWith('/manager/files/') ? file.url : inlineUrl;
    if (!url) {
        return <pre className="console-output">{json(block.data)}</pre>;
    }
    return <a className="file-result" href={url} download={file.name || undefined}>
        <span><strong>{file.name || '下载文件'}</strong><small>{file.mimeType || '未知类型'}
            {file.size == null ? '' : ` · ${String(file.size)} bytes`}</small></span><b>下载</b>
    </a>;
}

function Block({block}: { block: ResultBlock }) {
    const title = block.title && <h3>{block.title}</h3>;
    if (block.type === 'table') return <section className="result-block">{title}<ResultTable block={block}/></section>;
    if (block.type === 'metric') return <section className="result-block">{title}<MetricBlock block={block}/></section>;
    if (block.type === 'chart') return <section className="result-block">{title}<ChartBlock block={block}/></section>;
    if (block.type === 'file') return <section className="result-block">{title}<FileBlock block={block}/></section>;
    if (block.type === 'json') {
        return <section className="result-block">{title}
            <JsonBlock value={block.data}/>
        </section>;
    }
    return <section className={`result-block ${block.type}`}>{title}
        <pre className="console-output">{String(block.data ?? '')}</pre>
    </section>;
}

export default function ResultRenderer({result}: { result: ScriptExecutionResult | string }) {
    if (typeof result === 'string') return <pre className="console-output">{result}</pre>;
    if (!result.blocks?.length) return <p className="inline-empty">执行已结束，没有返回内容。</p>;
    return <div className="result-blocks">{result.blocks.map((block, index) =>
        <Block key={`${block.type}-${index}`} block={block}/>)}</div>;
}
