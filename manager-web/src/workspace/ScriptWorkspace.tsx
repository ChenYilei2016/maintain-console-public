import {lazy, Suspense, useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import Modal from '../Modal';
import AiAssistantModal from '../AiAssistantModal';
import {
    defaultParameterValues,
    executionParameters,
    parameterDefinitions,
    parameterSchemaIssues,
    parseParameterSchema,
    safeParameterValues
} from '../parameters';
import type {LoginInfo, RuntimeMetadata, ServiceInstance} from '../types';
import {useExecution} from '../execution/useExecution';
import {debugDraft} from '../execution/execution';
import ExecutionHistoryModal from '../execution/ExecutionHistoryModal';
import ToolGrantsModal from '../tools/ToolGrantsModal';
import {OPERATION_LABELS} from '../tools/toolApi';
import {TOOL_TEMPLATES} from './templates';
import {useScriptDraft} from './useScriptDraft';
import WorkspaceToolbar from './WorkspaceToolbar';
import WorkspaceResources from './WorkspaceResources';
import type {ParameterTab} from './ScriptParametersPanel';
import ScriptParametersPanel from './ScriptParametersPanel';
import type {ExecutionTarget} from './ExecutionTargetSettings';
import type {ResultView} from './ExecutionResultsPanel';
import ExecutionResultsPanel from './ExecutionResultsPanel';
import ScriptVersionsModal from './ScriptVersionsModal';
import './workspace.css';
import '../tools/tools.css';

const CodeEditor = lazy(() => import('../CodeEditor'));
type Dialog = 'details' | 'grants' | 'history' | 'versions' | 'example' | 'ai' | undefined;

export default function ScriptWorkspace({id, login}: { id: string; login: LoginInfo }) {
    const editor = useScriptDraft(id, login.employeeNo);
    const {tool, draft} = editor;
    const execution = useExecution();
    const [environment, setEnvironment] = useState('');
    const [values, setValues] = useState<Record<string, string>>({});
    const [instances, setInstances] = useState<ServiceInstance[]>([]);
    const [runtimeMetadata, setRuntimeMetadata] = useState<RuntimeMetadata>();
    const [target, setTarget] = useState<ExecutionTarget>({
        selectionMode: 'RANDOM',
        instanceId: '',
        timeoutSeconds: 180
    });
    const [parameterTab, setParameterTab] = useState<ParameterTab>('values');
    const [parametersOpen, setParametersOpen] = useState(false);
    const [resourcesOpen, setResourcesOpen] = useState(() => window.innerWidth >= 1280);
    const [resultView, setResultView] = useState<ResultView>('collapsed');
    const [dialog, setDialog] = useState<Dialog>();
    const [preview, setPreview] = useState<string>();
    const [favorite, setFavorite] = useState(false);
    const [resourceRevision, setResourceRevision] = useState(0);
    const [notice, setNotice] = useState('');
    const environments = login.availableEnvironments.filter(item => tool?.allowedEnvironments == null || tool.allowedEnvironments.includes(item.value));
    const selectedEnvironment = environments.find(item => item.value === environment);
    useEffect(() => {
        if (tool && !environments.some(item => item.value === environment)) setEnvironment(environments[0]?.value || '');
    }, [tool, environments, environment]);
    useEffect(() => {
        if (!tool) return;
        api.getResourceOverview(tool.serviceName).then(result => setFavorite(result.favorites.some(item => item.id === id))).catch(() => setFavorite(false));
    }, [id, tool?.serviceName]);
    useEffect(() => {
        const wide = window.matchMedia('(min-width: 1280px)');
        const resize = (event: MediaQueryListEvent) => setResourcesOpen(event.matches);
        wide.addEventListener('change', resize);
        return () => wide.removeEventListener('change', resize);
    }, []);
    useEffect(() => {
        let active = true;
        setInstances([]);
        setRuntimeMetadata(undefined);
        setTarget(current => ({...current, instanceId: ''}));
        if (tool && environment) api.listInstances(id, environment).then(result => {
            if (active) setInstances(result);
        })
            .catch(failure => {
                if (active) setNotice(failure.message);
            });
        return () => {
            active = false;
        };
    }, [id, tool?.version, environment]);
    useEffect(() => {
        let active = true;
        if (tool && environment) api.getRuntimeMetadata(id, tool.serviceName, environment, target.selectionMode === 'SPECIFIC' ? target.instanceId : undefined)
            .then(result => {
                if (active) setRuntimeMetadata(result);
            }).catch(() => {
                if (active) setRuntimeMetadata(undefined);
            });
        return () => {
            active = false;
        };
    }, [id, tool?.serviceName, environment, target.selectionMode, target.instanceId]);
    const schemaState = useMemo(() => {
        try {
            return {schema: parseParameterSchema(draft?.schema), error: ''};
        } catch (failure) {
            return {schema: undefined, error: failure instanceof Error ? failure.message : '参数配置无效'};
        }
    }, [draft?.schema]);
    const definitions = useMemo(() => parameterDefinitions(draft?.content || '', schemaState.schema), [draft?.content, schemaState.schema]);
    useEffect(() => {
        setValues(current => ({...defaultParameterValues(definitions), ...safeParameterValues(definitions, current)}));
    }, [definitions]);
    const issues = parameterSchemaIssues(draft?.content || '', schemaState.schema);
    const save = async () => {
        if (await editor.save()) {
            setNotice('已保存：共享工具已更新为此版本');
            setResourceRevision(value => value + 1);
        }
    };
    if (!tool || !draft) return <main className="tool-home"><a href="/">← 工具首页</a>
        <section className="tool-unavailable">
            <h1>{editor.error ? '无法打开开发工作台' : '正在加载工具…'}</h1><p role="alert">{editor.error}</p><a
            className="button" href={`/tools/${id}`}>前往运行页</a></section>
    </main>;
    const currentScript = {...tool, name: draft.name, content: draft.content};
    return <main className={'workbench ' + (resourcesOpen ? '' : 'resources-collapsed')}>
        <header className="workbench-header">
            <button className="icon-button" aria-label={resourcesOpen ? '收起资源栏' : '展开资源栏'}
                    onClick={() => setResourcesOpen(!resourcesOpen)}>☰
            </button>
            <a className="app-name" href="/">工具首页</a><span className="workspace-service">{tool.serviceName}</span>
            <div className="context-selectors"><label><span>调试环境</span><select value={environment}
                                                                                   disabled={execution.running}
                                                                                   onChange={event => setEnvironment(event.target.value)}>
                {!environments.length && <option value="">未授权环境</option>}{environments.map(item => <option
                key={item.value} value={item.value}>{item.name}</option>)}</select></label></div>
            {selectedEnvironment?.production && <span className="production-badge">生产环境</span>}
            <div className="app-header-actions"><small>{login.employeeName} · v{tool.version}</small></div>
        </header>
        {resourcesOpen &&
            <button className="resource-backdrop" aria-label="关闭资源栏" onClick={() => setResourcesOpen(false)}/>}
        <aside className="workbench-sidebar" id="resource-sidebar"><WorkspaceResources serviceName={tool.serviceName}
                                                                                       scriptId={id}
                                                                                       environment={environment}
                                                                                       revision={resourceRevision}/>
        </aside>
        <section className="workbench-main">
            <WorkspaceToolbar script={currentScript} draftChanged={editor.dirty} saving={editor.saving}
                              scriptIsFavorite={favorite}
                              aiEnabled={login.aiEnabled && tool.canEdit} parameterCount={definitions.length}
                              parametersOpen={parametersOpen}
                              onNameChange={name => editor.update({name})} onParametersToggle={() => {
                setParametersOpen(!parametersOpen);
                if (resultView === 'maximized') setResultView('open');
            }}
                              onSave={save} onHistory={() => setDialog('history')}
                              onRevisions={() => setDialog('versions')}
                              onFavorite={async () => {
                                  try {
                                      await api.setFavorite(id, !favorite);
                                      setFavorite(!favorite);
                                      setResourceRevision(value => value + 1);
                                  } catch (failure) {
                                      setNotice(String(failure));
                                  }
                              }}
                              onPermissions={() => {
                                  if (editor.dirty) setNotice('请先保存或处理当前草稿，再修改授权，避免混淆版本'); else setDialog('grants');
                              }}
                              onExample={() => setDialog('example')} onAiAssistant={() => setDialog('ai')}
                              onDetails={() => setDialog('details')}
                              onCopy={async () => {
                                  if (!window.confirm('复制当前保存版本为自己的私有工具？不会继承授权或未保存草稿。')) return;
                                  try {
                                      const copied = await api.saveTreeNode({
                                          nodeType: 'script',
                                          nodeName: tool.name + ' - 副本',
                                          serviceName: tool.serviceName,
                                          content: tool.content,
                                          parameterSchema: tool.parameterSchema,
                                          description: tool.description,
                                          toolMetadata: tool.toolMetadata,
                                          allowedEnvironments: environment ? [environment] : []
                                      });
                                      window.location.assign(`/workspace/${copied}`);
                                  } catch (failure) {
                                      setNotice(failure instanceof Error ? failure.message : '复制失败');
                                  }
                              }}/>
            {(notice || editor.error) && <div className="workspace-notice" role="status">{editor.error || notice}
                <button onClick={() => setNotice('')}>收起</button>
            </div>}
            {editor.recovery && <div className="workspace-notice">此标签页有可恢复草稿（基于 v{editor.recovery.version}）；运行值与敏感默认值未缓存。
                <button onClick={() => {
                    if (window.confirm('恢复将替换当前编辑内容。请检查与服务器版本的差异；敏感默认值需要重新填写。')) editor.recover();
                }}>恢复草稿</button>
                <button onClick={editor.discardRecovery}>放弃缓存</button>
            </div>}
            <div className={'workbench-panels result-' + resultView}>
                <section className="panel workbench-editor" aria-label="脚本编辑区">
                    {(schemaState.error || issues.length > 0 || !schemaState.schema) &&
                        <div className="script-guidance has-issues">
                            <span>{schemaState.error || issues.join(' ') || '旧原样替换仅可开发调试；分享前请明确配置类型化参数，并确认引用位置。'}</span>
                            <button onClick={() => {
                                setParameterTab('schema');
                                setParametersOpen(true);
                            }}>配置参数 →
                            </button>
                        </div>}
                    <Suspense fallback={<div className="code-editor-loading">编辑器加载中…</div>}><CodeEditor key={id}
                                                                                                              value={draft.content}
                                                                                                              disabled={!tool.canEdit}
                                                                                                              onChange={content => editor.update({content})}
                                                                                                              parameterNames={definitions.map(item => item.name)}
                                                                                                              runtimeMetadata={runtimeMetadata}/></Suspense>
                </section>
                <ScriptParametersPanel script={currentScript} parameterSchema={draft.schema} definitions={definitions}
                                       parameterValues={values}
                                       onValueChange={(name, value) => setValues(current => ({
                                           ...current,
                                           [name]: value
                                       }))} onValuesChange={setValues}
                                       onSchemaChange={schema => editor.update({schema})}
                                       target={target} onTargetChange={setTarget} instances={instances}
                                       environment={selectedEnvironment} draftChanged={editor.dirty}
                                       executing={execution.running} userId={login.employeeNo}
                                       allowAllInstances={tool.allowAllInstances}
                                       parameterTab={parameterTab} onTabChange={setParameterTab}
                                       parametersOpen={parametersOpen} onClose={() => setParametersOpen(false)}
                                       onPreview={async () => {
                                           try {
                                               setPreview(await api.previewScript(id, draft.content, executionParameters(definitions, values, Boolean(schemaState.schema)), draft.schema));
                                           } catch (failure) {
                                               setNotice(failure instanceof Error ? failure.message : '预览失败');
                                           }
                                       }} onExecute={() => {
                    if (schemaState.error || issues.length) {
                        setNotice(schemaState.error || issues.join(' '));
                        return;
                    }
                    if (target.selectionMode === 'SPECIFIC' && !target.instanceId) {
                        setNotice('请先选择实例');
                        return;
                    }
                    if ((selectedEnvironment?.production || draft.metadata.operationType !== 'QUERY') && !window.confirm(
                        `调试未保存内容：${draft.name}\n环境：${selectedEnvironment?.name}\n${draft.metadata.riskNote || '请确认业务影响范围'}\n不自动保存、不自动重试；二次确认不是审批。`)) return;
                    setResultView('open');
                    setParametersOpen(false);
                    void execution.execute(() => debugDraft({
                        scriptId: id,
                        version: tool.version,
                        content: draft.content,
                        parameterSchema: draft.schema,
                        parameters: executionParameters(definitions, values, Boolean(schemaState.schema)),
                        target: {...target, environment},
                        riskConfirmed: true
                    }));
                }} onExample={() => setDialog('example')} onEditScript={() => {
                    setParametersOpen(false);
                    document.querySelector<HTMLElement>('[aria-label="Groovy 脚本内容"]')?.focus({preventScroll: true});
                }}/>
                <ExecutionResultsPanel execution={execution} resultView={resultView} onViewChange={setResultView}/>
            </div>
        </section>
        {dialog === 'grants' && <ToolGrantsModal scriptId={id} environments={login.availableEnvironments}
                                                 onClose={() => setDialog(undefined)} onSaved={() => {
            void editor.reload();
            setResourceRevision(value => value + 1);
        }}/>}
        {dialog === 'history' &&
            <ExecutionHistoryModal scriptId={id} onClose={() => setDialog(undefined)} onRestore={restored => {
                setValues({...defaultParameterValues(definitions), ...safeParameterValues(definitions, restored)});
                setParameterTab('values');
                setNotice('已回填非敏感参数，尚未执行');
            }}/>}
        {dialog === 'versions' && <ScriptVersionsModal tool={tool} content={draft.content} schema={draft.schema}
                                                       onClose={() => setDialog(undefined)} onRestored={() => {
            void editor.reload();
            setResourceRevision(value => value + 1);
        }}/>}
        {dialog === 'ai' &&
            <AiAssistantModal script={currentScript} serviceName={tool.serviceName} parameterSchema={draft.schema}
                              onApplyScript={content => editor.update({content})}
                              onApplyParameterSchema={schema => editor.update({schema})} onNotice={setNotice}
                              onClose={() => setDialog(undefined)}/>}
        {dialog === 'details' && <Modal title="用途与风险说明" onClose={() => setDialog(undefined)} footer={<button
            onClick={() => setDialog(undefined)}>返回工作台，稍后保存</button>}>
            <div className="form-stack"><label><span>工具用途</span><textarea disabled={!tool.canEdit} rows={3}
                                                                              value={draft.description}
                                                                              onChange={event => editor.update({description: event.target.value})}/></label>
                <label><span>操作类型（不是只读保证）</span><select disabled={!tool.canEdit}
                                                                  value={draft.metadata.operationType}
                                                                  onChange={event => editor.update({
                                                                      metadata: {
                                                                          ...draft.metadata,
                                                                          operationType: event.target.value as typeof draft.metadata.operationType
                                                                      }
                                                                  })}>
                    {Object.entries(OPERATION_LABELS).map(([value, label]) => <option value={value}
                                                                                      key={value}>{label}</option>)}</select></label>
                <label><span>使用示例</span><textarea disabled={!tool.canEdit} value={draft.metadata.usageExample || ''}
                                                      onChange={event => editor.update({
                                                          metadata: {
                                                              ...draft.metadata,
                                                              usageExample: event.target.value
                                                          }
                                                      })}/></label>
                <label><span>风险与影响范围</span><textarea disabled={!tool.canEdit}
                                                            value={draft.metadata.riskNote || ''}
                                                            onChange={event => editor.update({
                                                                metadata: {
                                                                    ...draft.metadata,
                                                                    riskNote: event.target.value
                                                                }
                                                            })}/></label>
                <p>保存会更新共享工具。SQL 参数化和调用者业务数据范围必须由脚本和受控业务能力落实，类型校验不能替代。</p>
            </div>
        </Modal>}
        {dialog === 'example' && <Modal title="入门示例：参数 → 表单 → 表格" wide onClose={() => setDialog(undefined)}
                                        footer={<button disabled={!tool.canEdit} onClick={() => {
                                            editor.update({
                                                content: TOOL_TEMPLATES.table.content,
                                                schema: TOOL_TEMPLATES.table.schema
                                            });
                                            setDialog(undefined);
                                        }}>替换当前草稿，不保存</button>}><p>替换前请保留需要的草稿内容。</p>
            <pre className="preview-code">{TOOL_TEMPLATES.table.content}</pre>
        </Modal>}
        {preview !== undefined && <Modal title="参数替换预览（开发用途）" wide onClose={() => setPreview(undefined)}>
            <pre className="preview-code">{preview}</pre>
        </Modal>}
        {editor.conflict &&
            <Modal title="保存冲突：请比较并手动合并" wide onClose={() => setNotice('草稿仍保留，请先处理版本冲突')}
                   footer={<button onClick={editor.acceptLatestVersion}>已核对差异，以最新版本继续编辑</button>}>
                <p>服务器已更新到 v{editor.conflict.version}。当前草稿未覆盖服务器；确认后仍需手动合并并再次保存。</p>
                <div className="source-comparison">
                    <section><h3>服务器代码</h3>
                        <pre>{editor.conflict.content}</pre>
                        <h3>服务器参数</h3>
                        <pre>{editor.conflict.parameterSchema}</pre>
                    </section>
                    <section><h3>当前草稿</h3>
                        <pre>{draft.content}</pre>
                        <h3>草稿参数</h3>
                        <pre>{draft.schema}</pre>
                    </section>
                </div>
            </Modal>}
    </main>;
}
