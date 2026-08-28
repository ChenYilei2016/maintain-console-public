import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {api} from './api';
import DirectoryTree from './DirectoryTree';
import Modal from './Modal';
import {extractParameters, filterTree} from './tree';
import type {DirectoryNode, ExecutionHistory, LoginInfo, NoticeType, ScriptDetail, TreeNodeSaveRequest,} from './types';

const DEFAULT_SCRIPT = `def queryData() {
    // 可通过 ctx 获取 Spring Bean，通过 _log 输出日志
    def bean = ctx.getBean('$\${yourBeanName}')
    return bean.toString()
}

return queryData()`;

type Notice = { type: NoticeType; message: string };
type CreateDialog = { parent?: DirectoryNode; type: DirectoryNode['type']; name: string };
type RenameDialog = { node: DirectoryNode; name: string };
type DeleteDialog = { node: DirectoryNode; forceDelete: boolean };

function messageOf(error: unknown): string {
    return error instanceof Error ? error.message : '未知错误';
}

function formatTime(value?: string): string {
    if (!value) return '—';
    return value.replace('T', ' ');
}

function HistoryDetail({history}: { history: ExecutionHistory }) {
    return (
        <div className="history-detail">
            <div className="detail-grid">
                <span><small>执行人</small>{history.executorName}（{history.executorId}）</span>
                <span><small>执行时间</small>{formatTime(history.startTime)}</span>
                <span><small>状态</small><b className={`status-text ${history.status}`}>{history.status}</b></span>
                <span><small>耗时</small>{history.duration} ms</span>
            </div>
            {[
                ['执行脚本', history.scriptContent],
                ['执行参数', history.parameters],
                ['最终脚本', history.finalScriptContent],
                ['执行结果', history.result || history.errorMessage],
            ].map(([label, value]) => (
                <section key={label}>
                    <h3>{label}</h3>
                    <pre>{value || '—'}</pre>
                </section>
            ))}
        </div>
    );
}

