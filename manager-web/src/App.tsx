import {lazy, Suspense, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {api} from './api';
import AiAssistantModal from './AiAssistantModal';
import ScriptResourceExplorer from './workspace/ScriptResourceExplorer';
import ExecutionResultsPanel, {type ResultView} from './workspace/ExecutionResultsPanel';
import WorkspaceToolbar from './workspace/WorkspaceToolbar';
import './workspace/workspace.css';
import Modal from './Modal';
import ScriptParametersPanel, {type ParameterTab} from './workspace/ScriptParametersPanel';
import type {ExecutionTarget} from './workspace/ExecutionTargetSettings';
import {
    executionParameters,
    parameterDefinitions,
    parameterSchemaIssues,
    parameterValueText,
    parseParameterSchema
} from './parameters';
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
    TreeNodeSaveRequest,
} from './types';

const CodeEditor = lazy(() => import('./CodeEditor'));

const DEFAULT_SCRIPT = [
    '// 在右侧“参数配置”声明参数，在“运行填值”中输入；引用无需额外加引号',
    'def name = $${name}',
    'def count = $${count}',
    '',
    "_log.info('开始生成示例表格')",
    "def rows = (1..count).collect { index -> [index, 'Hello, ' + name] }",
    "return result(resultTable('问候示例', ['序号', '内容'], rows))",
].join('\n');

