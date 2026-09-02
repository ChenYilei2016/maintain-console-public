import {useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import Modal from '../Modal';
import {PARAMETER_TYPES} from '../parameters';
import {navigate} from '../navigation';
import {OPERATION_LABELS} from '../tools/toolApi';
import {OPERATION_TYPES} from '../types';
import type {DirectoryNode, EnvironmentOption} from '../types';
import {
    MAX_SCRIPT_IMPORT_SIZE,
    parseScriptImport,
    SCRIPT_IMPORT_LIMITS,
    toTreeNodeSaveRequest
} from './scriptImport';

interface Props {
    defaultServiceName: string;
    defaultParentId?: string;
    defaultEnvironment?: string;
    initialServices?: string[];
    environments: EnvironmentOption[];
    onClose: () => void;
    onCreated?: (id: string) => void;
}

interface FolderOption {
    id: string;
    label: string;
}

function folderOptions(nodes: DirectoryNode[], prefix = ''): FolderOption[] {
    return nodes.flatMap(node => node.type === 'folder'
        ? [{id: node.id, label: prefix + node.name}, ...folderOptions(node.children || [], prefix + '　')]
        : []);
}

export default function ScriptImportDialog({
                                               defaultServiceName,
                                               defaultParentId,
                                               defaultEnvironment,
                                               initialServices = [],
                                               environments,
                                               onClose,
                                               onCreated = id => navigate(`/workspace/${id}`),
                                           }: Props) {
    const [raw, setRaw] = useState('');
    const [name, setName] = useState('');
    const [services, setServices] = useState(initialServices);
    const [serviceName, setServiceName] = useState(defaultServiceName || initialServices[0] || '');
    const [parentId, setParentId] = useState(defaultParentId || '');
    const [folders, setFolders] = useState<FolderOption[]>([]);
    const [allowedEnvironments, setAllowedEnvironments] = useState<string[]>(() => {
        const selected = environments.some(item => item.value === defaultEnvironment)
            ? defaultEnvironment : environments[0]?.value;
        return selected ? [selected] : [];
    });
    const [loadingFolders, setLoadingFolders] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const parsed = useMemo(() => {
        if (!raw.trim()) return {document: undefined, error: ''};
        try {
            return {document: parseScriptImport(raw), error: ''};
        } catch (failure) {
            return {document: undefined, error: failure instanceof Error ? failure.message : '导入文档无效'};
        }
    }, [raw]);

    useEffect(() => {
        if (initialServices.length) return;
        api.listServices().then(items => {
            setServices(items);
            setServiceName(current => current || items[0] || '');
        }).catch(failure => setError(failure instanceof Error ? failure.message : '应用服务加载失败'));
    }, [initialServices.length]);
    useEffect(() => {
        if (parsed.document) setName(parsed.document.script.name);
    }, [parsed.document]);
    useEffect(() => {
        if (!serviceName) {
            setFolders([]);
            return;
        }
        let active = true;
        setLoadingFolders(true);
        api.getDirectoryTree(serviceName).then(nodes => {
            if (active) setFolders(folderOptions(nodes));
        }).catch(failure => {
            if (active) setError(failure instanceof Error ? failure.message : '目录加载失败');
        }).finally(() => {
            if (active) setLoadingFolders(false);
        });
        return () => {
            active = false;
        };
    }, [serviceName]);

    const document = parsed.document;
    const parameters = document?.script.parameterSchema.parameters || [];
    const requiredCount = parameters.filter(item => item.required).length;
    const sensitiveCount = parameters.filter(item => item.sensitive).length;
    const canCreate = Boolean(document && name.trim() && serviceName && allowedEnvironments.length && !saving);

    return <Modal title="导入工具 JSON" wide onClose={onClose} footer={<>
        <span className="import-safety-summary">创建为你的私有工具，不会执行工具，也不会导入任何授权。</span>
        <button type="button" onClick={onClose}>取消</button>
        <button className="primary" type="button" disabled={!canCreate} onClick={async () => {
            if (!document || !canCreate) return;
            setSaving(true);
            setError('');
            try {
                const id = await api.saveTreeNode(toTreeNodeSaveRequest(document, {
                    name,
                    serviceName,
                    parentId: parentId || undefined,
                    allowedEnvironments,
                }));
                onCreated(id);
            } catch (failure) {
                setError(failure instanceof Error ? failure.message : '工具创建失败');
            } finally {
                setSaving(false);
            }
        }}>{saving ? '创建中…' : '创建为私有工具并打开'}</button>
    </>}>
        <div className={`script-import-dialog ${document ? 'has-preview' : ''}`}>
            <section className="script-import-source">
                <label><span>粘贴工具导入 JSON</span><textarea rows={10} value={raw}
                                                                  placeholder="粘贴 maintain-console.script-import v1 文档"
                                                                  onChange={event => {
                                                                      setRaw(event.target.value);
                                                                      setError('');
                                                                  }}/></label>
                <label className="script-import-file"><span>或选择本地 JSON 文件</span>
                    <input type="file" accept=".json,application/json" onChange={async event => {
                        const file = event.target.files?.[0];
                        if (!file) return;
                        setRaw('');
                        setError('');
                        if (file.size > MAX_SCRIPT_IMPORT_SIZE) {
                            setError(`工具导入文件不能超过 ${MAX_SCRIPT_IMPORT_SIZE} 字节`);
                            return;
                        }
                        try {
                            setRaw(await file.text());
                        } catch {
                            setError('无法读取所选文件');
                        }
                    }}/></label>
                {!raw.trim() && <p className="empty-hint">导入文档只包含可移植工具内容；应用、目录、环境和授权由当前工作台决定。</p>}
                {parsed.error && <p className="safety-note" role="alert">{parsed.error}</p>}
            </section>

            {document && <section className="script-import-preview" aria-label="工具导入预览">
                <header><div><small>已识别工具</small><h3>{document.script.name}</h3></div>
                    <span className={`operation-badge ${document.script.toolMetadata.operationType.toLowerCase()}`}>
                        {OPERATION_LABELS[document.script.toolMetadata.operationType]}
                    </span></header>
                <p>{document.script.description}</p>
                {document.script.toolMetadata.operationType === OPERATION_TYPES.QUERY
                    ? <p className="import-query-note">查询类是用途声明，不代表脚本天然只读。</p>
                    : document.script.toolMetadata.riskNote &&
                    <p className="safety-note">风险与影响：{document.script.toolMetadata.riskNote}</p>}

                <div className="script-import-targets">
                    <label><span>工具名称</span><input maxLength={SCRIPT_IMPORT_LIMITS.name} value={name}
                                                       onChange={event => setName(event.target.value)}/></label>
                    <label><span>所属应用</span><select value={serviceName} onChange={event => {
                        const next = event.target.value;
                        setServiceName(next);
                        setParentId(next === defaultServiceName ? defaultParentId || '' : '');
                    }}>{services.map(item => <option key={item}>{item}</option>)}</select></label>
                    <label><span>目标目录</span><select value={parentId} disabled={loadingFolders}
                                                       onChange={event => setParentId(event.target.value)}>
                        <option value="">根目录</option>
                        {folders.map(folder => <option value={folder.id} key={folder.id}>{folder.label}</option>)}
                    </select></label>
                </div>

                <fieldset className="script-import-environments"><legend>允许环境</legend>
                    {environments.map(item => <label key={item.value}><input type="checkbox"
                                                                            checked={allowedEnvironments.includes(item.value)}
                                                                            onChange={event => setAllowedEnvironments(current => event.target.checked
                                                                                ? [...current, item.value]
                                                                                : current.filter(value => value !== item.value))}/>
                        <span>{item.name}{item.production ? ' · 生产' : ''}</span></label>)}
                </fieldset>

                <section className="script-import-parameters"><header><h4>参数配置</h4>
                    <span>{parameters.length} 个参数 · {requiredCount} 个必填 · {sensitiveCount} 个敏感</span></header>
                    {parameters.length ? <ul>{parameters.map(parameter => <li key={parameter.name}>
                        <strong>{parameter.label || parameter.name}</strong>
                        <code>{parameter.name}</code><span>{PARAMETER_TYPES[parameter.type]}</span>
                        {parameter.required && <b>必填</b>}{parameter.sensitive && <b>敏感</b>}
                        {parameter.description && <small>{parameter.description}</small>}
                    </li>)}</ul> : <p>此工具无需运行参数。</p>}
                </section>
                {document.script.toolMetadata.usageExample && <section className="script-import-usage"><h4>如何使用</h4>
                    <p>{document.script.toolMetadata.usageExample}</p></section>}
                <details><summary>审查 Groovy 源码与参数 Schema</summary>
                    <pre className="preview-code">{document.script.content}</pre>
                    <pre className="preview-code">{JSON.stringify(document.script.parameterSchema, null, 2)}</pre>
                </details>
            </section>}
            {error && <p className="safety-note" role="alert">{error}</p>}
        </div>
    </Modal>;
}