export default function App() {
    const [login, setLogin] = useState<LoginInfo>();
    const [services, setServices] = useState<string[]>([]);
    const [environment, setEnvironment] = useState('');
    const [service, setService] = useState('');
    const [tree, setTree] = useState<DirectoryNode[]>([]);
    const [search, setSearch] = useState('');
    const [script, setScript] = useState<ScriptDetail>();
    const [permissions, setPermissions] = useState('{}');
    const [parameterValues, setParameterValues] = useState<Record<string, string>>({});
    const [result, setResult] = useState('等待执行脚本…');
    const [loadingTree, setLoadingTree] = useState(false);
    const [saving, setSaving] = useState(false);
    const [executing, setExecuting] = useState(false);
    const [notice, setNotice] = useState<Notice>();
    const [showHelp, setShowHelp] = useState(false);
    const [createDialog, setCreateDialog] = useState<CreateDialog>();
    const [renameDialog, setRenameDialog] = useState<RenameDialog>();
    const [deleteDialog, setDeleteDialog] = useState<DeleteDialog>();
    const [preview, setPreview] = useState<string>();
    const [historyItems, setHistoryItems] = useState<ExecutionHistory[]>();
    const [historyPage, setHistoryPage] = useState(1);
    const [historyTotal, setHistoryTotal] = useState(0);
    const [selectedHistory, setSelectedHistory] = useState<ExecutionHistory>();
    const noticeTimer = useRef<number | undefined>(undefined);

    const showNotice = useCallback((message: string, type: NoticeType = 'error') => {
        window.clearTimeout(noticeTimer.current);
        setNotice({message, type});
        noticeTimer.current = window.setTimeout(() => setNotice(undefined), 2800);
    }, []);

    useEffect(() => () => window.clearTimeout(noticeTimer.current), []);

    useEffect(() => {
        let active = true;
        Promise.all([api.getLoginInfo(), api.listServices()])
            .then(([loginInfo, serviceNames]) => {
                if (!active) return;
                setLogin(loginInfo);
                setServices(serviceNames);
                if (loginInfo.availableEnvironments.length === 1) {
                    setEnvironment(loginInfo.availableEnvironments[0].value);
                }
            })
            .catch((error) => showNotice(`初始化失败：${messageOf(error)}`));
        return () => {
            active = false;
        };
    }, [showNotice]);

    const refreshTree = useCallback(async (serviceName: string) => {
        if (!serviceName) {
            setTree([]);
            return;
        }
        setLoadingTree(true);
        try {
            setTree(await api.getDirectoryTree(serviceName));
        } catch (error) {
            showNotice(`目录加载失败：${messageOf(error)}`);
        } finally {
            setLoadingTree(false);
        }
    }, [showNotice]);

    useEffect(() => {
        setScript(undefined);
        setParameterValues({});
        setResult('等待执行脚本…');
        void refreshTree(service);
    }, [refreshTree, service]);

    const loadScript = useCallback(async (scriptId: string) => {
        try {
            const detail = await api.getScriptDetail(scriptId);
            setScript(detail);
            setPermissions(detail.permissions || '{}');
            setResult('等待执行脚本…');
        } catch (error) {
            showNotice(`脚本加载失败：${messageOf(error)}`);
        }
    }, [showNotice]);

    const parameterNames = useMemo(() => extractParameters(script?.content || ''), [script?.content]);
    const visibleTree = useMemo(() => filterTree(tree, search), [search, tree]);
    const selectedEnvironment = login?.availableEnvironments.find((item) => item.value === environment);
    const isProduction = login?.env === 'prod' || /prod|生产/i.test(selectedEnvironment?.name || '');

    useEffect(() => {
        setParameterValues((current) => Object.fromEntries(
            parameterNames.map((name) => [name, current[name] || '']),
        ));
    }, [parameterNames]);

    const updateScript = (patch: Partial<ScriptDetail>) => {
        setScript((current) => current ? {...current, ...patch} : current);
    };

    const validateScript = (): ScriptDetail | undefined => {
        if (!script?.id) {
            showNotice('请先选择一个脚本', 'warning');
            return;
        }
        if (!script.content.trim()) {
            showNotice('脚本内容不能为空', 'warning');
            return;
        }
        return script;
    };

    const saveScript = async () => {
        const current = validateScript();
        if (!current) return;
        if (!current.canEdit) {
            showNotice('你没有编辑此脚本的权限');
            return;
        }
        if (!current.name.trim()) {
            showNotice('脚本名称不能为空', 'warning');
            return;
        }
        try {
            const parsed = JSON.parse(permissions);
            if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw new Error();
        } catch {
            showNotice('权限配置必须是 JSON 对象');
            return;
        }

        setSaving(true);
        try {
            const id = await api.saveTreeNode({
                nodeType: 'script',
                nodeId: current.id,
                nodeName: current.name.trim(),
                serviceName: service,
                content: current.content,
                permissions,
                description: current.name.trim(),
            });
            await refreshTree(service);
            await loadScript(id);
            showNotice('脚本已保存', 'success');
        } catch (error) {
            showNotice(`保存失败：${messageOf(error)}`);
        } finally {
            setSaving(false);
        }
    };

    const executeScript = async () => {
        const current = validateScript();
        if (!current) return;
        if (!current.canInvoke) {
            showNotice('你没有执行此脚本的权限');
            return;
        }
        if (!service || !environment) {
            showNotice('请选择执行环境和应用服务', 'warning');
            return;
        }

        const params = Object.fromEntries(parameterNames.map((name) => [
            name,
            parameterValues[name]?.trim() ? parameterValues[name] : 'null',
        ]));
        setExecuting(true);
        setResult('脚本执行中，请稍候…');
        try {
            const output = await api.executeScript({
                service,
                env: environment,
                scriptId: current.id,
                script: current.content,
                params: JSON.stringify(params),
            });
            setResult(output || '执行成功（无返回内容）');
            showNotice('脚本执行成功', 'success');
        } catch (error) {
            setResult(`执行失败\n${messageOf(error)}`);
            showNotice(`执行失败：${messageOf(error)}`);
        } finally {
            setExecuting(false);
        }
    };

    const openPreview = async () => {
        const current = validateScript();
        if (!current) return;
        try {
            setPreview(await api.previewScript(current.content, parameterValues));
        } catch (error) {
            showNotice(`预览失败：${messageOf(error)}`);
        }
    };

    const openHistory = async (page = 1) => {
        if (!script?.id) {
            showNotice('请先选择一个脚本', 'warning');
            return;
        }
        try {
            const response = await api.getHistory(script.id, page, 10);
            setHistoryItems(response.data);
            setHistoryPage(page);
            setHistoryTotal(response.totalElements);
            setSelectedHistory(undefined);
        } catch (error) {
            showNotice(`历史记录加载失败：${messageOf(error)}`);
        }
    };

    const confirmCreate = async () => {
        if (!createDialog || !service || !createDialog.name.trim()) return;
        const request: TreeNodeSaveRequest = {
            nodeType: createDialog.type,
            nodeName: createDialog.name.trim(),
            parentId: createDialog.parent?.id,
            serviceName: service,
        };
        if (createDialog.type === 'script') {
            request.content = DEFAULT_SCRIPT;
            request.permissions = '{}';
            request.description = request.nodeName;
        }
        try {
            const id = await api.saveTreeNode(request);
            setCreateDialog(undefined);
            await refreshTree(service);
            if (request.nodeType === 'script') await loadScript(id);
            showNotice(`${request.nodeType === 'folder' ? '文件夹' : '脚本'}已创建`, 'success');
        } catch (error) {
            showNotice(`创建失败：${messageOf(error)}`);
        }
    };

    const confirmRename = async () => {
        if (!renameDialog || !renameDialog.name.trim()) return;
        try {
            await api.saveTreeNode({
                nodeType: renameDialog.node.type,
                nodeId: renameDialog.node.id,
                nodeName: renameDialog.name.trim(),
                serviceName: service,
            });
            if (script?.id === renameDialog.node.id) updateScript({name: renameDialog.name.trim()});
            setRenameDialog(undefined);
            await refreshTree(service);
            showNotice('名称已更新', 'success');
        } catch (error) {
            showNotice(`重命名失败：${messageOf(error)}`);
        }
    };

    const confirmDelete = async () => {
        if (!deleteDialog) return;
        try {
            await api.deleteTreeNode(deleteDialog.node.id, deleteDialog.forceDelete);
            if (script?.id === deleteDialog.node.id) setScript(undefined);
            setDeleteDialog(undefined);
            await refreshTree(service);
            showNotice('节点已删除', 'success');
        } catch (error) {
            showNotice(`删除失败：${messageOf(error)}`);
        }
    };

    const restoreParameters = (serialized?: string) => {
        try {
            const restored = JSON.parse(serialized || '{}') as Record<string, unknown>;
            setParameterValues(Object.fromEntries(parameterNames.map((name) => [
                name,
                restored[name] == null ? '' : String(restored[name]),
            ])));
            setSelectedHistory(undefined);
            setHistoryItems(undefined);
            showNotice('执行参数已恢复', 'success');
        } catch {
            showNotice('历史参数不是有效 JSON，无法恢复');
        }
    };

    const copyText = async (text: string) => {
        try {
            await navigator.clipboard.writeText(text);
            showNotice('已复制到剪贴板', 'success');
        } catch {
            showNotice('浏览器未授权访问剪贴板');
        }
    };

    return (
        <main className="app-shell">
            <aside className="sidebar">
                <header className="brand">
                    <span className="brand-mark">MC</span>
                    <span><strong>Maintain Console</strong><small>远程脚本运维平台</small></span>
                    <button className="icon-button" type="button" aria-label="使用帮助"
                            onClick={() => setShowHelp(true)}>?
                    </button>
                </header>

                <section className="context-selectors">
                    {isProduction && <div className="production-warning">生产环境 · 谨慎操作</div>}
                    <label>
                        <span>执行环境</span>
                        <select value={environment} onChange={(event) => setEnvironment(event.target.value)}>
                            <option value="">请选择环境</option>
                            {login?.availableEnvironments.map((item) => <option key={item.value}
                                                                                value={item.value}>{item.name}</option>)}
                        </select>
                    </label>
                    <label>
                        <span>应用服务</span>
                        <select value={service} onChange={(event) => setService(event.target.value)}>
                            <option value="">请选择服务</option>
                            {services.map((item) => <option key={item} value={item}>{item}</option>)}
                        </select>
                    </label>
                </section>

                <section className="explorer">
                    <div className="explorer-heading">
                        <span><small>脚本资源</small><strong>{service || '未选择服务'}</strong></span>
                        <button type="button" disabled={!service}
                                onClick={() => setCreateDialog({type: 'folder', name: ''})}>+ 新建
                        </button>
                    </div>
                    <input className="search-input" value={search} onChange={(event) => setSearch(event.target.value)}
                           placeholder="搜索文件夹或脚本" aria-label="搜索目录树"/>
                    <div className="tree-scroll">
                        {loadingTree ? <p className="empty-hint">目录加载中…</p> : !service ? (
                            <p className="empty-hint">选择应用服务后查看脚本</p>
                        ) : visibleTree.length ? (
                            <DirectoryTree
                                nodes={visibleTree}
                                selectedId={script?.id}
                                searching={Boolean(search.trim())}
                                onSelect={(node) => void loadScript(node.id)}
                                onCreate={(parent) => setCreateDialog({
                                    parent,
                                    type: (parent.level ?? 0) >= 1 ? 'script' : 'folder',
                                    name: ''
                                })}
                                onRename={(node) => setRenameDialog({node, name: node.name})}
                                onDelete={(node) => setDeleteDialog({node, forceDelete: false})}
                            />
                        ) : <p className="empty-hint">{search ? '没有匹配结果' : '暂无脚本资源'}</p>}
                    </div>
                </section>

                <footer className="user-card">
                    <span className="avatar">{login?.employeeName?.slice(0, 1) || 'U'}</span>
                    <span><strong>{login?.employeeName || '加载中…'}</strong><small>{login?.employeeNo || '—'}</small></span>
                    <span className="profile-tag">{login?.env || '—'}</span>
                </footer>
            </aside>

            <section className="workspace">
                <header className="workspace-header">
                    <div>
                        <p className="eyebrow">SCRIPT WORKSPACE</p>
                        <h1>{script?.name || '脚本工作台'}</h1>
                    </div>
                    <div className="header-context">
                        <span>{selectedEnvironment?.name || '未选环境'}</span><b>/</b><span>{service || '未选服务'}</span>
                    </div>
                </header>

                {!script ? (
                    <div className="welcome-card">
                        <div className="welcome-symbol">⌁</div>
                        <h2>选择一个脚本开始工作</h2>
                        <p>在左侧选择环境、应用服务和脚本。你也可以新建目录，将常用运维操作沉淀为可审计的 Groovy 脚本。</p>
                    </div>
                ) : (
                    <div className="editor-layout">
                        <section className="panel script-panel">
                            <div className="panel-heading">
                                <div className="script-title">
                                    <input value={script.name} disabled={!script.canEdit}
                                           onChange={(event) => updateScript({name: event.target.value})}
                                           aria-label="脚本名称"/>
                                    <span className="permission-badges">
                    <b className={script.canRead ? 'allowed' : ''}>读</b>
                    <b className={script.canEdit ? 'allowed' : ''}>编</b>
                    <b className={script.canInvoke ? 'allowed' : ''}>执</b>
                  </span>
                                </div>
                                <div className="panel-actions">
                                    <span className="modified-at">更新于 {formatTime(script.updateTime)}</span>
                                    <button type="button" onClick={() => void openPreview()}>预览替换</button>
                                    <button type="button" onClick={() => void openHistory()}>执行历史</button>
                                    <button className="primary" type="button" disabled={!script.canEdit || saving}
                                            onClick={() => void saveScript()}>{saving ? '保存中…' : '保存脚本'}</button>
                                </div>
                            </div>

                            <label className="field-label" htmlFor="permissions">权限配置 <span>JSON · readerNo / editorNo / invokerNo</span></label>
                            <textarea id="permissions" className="permission-editor" rows={3} value={permissions}
                                      disabled={!script.canEdit}
                                      onChange={(event) => setPermissions(event.target.value)} spellCheck={false}/>

                            <div className="code-heading">
                                <span>Groovy 脚本</span><small>{script.content.split('\n').length} 行 · 参数占位符
                                $${'{name}'}</small></div>
                            <textarea className="code-editor" value={script.content} disabled={!script.canEdit}
                                      onChange={(event) => updateScript({content: event.target.value})}
                                      spellCheck={false} aria-label="Groovy 脚本内容"/>
                        </section>

                        <section className="panel parameter-panel">
                            <div className="section-title"><span><i
                                className="blue-dot"/>脚本参数</span><small>根据代码中的占位符自动生成</small></div>
                            {parameterNames.length ? (
                                <div className="parameter-grid">
                                    {parameterNames.map((name) => (
                                        <label key={name}>
                                            <span>{name}</span>
                                            <input value={parameterValues[name] || ''}
                                                   onChange={(event) => setParameterValues((current) => ({
                                                       ...current,
                                                       [name]: event.target.value
                                                   }))} placeholder={`输入 ${name}，留空按 null 执行`}/>
                                            {parameterValues[name] && <button type="button" aria-label={`清空 ${name}`}
                                                                              onClick={() => setParameterValues((current) => ({
                                                                                  ...current,
                                                                                  [name]: ''
                                                                              }))}>×</button>}
                                        </label>
                                    ))}
                                </div>
                            ) : <p className="inline-empty">当前脚本没有动态参数</p>}
                        </section>

                        <section className="panel result-panel">
                            <div className="section-title">
                                <span><i
                                    className={result.startsWith('执行失败') ? 'red-dot' : 'green-dot'}/>执行结果</span>
                                <button className="run-button" type="button" disabled={!script.canInvoke || executing}
                                        onClick={() => void executeScript()}>{executing ? '执行中…' : '▶ 执行脚本'}</button>
                            </div>
                            <pre className="console-output">{result}</pre>
                        </section>
                    </div>
                )}
            </section>

            {notice && <div className={`toast ${notice.type}`} role="status">{notice.message}</div>}

            {showHelp && (
                <Modal title="Maintain Console 使用指南" wide onClose={() => setShowHelp(false)}>
                    <div className="help-content">
                        <p>Maintain Console 用于在线编写、沉淀并远程执行 Groovy
                            运维脚本，适合查询数据、排障和经授权的数据修复。</p>
                        <ol>
                            <li>选择执行环境与应用服务。</li>
                            <li>从目录树选择脚本，或在目录下新建脚本。</li>
                            <li>使用 <code>$${'{参数名}'}</code> 声明动态参数，通过 <code>ctx</code> 获取 Spring Bean。
                            </li>
                            <li>预览替换后的代码，确认目标环境后执行；结果会进入执行历史。</li>
                        </ol>
                        <div className="safety-note"><strong>安全提示</strong>生产环境执行前，务必复核脚本、参数、权限与目标服务。
                        </div>
                    </div>
                </Modal>
            )}

            {createDialog && (
                <Modal title="新建资源" onClose={() => setCreateDialog(undefined)} footer={<>
                    <button type="button" onClick={() => setCreateDialog(undefined)}>取消</button>
                    <button className="primary" type="button" disabled={!createDialog.name.trim()}
                            onClick={() => void confirmCreate()}>创建
                    </button>
                </>}>
                    <div className="form-stack">
                        <label><span>类型</span><select value={createDialog.type}
                                                        disabled={(createDialog.parent?.level ?? 0) >= 1}
                                                        onChange={(event) => setCreateDialog({
                                                            ...createDialog,
                                                            type: event.target.value as DirectoryNode['type']
                                                        })}>
                            <option value="folder">文件夹</option>
                            <option value="script">脚本</option>
                        </select></label>
                        <label><span>名称</span><input autoFocus value={createDialog.name}
                                                       onChange={(event) => setCreateDialog({
                                                           ...createDialog,
                                                           name: event.target.value
                                                       })} onKeyDown={(event) => {
                            if (event.key === 'Enter') void confirmCreate();
                        }} placeholder="请输入名称"/></label>
                        <p>创建位置：{createDialog.parent?.name || '根目录'}</p>
                    </div>
                </Modal>
            )}

            {renameDialog && (
                <Modal title={`重命名${renameDialog.node.type === 'folder' ? '文件夹' : '脚本'}`}
                       onClose={() => setRenameDialog(undefined)} footer={<>
                    <button type="button" onClick={() => setRenameDialog(undefined)}>取消</button>
                    <button className="primary" type="button" disabled={!renameDialog.name.trim()}
                            onClick={() => void confirmRename()}>保存
                    </button>
                </>}>
                    <div className="form-stack"><label><span>新名称</span><input autoFocus value={renameDialog.name}
                                                                                 onChange={(event) => setRenameDialog({
                                                                                     ...renameDialog,
                                                                                     name: event.target.value
                                                                                 })} onKeyDown={(event) => {
                        if (event.key === 'Enter') void confirmRename();
                    }}/></label></div>
                </Modal>
            )}

            {deleteDialog && (
                <Modal title="确认删除" onClose={() => setDeleteDialog(undefined)} footer={<>
                    <button type="button" onClick={() => setDeleteDialog(undefined)}>取消</button>
                    <button className="danger-button" type="button"
                            disabled={deleteDialog.node.type === 'folder' && !deleteDialog.forceDelete}
                            onClick={() => void confirmDelete()}>删除
                    </button>
                </>}>
                    <div className="delete-copy">
                        <p>确定删除 <strong>{deleteDialog.node.name}</strong>？此操作不可撤销。</p>
                        {deleteDialog.node.type === 'folder' &&
                            <label><input type="checkbox" checked={deleteDialog.forceDelete}
                                          onChange={(event) => setDeleteDialog({
                                              ...deleteDialog,
                                              forceDelete: event.target.checked
                                          })}/>我确认同时删除该文件夹下的全部内容</label>}
                    </div>
                </Modal>
            )}

            {preview !== undefined && (
                <Modal title="参数替换预览" wide onClose={() => setPreview(undefined)} footer={<>
                    <button type="button" onClick={() => void copyText(preview)}>复制代码</button>
                    <button className="primary" type="button" onClick={() => setPreview(undefined)}>完成</button>
                </>}>
                    <pre className="preview-code">{preview}</pre>
                </Modal>
            )}

            {historyItems !== undefined && !selectedHistory && (
                <Modal title="执行历史" wide onClose={() => setHistoryItems(undefined)} footer={<>
                    <span
                        className="pagination-summary">共 {historyTotal} 条，第 {historyPage} / {Math.max(1, Math.ceil(historyTotal / 10))} 页</span>
                    <button type="button" disabled={historyPage <= 1}
                            onClick={() => void openHistory(historyPage - 1)}>上一页
                    </button>
                    <button type="button" disabled={historyPage * 10 >= historyTotal}
                            onClick={() => void openHistory(historyPage + 1)}>下一页
                    </button>
                </>}>
                    {historyItems.length ? <div className="table-scroll">
                        <table>
                            <thead>
                            <tr>
                                <th>执行时间</th>
                                <th>执行人</th>
                                <th>耗时</th>
                                <th>状态</th>
                            </tr>
                            </thead>
                            <tbody>{historyItems.map((item) => <tr key={item.id} tabIndex={0}
                                                                   onClick={() => setSelectedHistory(item)}
                                                                   onKeyDown={(event) => {
                                                                       if (event.key === 'Enter') setSelectedHistory(item);
                                                                   }}>
                                <td>{formatTime(item.startTime)}</td>
                                <td>{item.executorName}</td>
                                <td>{item.duration} ms</td>
                                <td><span className={`status-pill ${item.status}`}>{item.status}</span></td>
                            </tr>)}</tbody>
                        </table>
                    </div> : <p className="empty-hint">暂无执行历史</p>}
                </Modal>
            )}

            {selectedHistory && (
                <Modal title="执行历史详情" wide onClose={() => {
                    setSelectedHistory(undefined);
                    setHistoryItems(undefined);
                }} footer={<>
                    <button type="button" onClick={() => setSelectedHistory(undefined)}>返回列表</button>
                    <button className="primary" type="button"
                            onClick={() => restoreParameters(selectedHistory.parameters)}>恢复参数
                    </button>
                </>}><HistoryDetail history={selectedHistory}/></Modal>
            )}
        </main>
    );
}
