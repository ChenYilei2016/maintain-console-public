import type {ComponentProps} from 'react';
import ExecutionOutput from '../execution/ExecutionOutput';
import {OUTCOME_LABELS} from '../execution/execution';

export type ResultView = 'open' | 'maximized';

interface Props {
    execution: ComponentProps<typeof ExecutionOutput>;
    resultView: ResultView;
    onViewChange: (view: ResultView) => void;
}

export default function ExecutionResultsPanel({execution, resultView, onViewChange}: Props) {
    return <section className="panel workbench-results" aria-label="执行结果区">
        <header className="section-title">
            <span>执行结果</span>
            <small className="result-summary">{execution.running ? '正在等待当前请求…' : execution.report
                ? OUTCOME_LABELS[execution.report.outcome] + ' · ' + execution.report.duration + ' ms' : execution.error || '尚未执行'}</small>
            <div className="result-actions panel-actions">
                <button type="button" onClick={() => onViewChange(resultView === 'maximized' ? 'open' : 'maximized')}>
                    {resultView === 'maximized' ? '还原结果区' : '最大化结果'}</button>
            </div>
        </header>
        <div className="result-panel-body" id="execution-results">
            <ExecutionOutput {...execution}/>
        </div>
    </section>;
}
