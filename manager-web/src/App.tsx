import {lazy, Suspense, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {api} from './api';
import AiAssistantModal from './AiAssistantModal';
import DirectoryTree from './DirectoryTree';
import ExecutionTaskPanel from './ExecutionTaskPanel';
import Modal from './Modal';
import ParameterForm from './ParameterForm';
import ResultRenderer from './ResultRenderer';
import {executionParameters, parameterDefinitions, parseParameterSchema} from './parameters';
import {extractParameters, filterTree} from './tree';
import type {
    DirectoryNode,
    ExecutionApproval,
    ExecutionHistory,
    ExecutionTask,
    ExecutionTaskRequest,
    LoginInfo,
    NoticeType,
    RuntimeMetadata,
    ScriptDetail,
    ScriptExecutionResult,
    ScriptResourceOverview,
    ScriptRevision,
    ServiceInstance,
    TargetSelectionMode,
    TreeNodeSaveRequest,
} from './types';

const CodeEditor = lazy(() => import('./CodeEditor'));

const DEFAULT_SCRIPT = `// 通过 _log 输出过程日志；ctx 仅能访问客户端显式开放的 Bean
_log.info('Maintain Console script started')
return toJson([
    protocolVersion: 1,
    blocks: [[type: 'text', title: '执行结果', data: 'hello maintain console']]
])`;

type Notice = { type: NoticeType; message: string };
type CreateDialog = { parent?: DirectoryNode; type: DirectoryNode['type']; name: string };
type RenameDialog = { node: DirectoryNode; name: string };
type DeleteDialog = { node: DirectoryNode; forceDelete: boolean };
type ApprovalDialog = { reason: string; confirmation: string };
type ApprovalDecisionDialog = { approval: ExecutionApproval; approved: boolean; comment: string };

function messageOf(error: unknown): string {
    return error instanceof Error ? error.message : '未知错误';
}

function formatTime(value?: string): string {
    if (!value) return '—';
    return value.replace('T', ' ');
}

function isTerminalTask(task: ExecutionTask): boolean {
    return ['SUCCESS', 'FAILED', 'PARTIAL_SUCCESS', 'CANCELLED', 'TIMED_OUT'].includes(task.status);
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
                ['结构化结果', history.resultPayload],
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
    const [resourceView, setResourceView] = useState<'all' | 'favorites' | 'recent'>('all');
    const [resourceOverview, setResourceOverview] = useState<ScriptResourceOverview>({favorites: [], recent: []});
    const [script, setScript] = useState<ScriptDetail>();
    const [permissions, setPermissions] = useState('{}');
    const [parameterSchema, setParameterSchema] = useState('');
    const [parameterValues, setParameterValues] = useState<Record<string, string>>({});
    const [instances, setInstances] = useState<ServiceInstance[]>([]);
    const [runtimeMetadata, setRuntimeMetadata] = useState<RuntimeMetadata>();
    const [selectionMode, setSelectionMode] = useState<TargetSelectionMode>('RANDOM');
    const [instanceId, setInstanceId] = useState('');
    const [timeoutSeconds, setTimeoutSeconds] = useState(180);
    const [executionTask, setExecutionTask] = useState<ExecutionTask>();
    const [executionApproval, setExecutionApproval] = useState<ExecutionApproval>();
    const [productionConfirmation, setProductionConfirmation] = useState('');
    const [approvalDialog, setApprovalDialog] = useState<ApprovalDialog>();
    const [pendingApprovals, setPendingApprovals] = useState<ExecutionApproval[]>();
    const [approvalDecision, setApprovalDecision] = useState<ApprovalDecisionDialog>();
    const [result, setResult] = useState<ScriptExecutionResult | string>('等待执行脚本…');
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
    const [revisions, setRevisions] = useState<ScriptRevision[]>();
    const [selectedRevision, setSelectedRevision] = useState<ScriptRevision>();
    const [showAiAssistant, setShowAiAssistant] = useState(false);
    const noticeTimer = useRef<number | undefined>(undefined);
    const stopWatchingTask = useRef<() => void>(() => undefined);

    const showNotice = useCallback((message: string, type: NoticeType = 'error') => {
        window.clearTimeout(noticeTimer.current);
        setNotice({message, type});
        noticeTimer.current = window.setTimeout(() => setNotice(undefined), 2800);
    }, []);

    useEffect(() => () => {
        window.clearTimeout(noticeTimer.current);
        stopWatchingTask.current();
    }, []);

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

    const refreshResourceOverview = useCallback(async (serviceName: string) => {
        if (!serviceName) {
            setResourceOverview({favorites: [], recent: []});
            return;
        }
        try {
            setResourceOverview(await api.getResourceOverview(serviceName));
        } catch (error) {
            showNotice(`快捷资源加载失败：${messageOf(error)}`);
        }
    }, [showNotice]);

    useEffect(() => {
        stopWatchingTask.current();
        setScript(undefined);
        setParameterSchema('');
        setParameterValues({});
        setResult('等待执行脚本…');
        setExecutionTask(undefined);
        setExecutionApproval(undefined);
        setProductionConfirmation('');
        setExecuting(false);
        setShowAiAssistant(false);
        void refreshTree(service);
        void refreshResourceOverview(service);
    }, [refreshResourceOverview, refreshTree, service]);

    useEffect(() => {
        let active = true;
        setInstanceId('');
        if (!service || !environment) {
            setInstances([]);
            return () => {
                active = false;
            };
        }
        api.listInstances(service, environment)
            .then((availableInstances) => {
                if (active) setInstances(availableInstances);
            })
            .catch((error) => {
                if (active) {
                    setInstances([]);
                    showNotice(`实例加载失败：${messageOf(error)}`);
                }
            });
        return () => {
            active = false;
        };
    }, [environment, service, showNotice]);

    useEffect(() => {
        let active = true;
        if (!service || !environment) {
            setRuntimeMetadata(undefined);
            return () => {
                active = false;
            };
        }
        api.getRuntimeMetadata(service, environment, selectionMode === 'SPECIFIC' ? instanceId : undefined)
            .then((metadata) => {
                if (active) setRuntimeMetadata(metadata);
            })
            .catch(() => {
                if (active) setRuntimeMetadata(undefined);
            });
        return () => {
            active = false;
        };
    }, [environment, instanceId, selectionMode, service]);

    const loadScript = useCallback(async (scriptId: string) => {
        try {
            const detail = await api.getScriptDetail(scriptId);
            setScript(detail);
            setPermissions(detail.permissions || '{}');
            setParameterSchema(detail.parameterSchema || '');
            setParameterValues({});
            setResult('等待执行脚本…');
            setExecutionTask(undefined);
            setExecutionApproval(undefined);
            setProductionConfirmation('');
            setShowAiAssistant(false);
            void refreshResourceOverview(service);
        } catch (error) {
            showNotice(`脚本加载失败：${messageOf(error)}`);
        }
    }, [refreshResourceOverview, service, showNotice]);

    const parsedParameterSchema = useMemo(() => {
        try {
            return parseParameterSchema(parameterSchema);
        } catch {
            return undefined;
        }
    }, [parameterSchema]);
    const definitions = useMemo(
        () => parameterDefinitions(script?.content || '', parsedParameterSchema),
        [parsedParameterSchema, script?.content],
    );
    const visibleTree = useMemo(() => filterTree(tree, search), [search, tree]);
    const visibleShortcuts = useMemo(() => {
        const items = resourceView === 'favorites' ? resourceOverview.favorites : resourceOverview.recent;
        const keyword = search.trim().toLowerCase();
        return keyword ? items.filter((item) => item.name.toLowerCase().includes(keyword)) : items;
    }, [resourceOverview, resourceView, search]);
    const selectedEnvironment = login?.availableEnvironments.find((item) => item.value === environment);
    const isProduction = Boolean(selectedEnvironment?.production);

    useEffect(() => {
        setParameterValues((current) => Object.fromEntries(
            definitions.map((definition) => [
                definition.name,
                current[definition.name] ?? (definition.defaultValue == null ? '' : String(definition.defaultValue)),
            ]),
        ));
    }, [definitions]);

    const updateScript = (patch: Partial<ScriptDetail>) => {
        setScript((current) => current ? {...current, ...patch} : current);
    };

    const scriptIsFavorite = Boolean(script && resourceOverview.favorites.some((item) => item.id === script.id));

    const toggleFavorite = async () => {
        if (!script) return;
        try {
            await api.setFavorite(script.id, !scriptIsFavorite);
            await refreshResourceOverview(service);
            showNotice(scriptIsFavorite ? '已取消收藏' : '已加入收藏', 'success');
        } catch (error) {
            showNotice(`收藏更新失败：${messageOf(error)}`);
        }
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

    const validatedParameterSchema = (): string | null => {
        if (!parameterSchema.trim()) return '';
        try {
            const schema = parseParameterSchema(parameterSchema);
            const placeholders = extractParameters(script?.content || '');
            const declared = schema?.parameters.map((parameter) => parameter.name) || [];
            if (placeholders.length !== declared.length || placeholders.some((name) => !declared.includes(name))) {
                throw new Error('参数名称必须与脚本占位符完全一致');
            }
            return JSON.stringify(schema);
        } catch (error) {
            showNotice(`参数 Schema 无效：${messageOf(error)}`);
            return null;
        }
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
        const normalizedParameterSchema = validatedParameterSchema();
        if (normalizedParameterSchema === null) return;

        setSaving(true);
        try {
            const id = await api.saveTreeNode({
                nodeType: 'script',
                nodeId: current.id,
                nodeName: current.name.trim(),
                serviceName: service,
                content: current.content,
                parameterSchema: normalizedParameterSchema,
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

    const buildExecutionRequest = (): ExecutionTaskRequest | undefined => {
        const current = validateScript();
        if (!current) return undefined;
        if (!current.canInvoke) {
            showNotice('你没有执行此脚本的权限');
            return undefined;
        }
        if (!service || !environment) {
            showNotice('请选择执行环境和应用服务', 'warning');
            return undefined;
        }
        if (selectionMode === 'SPECIFIC' && !instanceId) {
            showNotice('请选择要执行的服务实例', 'warning');
            return undefined;
        }
        const normalizedParameterSchema = validatedParameterSchema();
        if (normalizedParameterSchema === null) return undefined;
        const params = executionParameters(definitions, parameterValues, Boolean(normalizedParameterSchema));
        return {
            service,
            env: environment,
            scriptId: current.id,
            script: current.content,
            params: JSON.stringify(params),
            parameterSchema: normalizedParameterSchema,
            selectionMode,
            instanceId: selectionMode === 'SPECIFIC' ? instanceId : undefined,
            timeoutSeconds,
        };
    };

    const executeScript = async () => {
        const request = buildExecutionRequest();
        if (!request) return;
        if (isProduction) {
            if (!executionApproval) {
                setApprovalDialog({reason: '', confirmation: ''});
                return;
            }
            try {
                const latestApproval = await api.getExecutionApproval(executionApproval.id);
                setExecutionApproval(latestApproval);
                if (latestApproval.status !== 'APPROVED') {
                    showNotice(`生产审批当前状态：${latestApproval.status}`, 'warning');
                    return;
                }
                request.approvalId = latestApproval.id;
                request.productionConfirmation = productionConfirmation;
            } catch (error) {
                showNotice(`审批状态查询失败：${messageOf(error)}`);
                return;
            }
        }
        setExecuting(true);
        setExecutionTask(undefined);
        setResult('正在创建执行任务…');
        try {
            const handleUpdate = (task: ExecutionTask) => {
                setExecutionTask(task);
                if (!isTerminalTask(task)) return;
                setExecuting(false);
                const onlyTarget = task.targets.length === 1 ? task.targets[0] : undefined;
                if (onlyTarget?.result) setResult(onlyTarget.result);
                if (task.status === 'SUCCESS' || task.status === 'PARTIAL_SUCCESS') {
                    showNotice(task.status === 'SUCCESS' ? '脚本执行成功' : '脚本部分执行成功',
                        task.status === 'SUCCESS' ? 'success' : 'warning');
                } else {
                    showNotice(`执行结束：${task.errorMessage || task.status}`,
                        task.status === 'CANCELLED' ? 'warning' : 'error');
                }
            };
            const task = await api.createExecutionTask(request);
            setExecutionApproval(undefined);
            setProductionConfirmation('');
            handleUpdate(task);
            if (!isTerminalTask(task)) {
                stopWatchingTask.current();
                stopWatchingTask.current = api.watchExecutionTask(task.id, handleUpdate, (error) => {
                    setExecuting(false);
                    showNotice(`执行任务连接失败：${messageOf(error)}`);
                });
            }
        } catch (error) {
            setResult(`执行失败\n${messageOf(error)}`);
            showNotice(`执行失败：${messageOf(error)}`);
            setExecuting(false);
        }
    };

    const requestProductionApproval = async () => {
        if (!approvalDialog) return;
        const request = buildExecutionRequest();
        if (!request) return;
        const expectedConfirmation = `PRODUCTION:${service}:${script?.name || ''}`;
        if (approvalDialog.confirmation !== expectedConfirmation) {
            showNotice(`确认文本不匹配，请输入 ${expectedConfirmation}`, 'warning');
            return;
        }
        try {
            const approval = await api.createExecutionApproval(request, approvalDialog.reason.trim());
            setExecutionApproval(approval);
            setProductionConfirmation(approvalDialog.confirmation);
            setApprovalDialog(undefined);
            setResult(`生产执行审批已提交\n审批单：${approval.id}\n当前状态：${approval.status}`);
            showNotice('生产执行审批已提交', 'success');
        } catch (error) {
            showNotice(`审批申请失败：${messageOf(error)}`);
        }
    };

    const openPendingApprovals = async () => {
        try {
            setPendingApprovals(await api.listPendingApprovals());
        } catch (error) {
            showNotice(`待审批列表加载失败：${messageOf(error)}`);
        }
    };

    const decideApproval = async () => {
        if (!approvalDecision) return;
        try {
            await api.decideExecutionApproval(approvalDecision.approval.id, approvalDecision.approved,
                approvalDecision.comment.trim());
            setApprovalDecision(undefined);
            await openPendingApprovals();
            showNotice(approvalDecision.approved ? '已批准执行' : '已驳回执行', 'success');
        } catch (error) {
            showNotice(`审批失败：${messageOf(error)}`);
        }
    };

    const cancelExecution = async () => {
        if (!executionTask || isTerminalTask(executionTask)) return;
        try {
            setExecutionTask(await api.cancelExecutionTask(executionTask.id));
            showNotice('已提交取消请求', 'warning');
        } catch (error) {
            showNotice(`取消失败：${messageOf(error)}`);
        }
    };

    const openPreview = async () => {
        const current = validateScript();
        if (!current) return;
        const normalizedParameterSchema = validatedParameterSchema();
        if (normalizedParameterSchema === null) return;
        try {
            setPreview(await api.previewScript(
                current.content,
                executionParameters(definitions, parameterValues, Boolean(normalizedParameterSchema)),
                normalizedParameterSchema,
            ));
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

    const openRevisions = async () => {
        if (!script?.id) return;
        try {
            setRevisions(await api.getScriptRevisions(script.id));
            setSelectedRevision(undefined);
        } catch (error) {
            showNotice(`版本历史加载失败：${messageOf(error)}`);
        }
    };

    const restoreRevision = async () => {
        if (!script?.id || !selectedRevision) return;
        try {
            const version = await api.restoreScriptRevision(script.id, selectedRevision.version);
            await loadScript(script.id);
            setSelectedRevision(undefined);
            setRevisions(undefined);
            showNotice(`已恢复为新版本 v${version}`, 'success');
        } catch (error) {
            showNotice(`版本恢复失败：${messageOf(error)}`);
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
            request.parameterSchema = '{"version":1,"parameters":[]}';
            request.description = request.nodeName;
        }
        try {
            const id = await api.saveTreeNode(request);
            setCreateDialog(undefined);
            await refreshTree(service);
            await refreshResourceOverview(service);
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
            await refreshResourceOverview(service);
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
            await refreshResourceOverview(service);
            showNotice('节点已删除', 'success');
        } catch (error) {
            showNotice(`删除失败：${messageOf(error)}`);
        }
    };

    const restoreParameters = (serialized?: string) => {
        try {
            const restored = JSON.parse(serialized || '{}') as Record<string, unknown>;
            setParameterValues(Object.fromEntries(definitions.map((definition) => [
                definition.name,
                definition.sensitive || restored[definition.name] == null ? '' : String(restored[definition.name]),
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
                    <div className="resource-tabs">
                        <button className={resourceView === 'all' ? 'active' : ''} type="button"
                                onClick={() => setResourceView('all')}>全部
                        </button>
                        <button className={resourceView === 'favorites' ? 'active' : ''} type="button"
                                onClick={() => setResourceView('favorites')}>收藏 {resourceOverview.favorites.length}</button>
                        <button className={resourceView === 'recent' ? 'active' : ''} type="button"
                                onClick={() => setResourceView('recent')}>最近 {resourceOverview.recent.length}</button>
                    </div>
                    <div className="tree-scroll">
                        {loadingTree ? <p className="empty-hint">目录加载中…</p> : !service ? (
                            <p className="empty-hint">选择应用服务后查看脚本</p>
                        ) : resourceView !== 'all' ? (visibleShortcuts.length ? <div className="shortcut-list">
                                    {visibleShortcuts.map((item) => <button type="button" key={item.id}
                                                                            className={script?.id === item.id ? 'active' : ''}
                                                                            onClick={() => void loadScript(item.id)}>
                                        <span>{item.favorite ? '★' : '◷'}</span><strong>{item.name}</strong>
                                        <small>{item.lastOpenTime ? formatTime(item.lastOpenTime) : '已收藏'}</small>
                                    </button>)}</div> :
                                <p className="empty-hint">暂无{resourceView === 'favorites' ? '收藏' : '最近使用'}脚本</p>
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
                        {login?.canApprove &&
                            <button type="button" onClick={() => void openPendingApprovals()}>待审批</button>}
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
                                    <button type="button"
                                            className={scriptIsFavorite ? 'favorite-button active' : 'favorite-button'}
                                            onClick={() => void toggleFavorite()}>{scriptIsFavorite ? '★ 已收藏' : '☆ 收藏'}</button>
                                    {login?.aiEnabled && <button type="button" className="ai-button"
                                                                 onClick={() => setShowAiAssistant(true)}>AI
                                        助手</button>}
                                    <button type="button" onClick={() => void openPreview()}>预览替换</button>
                                    <button type="button" onClick={() => void openRevisions()}>版本历史</button>
                                    <button type="button" onClick={() => void openHistory()}>执行历史</button>
                                    <button className="primary" type="button" disabled={!script.canEdit || saving}
                                            onClick={() => void saveScript()}>{saving ? '保存中…' : '保存脚本'}</button>
                                </div>
                            </div>

                            <label className="field-label" htmlFor="permissions">权限配置 <span>JSON · readerNo / editorNo / invokerNo</span></label>
                            <textarea id="permissions" className="permission-editor" rows={3} value={permissions}
                                      disabled={!script.canEdit}
                                      onChange={(event) => setPermissions(event.target.value)} spellCheck={false}/>

                            <label className="field-label" htmlFor="parameter-schema">参数 Schema <span>JSON · 类型、默认值、校验、敏感值</span></label>
                            <textarea id="parameter-schema" className="permission-editor" rows={5}
                                      value={parameterSchema}
                                      disabled={!script.canEdit}
                                      onChange={(event) => setParameterSchema(event.target.value)} spellCheck={false}
                                      placeholder='{"version":1,"parameters":[{"name":"id","type":"STRING","required":true}]}'/>

                            <div className="code-heading">
                                <span>Groovy 脚本</span><small>{script.content.split('\n').length} 行 · 参数占位符
                                $${'{name}'}</small></div>
                            <Suspense fallback={<div className="code-editor-loading">编辑器加载中…</div>}>
                                <CodeEditor value={script.content} disabled={!script.canEdit}
                                            parameterNames={definitions.map((definition) => definition.name)}
                                            runtimeMetadata={runtimeMetadata}
                                            onChange={(content) => updateScript({content})}/>
                            </Suspense>
                        </section>

                        <section className="panel parameter-panel">
                            <div className="section-title"><span><i
                                className="blue-dot"/>脚本参数</span><small>{parsedParameterSchema ? '按类型化 Schema 生成' : '旧脚本按占位符生成'}</small>
                            </div>
                            <div className="execution-target-config">
                                <label><span>执行模式</span><select value={selectionMode}
                                                                    onChange={(event) => setSelectionMode(event.target.value as TargetSelectionMode)}>
                                    <option value="RANDOM">随机单实例</option>
                                    <option value="SPECIFIC">指定单实例</option>
                                    <option value="ALL">全部实例</option>
                                </select></label>
                                {selectionMode === 'SPECIFIC' && <label><span>目标实例</span><select value={instanceId}
                                                                                                     onChange={(event) => setInstanceId(event.target.value)}>
                                    <option value="">请选择实例</option>
                                    {instances.map((instance) => <option key={instance.id} value={instance.id}>
                                        {instance.id} · {instance.host}:{instance.port}
                                    </option>)}
                                </select></label>}
                                <label><span>超时时间（秒）</span><input type="number" min={1} max={900}
                                                                       value={timeoutSeconds}
                                                                       onChange={(event) => setTimeoutSeconds(Number(event.target.value))}/></label>
                                <small>已发现 {instances.length} 个可用实例</small>
                            </div>
                            <ParameterForm definitions={definitions} values={parameterValues}
                                           onChange={(name, value) => setParameterValues((current) => ({
                                               ...current,
                                               [name]: value
                                           }))}/>
                        </section>

                        <section className="panel result-panel">
                            <div className="section-title">
                                <span><i
                                    className={(typeof result === 'string' && result.startsWith('执行失败')) ||
                                    (typeof result !== 'string' && result.blocks.some((block) => block.type === 'error')) ? 'red-dot' : 'green-dot'}/>执行结果</span>
                                <span className="result-actions">
                                    {executing && executionTask && <button type="button" className="cancel-button"
                                                                           onClick={() => void cancelExecution()}>取消任务</button>}
                                    <button className="run-button" type="button"
                                            disabled={!script.canInvoke || executing}
                                            onClick={() => void executeScript()}>{executing ? '执行中…'
                                        : isProduction && executionApproval ? '检查审批并执行' : '▶ 执行脚本'}</button>
                                </span>
                            </div>
                            {executionTask ? <ExecutionTaskPanel task={executionTask}/> :
                                <ResultRenderer result={result}/>}
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
                            <li>使用 <code>$${'{参数名}'}</code> 声明动态参数；<code>ctx</code> 仅能获取客户端白名单中的
                                Spring Bean。
                            </li>
                            <li>预览替换后的代码，确认目标环境后执行；结果会进入执行历史。</li>
                        </ol>
                        <div className="safety-note"><strong>安全提示</strong>生产环境执行前，务必复核脚本、参数、权限与目标服务。
                        </div>
                    </div>
                </Modal>
            )}

            {showAiAssistant && script && <AiAssistantModal script={script} serviceName={service}
                                                            parameterSchema={parameterSchema}
                                                            onApplyScript={(content) => updateScript({content})}
                                                            onApplyParameterSchema={setParameterSchema}
                                                            onNotice={showNotice}
                                                            onClose={() => setShowAiAssistant(false)}/>}

            {approvalDialog && (
                <Modal title="生产执行审批申请" wide onClose={() => setApprovalDialog(undefined)} footer={<>
                    <button type="button" onClick={() => setApprovalDialog(undefined)}>取消</button>
                    <button className="danger-button" type="button"
                            disabled={!approvalDialog.reason.trim() || !approvalDialog.confirmation.trim()}
                            onClick={() => void requestProductionApproval()}>提交审批
                    </button>
                </>}>
                    <div className="production-approval-form">
                        <div className="safety-note"><strong>生产环境保护</strong>
                            审批仅对当前脚本、参数、服务、实例模式和超时时间有效，任一内容变更都需重新申请。
                        </div>
                        <label><span>申请理由</span><textarea rows={4} value={approvalDialog.reason}
                                                              onChange={(event) => setApprovalDialog({
                                                                  ...approvalDialog,
                                                                  reason: event.target.value
                                                              })}
                                                              placeholder="说明执行目的、影响范围和回滚方式"/></label>
                        <label><span>二次确认，请输入 <code>PRODUCTION:{service}:{script?.name}</code></span>
                            <input value={approvalDialog.confirmation}
                                   onChange={(event) => setApprovalDialog({
                                       ...approvalDialog,
                                       confirmation: event.target.value
                                   })}/>
                        </label>
                    </div>
                </Modal>
            )}

            {pendingApprovals !== undefined && !approvalDecision && (
                <Modal title="待审批的生产执行" wide onClose={() => setPendingApprovals(undefined)}>
                    {pendingApprovals.length ?
                        <div className="approval-list">{pendingApprovals.map((approval) => <section
                            key={approval.id} className="approval-card">
                            <header>
                                <span><strong>{approval.scriptName}</strong><small>{approval.serviceName} · {approval.selectionMode}</small></span>
                                <code>{approval.id}</code></header>
                            <p>{approval.reason}</p>
                            <div className="approval-meta">
                                <span>申请人：{approval.requesterName}（{approval.requesterId}）</span>
                                <span>过期：{formatTime(approval.expireTime)}</span></div>
                            <pre>{approval.scriptContent}</pre>
                            <footer>
                                <button type="button"
                                        onClick={() => setApprovalDecision({approval, approved: false, comment: ''})}>驳回
                                </button>
                                <button className="primary" type="button"
                                        onClick={() => setApprovalDecision({approval, approved: true, comment: ''})}>批准
                                </button>
                            </footer>
                        </section>)}</div> : <p className="empty-hint">暂无待审批申请</p>}
                </Modal>
            )}

            {approvalDecision && (
                <Modal title={approvalDecision.approved ? '批准生产执行' : '驳回生产执行'}
                       onClose={() => setApprovalDecision(undefined)} footer={<>
                    <button type="button" onClick={() => setApprovalDecision(undefined)}>取消</button>
                    <button className={approvalDecision.approved ? 'primary' : 'danger-button'} type="button"
                            disabled={!approvalDecision.comment.trim()} onClick={() => void decideApproval()}>确认
                    </button>
                </>}>
                    <div className="form-stack"><label><span>审批意见</span><textarea rows={4}
                                                                                      value={approvalDecision.comment}
                                                                                      onChange={(event) => setApprovalDecision({
                                                                                          ...approvalDecision,
                                                                                          comment: event.target.value
                                                                                      })}
                                                                                      placeholder="记录判断依据与注意事项"/></label>
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

            {revisions !== undefined && !selectedRevision && (
                <Modal title="脚本版本历史" wide onClose={() => setRevisions(undefined)}>
                    {revisions.length ? <div className="table-scroll">
                            <table>
                                <thead>
                                <tr>
                                    <th>版本</th>
                                    <th>保存人</th>
                                    <th>保存时间</th>
                                </tr>
                                </thead>
                                <tbody>{revisions.map((revision) => <tr key={revision.id} tabIndex={0}
                                                                        onClick={() => setSelectedRevision(revision)}
                                                                        onKeyDown={(event) => {
                                                                            if (event.key === 'Enter') setSelectedRevision(revision);
                                                                        }}>
                                    <td>v{revision.version}</td>
                                    <td>{revision.creatorName}</td>
                                    <td>{formatTime(revision.createTime)}</td>
                                </tr>)}</tbody>
                            </table>
                        </div>
                        : <p className="empty-hint">暂无版本记录</p>}
                </Modal>
            )}

            {selectedRevision && (
                <Modal title={`版本 v${selectedRevision.version}`} wide onClose={() => setSelectedRevision(undefined)}
                       footer={<>
                           <button type="button" onClick={() => setSelectedRevision(undefined)}>返回列表</button>
                           <button className="primary" type="button" disabled={!script?.canEdit}
                                   onClick={() => void restoreRevision()}>恢复为新版本
                           </button>
                       </>}>
                    <div className="revision-compare">
                        <section><h3>当前编辑器</h3>
                            <pre>{script?.content || ''}</pre>
                        </section>
                        <section><h3>历史 v{selectedRevision.version}</h3>
                            <pre>{selectedRevision.content}</pre>
                        </section>
                    </div>
                </Modal>
            )}
        </main>
    );
}
