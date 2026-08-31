import ExecutionTaskPanel from '../ExecutionTaskPanel';
import ResultRenderer from '../ResultRenderer';
import type {ExecutionTask, ScriptExecutionResult} from '../types';

export type ResultView = 'collapsed' | 'open' | 'maximized';

interface Props {
    executionTask?: ExecutionTask;
    result: ScriptExecutionResult | string;
    executing: boolean;
    resultView: ResultView;
    onViewChange: (view: ResultView) => void;
    onCancel: () => void;
}

export default function ExecutionResultsPanel({
                                                  executionTask,
                                                  result,
                                                  executing,
                                                  resultView,
                                                  onViewChange,
                                                  onCancel
                                              }: Props) {
    return (
        <section className="panel workbench-results" aria-label="执行结果区">
            <header className="section-title">
                <span><i className={executionTask?.status === 'FAILED' ? 'red-dot' : 'green-dot'}/>执行结果</span>
                <small className="result-summary">{executionTask
                    ? executionTask.status + ' · ' + executionTask.targets.length + ' 个实例 · ' + (executionTask.duration ?? '—') + ' ms'
                    : executing ? '正在执行…' : typeof result === 'string' ? result.split('\n')[0] : '结果已就绪'}</small>
                <div className="result-actions panel-actions">
                    {executing && executionTask && <button type="button" className="cancel-button"
                                                           onClick={() => onCancel()}>取消任务</button>}
                    <button type="button" aria-expanded={resultView !== 'collapsed'} aria-controls="execution-results"
                            onClick={() => onViewChange(resultView === 'collapsed' ? 'open' : 'collapsed')}>
                        {resultView === 'collapsed' ? '展开结果' : '收起结果'}</button>
                    <button type="button"
                            onClick={() => onViewChange(resultView === 'maximized' ? 'open' : 'maximized')}>
                        {resultView === 'maximized' ? '还原结果区' : '最大化结果'}</button>
                </div>
            </header>
            <div className="result-panel-body" id="execution-results" hidden={resultView === 'collapsed'}>
                {executionTask ? <ExecutionTaskPanel task={executionTask}/> : <ResultRenderer result={result}/>}
            </div>
        </section>
    );
}
