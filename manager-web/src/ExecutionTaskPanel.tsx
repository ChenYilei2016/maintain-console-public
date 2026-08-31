import ResultRenderer from './ResultRenderer';
import type {ExecutionTask} from './types';

export default function ExecutionTaskPanel({task}: { task: ExecutionTask }) {
    return <div className="task-result">
        <details className="task-details">
            <summary>执行详情 · {task.id}</summary>
            <div className="task-summary">
            <span><small>任务</small><code>{task.id}</code></span>
            <span><small>状态</small><b className={`status-text ${task.status.toLowerCase()}`}>{task.status}</b></span>
            <span><small>目标</small>{task.targets.length} 个实例</span>
            <span><small>耗时</small>{task.duration == null ? '—' : `${task.duration} ms`}</span>
            </div>
        </details>
        {task.errorMessage && <p className="task-error">{task.errorMessage}</p>}
        <div className="target-results">{task.targets.map((target) => <section className="target-result"
                                                                               key={target.instance.id}>
            <header>
                <span><strong>{target.instance.id}</strong><small>{target.instance.host}:{target.instance.port}</small></span>
                <b className={`status-pill ${target.status.toLowerCase()}`}>{target.status}</b>
            </header>
            {target.result && <ResultRenderer result={target.result}/>}
            {target.errorMessage && <pre className="console-output error">{target.errorMessage}</pre>}
        </section>)}</div>
    </div>;
}
