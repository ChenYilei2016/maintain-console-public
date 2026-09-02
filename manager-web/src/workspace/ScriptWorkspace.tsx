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
import type {LoginInfo, RuntimeMetadata, ScriptDetail, ServiceInstance} from '../types';
import {useExecution} from '../execution/useExecution';
import {debugDraft, runTool} from '../execution/execution';
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
import {navigate} from '../navigation';
import ExecutionConfirmation from './ExecutionConfirmation';
import ScriptImportDialog from './ScriptImportDialog';

const CodeEditor = lazy(() => import('../CodeEditor'));
const SavedScriptRunner = lazy(() => import('./SavedScriptRunner'));
type Dialog = 'execute' | 'details' | 'grants' | 'history' | 'versions' | 'example' | 'ai' | 'import' | undefined;

export const canOpenScriptEditor = (script: Pick<ScriptDetail, 'canRead' | 'canEdit'>) => script.canRead || script.canEdit;

export function ScriptPermissionFallback({name, canManage, onManage}: {
    name: string; canManage: boolean; onManage: () => void;
}) {
    return <main className="tool-home"><a href="/workspace">← 脚本目录</a>
        <section className="tool-unavailable"><h1>{name}</h1>
            {canManage ? <>
                <p>当前账号只有授权管理能力，不能查看源码或运行脚本。</p>
                <button className="primary" type="button" onClick={onManage}>管理授权</button>
            </> : <p role="alert">目录名称全员可见，但当前账号没有查看、运行或授权管理能力。</p>}
        </section>
    </main>;
}

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
    const [resultView, setResultView] = useState<ResultView>('open');
    const [dialog, setDialog] = useState<Dialog>();
    const [preview, setPreview] = useState<string>();
    const [favorite, setFavorite] = useState(false);
    const [resourceRevision, setResourceRevision] = useState(0);
    const [notice, setNotice] = useState('');
    const [exampleTemplate, setExampleTemplate] = useState<keyof typeof TOOL_TEMPLATES>('table');
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
        setRuntimeMetadata(undefined);
        if (tool && environment) api.getRuntimeMetadata(id, tool.serviceName, environment, target.selectionMode === 'SPECIFIC' ? target.instanceId : undefined)
            .then(result => {
                if (active) setRuntimeMetadata(result);
            }).catch(() => {
                if (active) setRuntimeMetadata(undefined);
            });
        return () => {
            active = false;
        };
    }, [id, tool?.serviceName, tool?.version, environment, target.selectionMode, target.instanceId]);
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
    }, [definitions, environment]);
    const issues = parameterSchemaIssues(draft?.content || '', schemaState.schema);
    const save = async () => {
        if (await editor.save()) {
            setNotice('已保存：共享脚本已更新为此版本');
            setResourceRevision(value => value + 1);
        }
    };
    const openGrants = () => {
        if (editor.dirty) setNotice('请先保存或处理当前草稿，再修改授权，避免混淆版本');
        else setDialog('grants');
    };
    const selectScript = (nextId: string) => {
        if (nextId === id) return;
        if (editor.dirty && !window.confirm('当前脚本有未保存草稿，切换后草稿会保留在此浏览器。确认切换？')) return;
        if (execution.running && !window.confirm('当前请求仍在等待结果；切换页面不会终止远端操作。仍要切换？')) return;
        navigate(`/workspace/${nextId}`);
    };
    const rejectExecution = (message: string) => {
        setResultView('open');
        setNotice(`未发起执行：${message}`);
        execution.reject(message);
    };
    const startExecution = () => {
        if (!tool || !draft) return;
        setResultView('open');
        setParametersOpen(false);
        setNotice('');
        const request = {
            scriptId: id,
            version: tool.version,
            parameters: executionParameters(definitions, values, Boolean(schemaState.schema)),
            target: {...target, environment},
            riskConfirmed: true
        };
        void execution.execute(() => tool.canEdit ? debugDraft({
            ...request,
            content: draft.content,
            parameterSchema: draft.schema,
        }) : runTool(request));
    };
    const executeCurrent = () => {
        if (!tool || !draft || execution.running) return;
        if (!tool.canInvoke) {
            rejectExecution('当前无运行权限，请联系脚本负责人授权');
            return;
        }
        if (!environment) {
            rejectExecution('当前脚本没有可运行环境，请先在授权中配置允许环境');
            return;
        }
        if (!instances.length) {
            rejectExecution('当前环境没有可用实例');
            return;
        }
        if (schemaState.error || issues.length) {
            rejectExecution(schemaState.error || issues.join(' '));
            return;
        }
        if (target.selectionMode === 'SPECIFIC' && !target.instanceId) {
            rejectExecution('请先在目标设置中选择实例');
            return;
        }
        if (selectedEnvironment?.production || draft.metadata.operationType !== 'QUERY') {
            setResultView('open');
            setNotice('等待二次确认：尚未发起执行');
            setDialog('execute');
            return;
        }
        startExecution();
    };
    useEffect(() => {
        const shortcut = (event: KeyboardEvent) => {
            if (!(event.metaKey || event.ctrlKey) || event.altKey || dialog) return;
            if (event.key.toLowerCase() === 's') {
                event.preventDefault();
                if (tool?.canEdit && !editor.saving) void save();
            } else if (event.key === 'Enter') {
                event.preventDefault();
                executeCurrent();
            } else if (event.key.toLowerCase() === 'p') {
                event.preventDefault();
                setResourcesOpen(true);
                window.requestAnimationFrame(() => document.querySelector<HTMLInputElement>(
                    '.workbench-sidebar [aria-label="搜索目录树"]')?.focus({preventScroll: true}));
            }
        };
        window.addEventListener('keydown', shortcut);
        return () => window.removeEventListener('keydown', shortcut);
    });
    if (!tool || !draft) return <main className="tool-home"><a href="/workspace">← 脚本目录</a>
        <section className="tool-unavailable">
            <h1>{editor.error ? '无法打开脚本工作台' : '正在加载脚本…'}</h1><p role="alert">{editor.error}</p></section>
    </main>;
    if (!canOpenScriptEditor(tool)) return tool.canInvoke
        ? <main className={'workbench ' + (resourcesOpen ? '' : 'resources-collapsed')}>
            <header className="workbench-header">
                <button className="icon-button" aria-label={resourcesOpen ? '收起资源栏' : '展开资源栏'}
                        onClick={() => setResourcesOpen(!resourcesOpen)}>☰
                </button>
                <a className="app-name" href="/workspace">脚本工作台</a>
                <span className="workspace-service">{tool.serviceName}</span>
                <div className="app-header-actions"><small>{login.employeeName} · 运行已保存 v{tool.version}</small>
                </div>
            </header>
            {resourcesOpen && <button className="resource-backdrop" aria-label="关闭资源栏"
                                      onClick={() => setResourcesOpen(false)}/>}
            <aside className="workbench-sidebar"><WorkspaceResources serviceName={tool.serviceName} scriptId={id}
                                                                     environment="" environments={login.availableEnvironments}
                                                                     revision={resourceRevision}
                                                                     onScriptSelect={selectScript}/></aside>
            <section className="workbench-main saved-runner-workspace">
                <Suspense fallback={<div className="app-loading">正在加载运行表单…</div>}>
                    <SavedScriptRunner id={id} login={login} execution={execution}/>
                </Suspense>
            </section>
        </main>
        : <>
            <ScriptPermissionFallback name={tool.name} canManage={tool.canManage}
                                      onManage={() => setDialog('grants')}/>
            {dialog === 'grants' && <ToolGrantsModal scriptId={id} environments={login.availableEnvironments}
                                                     onClose={() => setDialog(undefined)} onSaved={() => {
                void editor.reload();
                setResourceRevision(value => value + 1);
            }}/>}</>;
    const currentScript = {...tool, name: draft.name, content: draft.content};
    return <main className={'workbench ' + (resourcesOpen ? '' : 'resources-collapsed')}>
        <header className="workbench-header">
            <button className="icon-button" aria-label={resourcesOpen ? '收起资源栏' : '展开资源栏'}
                    onClick={() => setResourcesOpen(!resourcesOpen)}>☰
            </button>
            <a className="app-name" href="/workspace">脚本工作台</a><span
            className="workspace-service">{tool.serviceName}</span>
            <div className="context-selectors"><label><span>调试环境</span><select aria-label="调试环境"
                                                                                   value={environment}
                                                                                   disabled={execution.running}
                                                                                   onChange={event => setEnvironment(event.target.value)}>
                {!environments.length && <option value="">没有已授权环境</option>}
                {login.availableEnvironments.map(item => {
                    const allowed = environments.some(environment => environment.value === item.value);
                    return <option key={item.value} value={item.value} disabled={!allowed}>
                        {item.name}{allowed ? '' : ' · 未授权'}
                    </option>;
                })}</select></label>
                {login.availableEnvironments.length > environments.length && (tool.canManage
                    ? <button className="environment-access-hint" type="button" onClick={openGrants}>
                        开启更多环境
                    </button>
                    : <span className="environment-access-hint">部分环境未授权</span>)}</div>
            {selectedEnvironment?.production && <span className="production-badge">生产环境</span>}
            <div className="app-header-actions"><small>{login.employeeName} · v{tool.version}</small></div>
        </header>
        {resourcesOpen &&
            <button className="resource-backdrop" aria-label="关闭资源栏" onClick={() => setResourcesOpen(false)}/>}
        <aside className="workbench-sidebar" id="resource-sidebar"><WorkspaceResources serviceName={tool.serviceName}
                                                                                       scriptId={id}
                                                                                       environment={environment}
                                                                                       environments={login.availableEnvironments}
                                                                                       revision={resourceRevision}
                                                                                       onScriptSelect={selectScript}/>
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
                              onPermissions={openGrants}
                              onExample={() => setDialog('example')} onAiAssistant={() => setDialog('ai')}
                              onDetails={() => setDialog('details')}
                              onImport={() => setDialog('import')}
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
                                      navigate(`/workspace/${copied}`);
                                  } catch (failure) {
                                      setNotice(failure instanceof Error ? failure.message : '复制失败');
                                  }
                              }}/>
            {(notice || editor.error) && <div className="workspace-notice" role="status">{editor.error || notice}
                <button onClick={() => setNotice('')}>收起</button>
            </div>}
            {editor.recovery && <div className="workspace-notice">当前浏览器有可恢复草稿（基于 v{editor.recovery.version}）；运行值与敏感默认值未缓存。
                <button onClick={() => {
                    if (window.confirm(`缓存基于 v${editor.recovery!.version}，服务器当前为 v${tool.version}。恢复会替换当前编辑内容，但不会保存；请在版本对比中核对差异，敏感默认值需要重新填写。`)) editor.recover();
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
                                       }} onExecute={executeCurrent} onExample={() => setDialog('example')}
                                       onEditScript={() => {
                    setParametersOpen(false);
                    document.querySelector<HTMLElement>('[aria-label="Groovy 脚本内容"]')?.focus({preventScroll: true});
                }}/>
                <ExecutionResultsPanel execution={execution} resultView={resultView} onViewChange={setResultView}/>
            </div>
        </section>
        {dialog === 'execute' && (
            <ExecutionConfirmation scriptName={draft.name}
                                   environment={selectedEnvironment?.name || environment}
                                   target={target.selectionMode === 'ALL' ? '全部实例'
                                       : target.instanceId || '随机单实例'} version={tool.version}
                                   riskNote={draft.metadata.riskNote || ''}
                                   confirmLabel={tool.canEdit ? '确认并调试' : '确认并运行'}
                                   onCancel={() => {
                                       setDialog(undefined);
                                       rejectExecution('已取消风险确认');
                                   }} onConfirm={() => {
                setDialog(undefined);
                startExecution();
            }}/>
        )}
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
        {dialog === 'import' && <ScriptImportDialog defaultServiceName={tool.serviceName}
                                                    defaultParentId={tool.parentId}
                                                    defaultEnvironment={environment}
                                                    environments={login.availableEnvironments}
                                                    onClose={() => setDialog(undefined)}/>}
        {dialog === 'details' && <Modal title="用途与风险说明" onClose={() => setDialog(undefined)} footer={<button
            onClick={() => setDialog(undefined)}>返回工作台，稍后保存</button>}>
            <div className="form-stack"><label><span>脚本用途</span><textarea disabled={!tool.canEdit} rows={3}
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
                <p>保存会更新共享脚本。SQL 参数化和调用者业务数据范围必须由脚本和受控业务能力落实，类型校验不能替代。</p>
            </div>
        </Modal>}
        {dialog === 'example' && <Modal title="示例库：代码与结构化结果" wide onClose={() => setDialog(undefined)}
                                        footer={<button disabled={!tool.canEdit} onClick={() => {
                                            editor.update({
                                                content: TOOL_TEMPLATES[exampleTemplate].content,
                                                schema: TOOL_TEMPLATES[exampleTemplate].schema
                                            });
                                            setDialog(undefined);
                                        }}>应用到当前草稿，不保存</button>}>
            <p>示例只使用固定数据；应用前请保留需要的草稿内容。</p>
            <label className="example-selector"><span>选择示例</span><select value={exampleTemplate}
                                                                             onChange={event => setExampleTemplate(event.target.value as keyof typeof TOOL_TEMPLATES)}>
                {Object.entries(TOOL_TEMPLATES).map(([key, value]) => <option value={key}
                                                                              key={key}>{value.name}</option>)}</select></label>
            <p>{TOOL_TEMPLATES[exampleTemplate].description}</p>
            <pre className="preview-code">{TOOL_TEMPLATES[exampleTemplate].content}</pre>
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
