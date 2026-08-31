import ResultRenderer from '../ResultRenderer';
import type {ExecutionReport} from './execution';
import {OUTCOME_LABELS} from './execution';

export default function ExecutionOutput({report, error, running, elapsed}: {
    report?: ExecutionReport; error: string; running: boolean; elapsed: number;
}) {
    if (running) return <div className="execution-feedback" role="status"><strong>执行中
        · {(elapsed / 1000).toFixed(1)} 秒</strong>
        <p>正在等待当前请求返回。离开页面不会终止远端操作；没有自动重试。</p></div>;
    if (error) return <div className="execution-feedback error" role="alert">{error}</div>;
    if (!report) return <div className="execution-feedback"><strong>结果将在这里展示</strong>
        <p>填写参数并运行工具；打开页面不会自动执行。</p></div>;
    return <div className="execution-output">
        <header>
            <strong>{OUTCOME_LABELS[report.outcome]}</strong><span>v{report.scriptVersion} · {report.duration} ms · {report.draft ? '草稿调试' : '已保存工具'}</span>
        </header>
        {report.warning && <p className="safety-note">{report.warning}</p>}
        {report.targets.map(target => <section key={target.instanceId}>
            <h3>{target.host}:{target.port} · {OUTCOME_LABELS[target.outcome]}</h3>
            {target.message && <p className="safety-note">{target.message}</p>}
            {target.result && <ResultRenderer result={target.result}/>}
        </section>)}
    </div>;
}