const DEFAULT_PARAMETER_SCHEMA = JSON.stringify({
    version: 1, parameters: [
        {name: 'name', type: 'STRING', required: true, defaultValue: 'Maintain Console', description: '希望向谁问好'},
        {
            name: 'count',
            type: 'NUMBER',
            required: true,
            defaultValue: 3,
            min: 1,
            max: 20,
            description: '生成多少行示例数据'
        },
    ]
}, null, 2);

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
    const [resourceOverview, setResourceOverview] = useState<ScriptResourceOverview>({favorites: [], recent: []});
    const [script, setScript] = useState<ScriptDetail>();
    const [permissions, setPermissions] = useState('{}');
    const [parameterSchema, setParameterSchema] = useState('');
    const [parameterTab, setParameterTab] = useState<ParameterTab>('values');
    const [resourcesOpen, setResourcesOpen] = useState(() => window.innerWidth >= 1280);
    const [parametersOpen, setParametersOpen] = useState(false);
    const [resultView, setResultView] = useState<ResultView>('collapsed');
    const [showPermissions, setShowPermissions] = useState(false);
    const [savedDraft, setSavedDraft] = useState('');
    const [showExample, setShowExample] = useState(false);
    const [parameterValues, setParameterValues] = useState<Record<string, string>>({});
    const [instances, setInstances] = useState<ServiceInstance[]>([]);
    const [runtimeMetadata, setRuntimeMetadata] = useState<RuntimeMetadata>();
    const [executionTarget, setExecutionTarget] = useState<ExecutionTarget>({
        selectionMode: 'RANDOM',
        instanceId: '',
        timeoutSeconds: 180
    });
    const {selectionMode, instanceId, timeoutSeconds} = executionTarget;
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

    useEffect(() => {
        const wideScreen = window.matchMedia('(min-width: 1280px)');
        const updateResources = (event: MediaQueryListEvent) => setResourcesOpen(event.matches);
        wideScreen.addEventListener('change', updateResources);
        return () => wideScreen.removeEventListener('change', updateResources);
    }, []);

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
        setResultView('collapsed');
        setExecutionApproval(undefined);
        setProductionConfirmation('');
        setExecuting(false);
        setShowAiAssistant(false);
        void refreshTree(service);
        void refreshResourceOverview(service);
    }, [refreshResourceOverview, refreshTree, service]);

    useEffect(() => {
        let active = true;
        setExecutionTarget((current) => ({...current, instanceId: ''}));
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
            setSavedDraft(JSON.stringify([detail.name, detail.content, detail.permissions || '{}', detail.parameterSchema || '']));
            setParameterValues({});
            setResult('等待执行脚本…');
            setExecutionTask(undefined);
            setResultView('collapsed');
            setExecutionApproval(undefined);
            setProductionConfirmation('');
            setShowAiAssistant(false);
            void refreshResourceOverview(service);
        } catch (error) {
            showNotice(`脚本加载失败：${messageOf(error)}`);
        }
    }, [refreshResourceOverview, service, showNotice]);

    const schemaValidation = useMemo(() => {
        try {
            return {schema: parseParameterSchema(parameterSchema), error: ''};
        } catch (error) {
            return {schema: undefined, error: messageOf(error)};
        }
    }, [parameterSchema]);
    const parsedParameterSchema = schemaValidation.schema;
    const definitions = useMemo(
        () => parameterDefinitions(script?.content || '', parsedParameterSchema),
        [parsedParameterSchema, script?.content],
    );
    const selectedEnvironment = login?.availableEnvironments.find((item) => item.value === environment);
    const isProduction = Boolean(selectedEnvironment?.production);
    const draftChanged = Boolean(script && savedDraft !== JSON.stringify([script.name, script.content, permissions, parameterSchema]));
    const schemaIssues = parameterSchemaIssues(script?.content || '', parsedParameterSchema);

    useEffect(() => {
        setParameterTab(definitions.length ? 'values' : 'schema');
        setParametersOpen(false);
    }, [script?.id]);

    useEffect(() => {
        setParameterValues((current) => Object.fromEntries(
            definitions.map((definition) => [
                definition.name,
                current[definition.name] ?? parameterValueText(definition.defaultValue),
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
            const issues = parameterSchemaIssues(script?.content || '', schema);
            if (issues.length) throw new Error(issues.join(' '));
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
        setResultView('open');
        setParametersOpen(false);
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
            request.parameterSchema = DEFAULT_PARAMETER_SCHEMA;
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
                definition.sensitive ? '' : parameterValueText(restored[definition.name]),
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
        <main className={`workbench ${resourcesOpen ? '' : 'resources-collapsed'}`}>
            <header className="workbench-header">
                <button className="icon-button" type="button" aria-label={resourcesOpen ? '收起资源栏' : '展开资源栏'}
                        aria-expanded={resourcesOpen} aria-controls="resource-sidebar"
                        onClick={() => setResourcesOpen(!resourcesOpen)}>☰
                </button>
                <strong className="app-name">Maintain Console</strong>
                <section className="context-selectors">
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
                {isProduction && <span className="production-badge">生产环境</span>}
                <div className="app-header-actions">
                    {login?.canApprove &&
                        <button type="button" onClick={() => void openPendingApprovals()}>待审批</button>}
                    <button className="icon-button" type="button" aria-label="使用帮助"
                            onClick={() => setShowHelp(true)}>?
                    </button>
                </div>
            </header>
            {resourcesOpen && <button className="resource-backdrop" type="button" aria-label="关闭资源栏"
                                      onClick={() => setResourcesOpen(false)}/>}
            <aside className="workbench-sidebar" id="resource-sidebar" aria-label="脚本资源">
                <ScriptResourceExplorer key={service} serviceName={service} tree={tree} overview={resourceOverview}
                                        loading={loadingTree} selectedId={script?.id}
                                        onSelect={(scriptId) => {
                                            void loadScript(scriptId);
                                            if (window.innerWidth < 1280) setResourcesOpen(false);
                                        }}
                                        onCreate={(parent) => setCreateDialog({
                                            parent, type: (parent?.level ?? 0) >= 1 ? 'script' : 'folder', name: '',
                                        })}
                                        onRename={(node) => setRenameDialog({node, name: node.name})}
                                        onDelete={(node) => setDeleteDialog({node, forceDelete: false})}/>

                <footer className="user-card">
                    <span className="avatar">{login?.employeeName?.slice(0, 1) || 'U'}</span>
                    <span><strong>{login?.employeeName || '加载中…'}</strong><small>{login?.employeeNo || '—'}</small></span>
                    <span className="profile-tag">{login?.env || '—'}</span>
                </footer>
            </aside>

            <section className="workbench-main">
                <WorkspaceToolbar script={script} draftChanged={draftChanged} saving={saving}
                                  scriptIsFavorite={scriptIsFavorite} aiEnabled={Boolean(login?.aiEnabled)}
                                  parameterCount={definitions.length} parametersOpen={parametersOpen}
                                  onNameChange={(name) => updateScript({name})}
                                  onParametersToggle={() => {
                                      setResultView((current) => current === 'maximized' ? 'open' : current);
                                      setParametersOpen(!parametersOpen);
                                  }}
                                  onSave={saveScript} onHistory={openHistory} onRevisions={openRevisions}
                                  onFavorite={toggleFavorite}
                                  onPermissions={() => setShowPermissions(true)} onExample={() => setShowExample(true)}
                                  onAiAssistant={() => setShowAiAssistant(true)}/>

                {!script ? <div className="welcome-card">
                    <div className="welcome-symbol">⌁</div>
                    <h2>选择一个脚本开始工作</h2>
                    <p>选择环境与应用服务，再从资源栏打开脚本。左侧编写代码，右侧填参数，下方查看执行结果。</p>
                    <button type="button" className="welcome-resource-button"
                            onClick={() => setResourcesOpen(true)}>打开脚本资源
                    </button>
                </div> : <div className={'workbench-panels result-' + resultView}>
                    <section className="panel workbench-editor" aria-label="脚本编辑区">
                        {(!definitions.length || schemaIssues.length > 0 || schemaValidation.error) &&
                            <div
                                className={'script-guidance ' + (schemaIssues.length || schemaValidation.error ? 'has-issues' : '')}>
                                <span>{schemaValidation.error || schemaIssues.join(' ') || '将每次会变化的值定义为参数，运行时只需填表。'}</span>
                                <button type="button" onClick={() => {
                                    setParameterTab('schema');
                                    setParametersOpen(true);
                                }}>配置参数 →
                                </button>
                            </div>}
                        <Suspense fallback={<div className="code-editor-loading">编辑器加载中…</div>}>
                            <CodeEditor key={script.id} value={script.content} disabled={!script.canEdit}
                                        parameterNames={definitions.map((definition) => definition.name)}
                                        runtimeMetadata={runtimeMetadata}
                                        onChange={(content) => updateScript({content})}/>
                        </Suspense>
                    </section>

                    <ScriptParametersPanel script={script} parameterSchema={parameterSchema}
                                           definitions={definitions} parameterValues={parameterValues}
                                           onSchemaChange={setParameterSchema}
                                           onValueChange={(name, value) => setParameterValues((current) => ({
                                               ...current,
                                               [name]: value
                                           }))}
                                           target={executionTarget} onTargetChange={setExecutionTarget}
                                           instances={instances}
                                           environment={selectedEnvironment}
                                           draftChanged={draftChanged} executing={executing}
                                           hasApproval={Boolean(executionApproval)}
                                           parameterTab={parameterTab} onTabChange={setParameterTab}
                                           parametersOpen={parametersOpen} onClose={() => setParametersOpen(false)}
                                           onPreview={openPreview} onExecute={executeScript}
                                           onExample={() => setShowExample(true)}
                                           onEditScript={() => {
                                               setParametersOpen(false);
                                               document.querySelector<HTMLElement>('[aria-label="Groovy 脚本内容"]')?.focus({preventScroll: true});
                                           }}/>

                    <ExecutionResultsPanel executionTask={executionTask} result={result} executing={executing}
                                           resultView={resultView} onViewChange={setResultView}
                                           onCancel={cancelExecution}/>
                </div>}
            </section>

            {notice && <div className={`toast ${notice.type}`} role="status">{notice.message}</div>}

            {showPermissions && script && <Modal title="脚本权限设置" onClose={() => setShowPermissions(false)}
                                                 footer={<button type="button"
                                                                 onClick={() => setShowPermissions(false)}>完成，返回工作台</button>}>
                <p className="schema-footnote">控制脚本的读取、编辑与执行；修改后需点击“保存脚本”。</p>
                <label className="field-label"
                       htmlFor="permissions">权限配置 <span>readerNo / editorNo / invokerNo</span></label>
                <textarea id="permissions" className="permission-editor" rows={6} value={permissions}
                          disabled={!script.canEdit} onChange={(event) => setPermissions(event.target.value)}
                          spellCheck={false}/>
                <p className="schema-footnote">更新于 {formatTime(script.updateTime)}</p>
            </Modal>}

            {showHelp && (
                <Modal title="Maintain Console 使用指南" wide onClose={() => setShowHelp(false)}>
                    <div className="help-content">
                        <p>Maintain Console 用于在线编写、沉淀并远程执行 Groovy
                            运维脚本，适合查询数据、排障和经授权的数据修复。</p>
                        <ol>
                            <li>选择执行环境与应用服务。</li>
                            <li>从目录树选择脚本，或在目录下新建脚本。</li>
                            <li>打开右侧“参数配置”，添加名称、类型、用途说明和默认值，无需手写 Schema。</li>
                            <li>在脚本中使用 <code>{'def count = $${count}'}</code> 引用参数，不要额外加引号。
                                也可以先写占位符，再点击“从脚本识别”。
                            </li>
                            <li>切换右侧“运行填值”，数字、下拉选项、是/否等会按类型显示；本次输入不改变默认值。</li>
                            <li>点击“代码补全”或按 Ctrl + Space 查看提示；输入 <code>_log.</code> 查看日志方法。
                                <code>ctx</code> 仅能获取客户端白名单中的 Spring Bean。
                            </li>
                            <li>预览替换后的代码，确认目标环境后执行；结果会进入执行历史。</li>
                        </ol>
                        <div className="safety-note"><strong>安全提示</strong>生产环境执行前，务必复核脚本、参数、权限与目标服务。
                        </div>
                    </div>
                </Modal>
            )}

            {showExample && script && <Modal title="入门示例：参数 → 表单 → 表格结果" wide
                                             onClose={() => setShowExample(false)} footer={<>
                <button type="button" onClick={() => setShowExample(false)}>关闭</button>
                <button className="primary" type="button" disabled={!script.canEdit} onClick={() => {
                    updateScript({content: DEFAULT_SCRIPT});
                    setParameterSchema(DEFAULT_PARAMETER_SCHEMA);
                    setParameterValues({});
                    setParameterTab('values');
                    setShowExample(false);
                    showNotice('示例已载入草稿，尚未保存或执行', 'success');
                }}>替换当前草稿为示例
                </button>
            </>}>
                <div className="help-content">
                    <p>示例声明 <code>name</code>（文本）和 <code>count</code>（1～20 的数字），下方表单会自动生成。
                        执行只生成示例表格，不访问业务数据。</p>
                    <pre className="preview-code">{DEFAULT_SCRIPT}</pre>
                    <div className="safety-note">载入会替换当前未保存的脚本与参数配置；不会自动保存，也不会自动执行。</div>
                </div>
            </Modal>}

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
