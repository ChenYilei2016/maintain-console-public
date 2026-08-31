import {useEffect, useState} from 'react';
import ParameterForm from '../ParameterForm';
import ParameterPresets from '../ParameterPresets';
import {defaultParameterValues, executionParameters, safeParameterValues} from '../parameters';
import type {LoginInfo, ServiceInstance} from '../types';
import type {ExecutionTarget} from '../workspace/ExecutionTargetSettings';
import ExecutionTargetSettings from '../workspace/ExecutionTargetSettings';
import ParameterScrollArea from '../workspace/ParameterScrollArea';
import ExecutionHistoryModal from '../execution/ExecutionHistoryModal';
import ExecutionOutput from '../execution/ExecutionOutput';
import {useExecution} from '../execution/useExecution';
import {runTool} from '../execution/execution';
import type {ToolForm} from './toolApi';
import {OPERATION_LABELS, toolApi} from './toolApi';
import './tools.css';

export default function ToolRunPage({id, login}: { id: string; login: LoginInfo }) {
    const [tool, setTool] = useState<ToolForm>();
    const [environment, setEnvironment] = useState('');
    const [values, setValues] = useState<Record<string, string>>({});
    const [instances, setInstances] = useState<ServiceInstance[]>([]);
    const [target, setTarget] = useState<ExecutionTarget>({
        selectionMode: 'RANDOM',
        instanceId: '',
        timeoutSeconds: 180
    });
    const [error, setError] = useState('');
    const [showHistory, setShowHistory] = useState(false);
    const [refresh, setRefresh] = useState(0);
    const execution = useExecution();
    useEffect(() => {
        let active = true;
        setError('');
        toolApi.open(id).then(form => {
            if (!active) return;
            setTool(form);
            setEnvironment(current => form.environments.some(item => item.value === current) ? current : form.environments[0]?.value || '');
            setValues(current => ({...defaultParameterValues(form.parameters), ...safeParameterValues(form.parameters, current)}));
            setTarget(current => ({
                ...current, timeoutSeconds: form.defaultTimeoutSeconds,
                selectionMode: !form.allowAllInstances && current.selectionMode === 'ALL' ? 'RANDOM' : current.selectionMode
            }));
        }).catch(failure => {
            if (active) {
                setTool(undefined);
                setError(failure.message);
            }
        });
        return () => {
            active = false;
        };
    }, [id, refresh]);
    useEffect(() => {
        let active = true;
        setInstances([]);
        setTarget(current => ({...current, instanceId: ''}));
        if (environment) toolApi.instances(id, environment).then(result => {
            if (active) setInstances(result);
        })
            .catch(failure => {
                if (active) setError(failure.message);
            });
        return () => {
            active = false;
        };
    }, [id, environment, refresh]);
    const selectedEnvironment = tool?.environments.find(item => item.value === environment);
    return <main className="tool-run-page">
        <header className="tool-app-header"><a href="/">←
            工具首页</a><span>{login.employeeName} · {login.employeeNo}</span></header>
        {!tool ? <section className="tool-unavailable"><h1>{error ? '暂时无法打开这个工具' : '正在打开工具…'}</h1>
            <p role="alert">{error || '正在检查你的访问权限'}</p>
            <button onClick={() => setRefresh(value => value + 1)}>重新检查</button>
        </section> : <>
            <section className="run-intro">
                <div>
                    <div className="eyebrow">{tool.serviceName} / {OPERATION_LABELS[tool.metadata.operationType]} /
                        v{tool.version}</div>
                    <h1>{tool.name}</h1><p>{tool.description || '作者尚未填写用途说明，请确认用途后运行。'}</p>
                    <small>负责人：{tool.owner} · 当前使用已保存版本；未保存草稿不会影响此页面</small></div>
                <div className="panel-actions">
                    <button disabled={execution.running} onClick={() => setRefresh(value => value + 1)}>刷新工具版本
                    </button>
                    <button onClick={() => setShowHistory(true)}>执行历史</button>
                    {tool.canRead &&
                        <a className="button" href={`/workspace/${id}`}>{tool.canEdit ? '编辑工具' : '查看代码'}</a>}
                </div>
            </section>
            <div className="run-layout">
                <section className="run-form-panel" aria-label="工具参数">
                    <header className="run-target-header"><label><span>执行环境</span><select
                        disabled={execution.running} value={environment} onChange={event => {
                        setEnvironment(event.target.value);
                        setError('');
                    }}>
                        {!tool.environments.length && <option value="">作者尚未允许任何环境</option>}
                        {tool.environments.map(item => <option value={item.value}
                                                               key={item.value}>{item.name}{item.production ? ' · 生产' : ''}</option>)}</select></label>
                        <ExecutionTargetSettings target={target} instances={instances} onChange={setTarget}
                                                 allowAllInstances={tool.allowAllInstances}/>
                        <small
                            className="parameter-completion">已填写 {tool.parameters.filter(item => values[item.name]?.trim()).length} / {tool.parameters.length} 项
                            · 标 * 为必填</small>
                    </header>
                    <ParameterScrollArea view="run" label="工具参数列表" itemCount={tool.parameters.length}>
                        <form id="tool-run-form" onSubmit={event => {
                            event.preventDefault();
                            if (!environment || !instances.length || execution.running) return;
                            if (target.selectionMode === 'SPECIFIC' && !target.instanceId) {
                                setError('请在目标设置中指定实例');
                                return;
                            }
                            const confirmationNeeded = selectedEnvironment?.production || tool.metadata.operationType !== 'QUERY';
                            if (confirmationNeeded && !window.confirm(`确认运行「${tool.name}」？\n环境：${selectedEnvironment?.name}\n目标：${target.selectionMode === 'ALL' ? '全部实例' : target.instanceId || '随机单实例'}\n${tool.metadata.riskNote || '脚本可能修改业务数据，请确认影响范围。'}\n二次确认不是审批或安全隔离，操作不会自动重试。`)) return;
                            void execution.execute(() => runTool({
                                scriptId: id,
                                version: tool.version,
                                parameters: executionParameters(tool.parameters, values, true),
                                target: {...target, environment},
                                riskConfirmed: true
                            }));
                        }}>
                            {tool.metadata.usageExample &&
                                <p className="tool-usage">使用示例：{tool.metadata.usageExample}</p>}
                            <ParameterPresets key={id + environment} userId={login.employeeNo} scriptId={id}
                                              environment={environment}
                                              definitions={tool.parameters} values={values} onChange={setValues}/>
                            <ParameterForm definitions={tool.parameters} values={values} instances={instances}
                                           onChange={(name, value) => setValues(current => ({
                                               ...current,
                                               [name]: value
                                           }))}/>
                        </form>
                    </ParameterScrollArea>
                    <footer className="run-submit">
                        {(selectedEnvironment?.production || tool.metadata.operationType !== 'QUERY') &&
                            <p className="production-warning">{selectedEnvironment?.production ? '生产环境 · ' : ''}{tool.metadata.riskNote || '请确认操作影响范围；类型标识不代表只读保证。'}</p>}
                        {error && <p role="alert" className="field-error">{error}</p>}
                        {!instances.length &&
                            <p className="field-error">{environment ? '当前没有可用实例，不能发起运行。' : '请联系作者配置允许环境。'}</p>}
                        <button className="run-button" type="submit" form="tool-run-form"
                                disabled={execution.running || !environment || !instances.length}>
                            {execution.running ? `执行中 · ${(execution.elapsed / 1000).toFixed(1)} 秒` : '运行工具'}</button>
                        <small>运行会留下记录；断网或超时不等于远端已停止。</small>
                    </footer>
                </section>
                <section className="run-result-panel" aria-label="本次运行结果"><h2>本次结果</h2>
                    <ExecutionOutput {...execution}/></section>
            </div>
            {showHistory && <ExecutionHistoryModal scriptId={id} onClose={() => setShowHistory(false)}
                                                   onRestore={restored => setValues({
                                                       ...defaultParameterValues(tool.parameters), ...safeParameterValues(tool.parameters, restored),
                                                   })}/>}
        </>}
    </main>;
}
