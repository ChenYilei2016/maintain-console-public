import {useEffect, useRef} from 'react';
import ParameterForm from '../ParameterForm';
import ParameterPresets from '../ParameterPresets';
import ParameterSchemaEditor from '../ParameterSchemaEditor';
import type {EnvironmentOption, ParameterDefinition, ScriptDetail, ServiceInstance} from '../types';
import ExecutionTargetSettings, {type ExecutionTarget} from './ExecutionTargetSettings';
import ParameterScrollArea from './ParameterScrollArea';

export type ParameterTab = 'values' | 'schema';

interface Props {
    script: ScriptDetail;
    parameterSchema: string;
    definitions: ParameterDefinition[];
    parameterValues: Record<string, string>;
    onValueChange: (name: string, value: string) => void;
    onSchemaChange: (value: string) => void;
    target: ExecutionTarget;
    onTargetChange: (target: ExecutionTarget) => void;
    instances: ServiceInstance[];
    environment?: EnvironmentOption;
    draftChanged: boolean;
    executing: boolean;
    userId: string;
    allowAllInstances: boolean;
    onValuesChange: (values: Record<string, string>) => void;
    parameterTab: ParameterTab;
    onTabChange: (tab: ParameterTab) => void;
    parametersOpen: boolean;
    onClose: () => void;
    onPreview: () => void;
    onExecute: () => void;
    onExample: () => void;
    onEditScript: () => void;
}

export default function ScriptParametersPanel({
                                                  script,
                                                  parameterSchema,
                                                  definitions,
                                                  parameterValues,
                                                  onValueChange,
                                                  onSchemaChange: setParameterSchema,
                                                  target,
                                                  onTargetChange,
                                                  instances,
                                                  environment,
                                                  draftChanged,
                                                  executing,
                                                  userId,
                                                  allowAllInstances,
                                                  onValuesChange,
                                                  parameterTab,
                                                  onTabChange: setParameterTab,
                                                  parametersOpen,
                                                  onClose,
                                                  onPreview,
                                                  onExecute,
                                                  onExample,
                                                  onEditScript,
                                              }: Props) {
    const panel = useRef<HTMLElement>(null);
    const executionForm = useRef<HTMLFormElement>(null);
    const isProduction = Boolean(environment?.production);

    useEffect(() => {
        if (!parametersOpen || window.innerWidth >= 1000) return;
        const opener = document.activeElement;
        panel.current?.querySelector<HTMLElement>('[role="tab"][aria-selected="true"]')?.focus({preventScroll: true});
        return () => {
            if (opener instanceof HTMLElement && opener.isConnected && panel.current?.contains(document.activeElement)) {
                opener.focus({preventScroll: true});
            }
        };
    }, [parametersOpen]);

    return (
        <aside ref={panel} className={'panel workbench-parameters ' + (parametersOpen ? 'drawer-open' : '')}
               id="parameter-sidebar" aria-label="参数与运行"
               onKeyDown={(event) => {
                   if (event.key === 'Escape' && !(event.target as HTMLElement).closest('[role="dialog"]')) onClose();
               }}>
            <header className="parameter-panel-header">
                <strong>参数与运行 <small>{definitions.length}</small></strong>
                <button className="icon-button parameter-close" type="button" aria-label="收起参数栏"
                        onClick={() => onClose()}>×
                </button>
            </header>
            <ExecutionTargetSettings target={target} instances={instances} onChange={onTargetChange}
                                     allowAllInstances={allowAllInstances}/>
            <small
                className="parameter-completion">已填写 {definitions.filter(item => parameterValues[item.name]?.trim()).length} / {definitions.length} 项</small>
            <div className="workspace-tabs" role="tablist" aria-label="参数视图"
                 onKeyDown={(event) => {
                     if (!['ArrowLeft', 'ArrowRight'].includes(event.key)) return;
                     event.preventDefault();
                     event.currentTarget.querySelector<HTMLButtonElement>('[aria-selected="false"]')?.focus();
                     setParameterTab(parameterTab === 'values' ? 'schema' : 'values');
                 }}>
                {([['values', '运行填值'], ['schema', '参数配置']] as const).map(([tab, title]) =>
                    <button key={tab} type="button" role="tab" id={'tab-' + tab}
                            tabIndex={parameterTab === tab ? 0 : -1}
                            aria-selected={parameterTab === tab} aria-controls={'panel-' + tab}
                            onClick={() => setParameterTab(tab)}>{title}</button>)}
            </div>
            <ParameterScrollArea key={script.id} view={parameterTab} itemCount={definitions.length}
                                 label={parameterTab === 'values' ? '运行参数列表' : '参数配置列表'}>
                <form id="execution-form" ref={executionForm} hidden={parameterTab !== 'values'}
                      onSubmit={(event) => {
                          event.preventDefault();
                          onExecute();
                      }}>
                    <div id="panel-values" role="tabpanel" aria-labelledby="tab-values">
                        <p className="parameter-form-note">填写本次运行值，不会改写参数默认值。</p>
                        <ParameterPresets key={script.id + environment?.value} userId={userId} scriptId={script.id}
                                          environment={environment?.value || ''} definitions={definitions}
                                          values={parameterValues} onChange={onValuesChange}/>
                        <ParameterForm definitions={definitions} values={parameterValues} instances={instances}
                                       onChange={onValueChange}/>
                        {!definitions.length && <button className="text-button" type="button"
                                                        onClick={() => setParameterTab('schema')}>添加参数或从脚本识别
                            →</button>}
                    </div>
                </form>
                <div id="panel-schema" role="tabpanel" aria-labelledby="tab-schema" hidden={parameterTab !== 'schema'}>
                    <ParameterSchemaEditor key={script.id} value={parameterSchema} script={script.content}
                                           disabled={!script.canEdit} onChange={setParameterSchema}
                                           onEditScript={onEditScript}
                                           onLoadExample={() => onExample()}/>
                </div>
            </ParameterScrollArea>
            <footer className="parameter-panel-footer">
                {isProduction && <div className="production-warning">生产环境 · 请核对目标和操作风险，确认不是审批</div>}
                <small className="execution-context" title={script.serviceName}>
                    {environment?.name || '未选环境'} / {script.serviceName}
                    {draftChanged && <b> · 使用当前草稿</b>}
                </small>
                {parameterTab === 'values' ? <div className="execution-actions">
                    <button type="button" disabled={executing} onClick={() => {
                        if (executionForm.current?.reportValidity()) onPreview();
                    }}>预览替换
                    </button>
                    <button className="run-button" type="submit" form="execution-form"
                            disabled={!script.canInvoke || !script.canEdit || executing || !environment || !instances.length}>{executing ? '执行中…'
                        : '▶ 调试当前内容'}</button>
                </div> : <button className="run-button" type="button"
                                 onClick={() => setParameterTab('values')}>完成配置，填写运行参数 →</button>}
            </footer>
        </aside>
    );
}
