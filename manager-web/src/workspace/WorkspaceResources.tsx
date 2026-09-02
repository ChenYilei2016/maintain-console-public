import {useCallback, useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import Modal from '../Modal';
import type {DirectoryNode, EnvironmentOption, ScriptResourceOverview} from '../types';
import {OPERATION_TYPES} from '../types';
import ScriptResourceExplorer from './ScriptResourceExplorer';
import ScriptImportDialog from './ScriptImportDialog';
import {TOOL_TEMPLATES} from './templates';
import {navigate} from '../navigation';

type ResourceDialog = {
    kind: 'create' | 'rename' | 'delete'; node?: DirectoryNode; name: string;
    nodeType: DirectoryNode['type']; expectedVersion?: number; forceDelete: boolean
};

export function WorkspaceServiceSelector({value, services, onChange}: {
    value: string;
    services: string[];
    onChange: (serviceName: string) => void;
}) {
    const options = value && !services.includes(value) ? [value, ...services] : services;
    return <label className="workspace-service-selector"><span>应用服务</span>
        <select aria-label="脚本服务" value={value} onChange={event => onChange(event.target.value)}>
            {!options.length && <option value="">加载服务…</option>}
            {options.map(item => <option key={item}>{item}</option>)}
        </select>
    </label>;
}

/** 资源的请求、筛选及变更状态集中在资源 Module，不由编辑页面代管。 */
export default function WorkspaceResources({
                                               serviceName,
                                               scriptId,
                                               environment,
                                               environments,
                                               revision,
                                               onScriptSelect,
                                               onServiceChange,
                                               onServicesChange,
                                           }: {
    serviceName: string; scriptId?: string; environment: string; environments: EnvironmentOption[]; revision: number;
    onScriptSelect?: (id: string) => void;
    onServiceChange: (serviceName: string) => void;
    onServicesChange: (services: string[]) => void;
}) {
    const [services, setServices] = useState<string[]>(serviceName ? [serviceName] : []);
    const [tree, setTree] = useState<DirectoryNode[]>([]);
    const [overview, setOverview] = useState<ScriptResourceOverview>({favorites: [], recent: []});
    const [selectedFolderId, setSelectedFolderId] = useState('');
    const [dialog, setDialog] = useState<ResourceDialog>();
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [importOpen, setImportOpen] = useState(false);
    useEffect(() => {
        api.listServices().then(items => {
            setServices(items);
            onServicesChange(items);
            if (!serviceName) onServiceChange(items[0] || '');
        }).catch(failure => setError(failure.message));
    }, []);
    useEffect(() => {
        setSelectedFolderId('');
        setError('');
    }, [serviceName]);
    const selectedFolder = useMemo(() => {
        const find = (nodes: DirectoryNode[]): DirectoryNode | undefined => {
            for (const node of nodes) {
                if (node.id === selectedFolderId && node.type === 'folder') return node;
                const child = find(node.children || []);
                if (child) return child;
            }
        };
        return selectedFolderId ? find(tree) : undefined;
    }, [selectedFolderId, tree]);
    const refresh = useCallback(async () => {
        if (!serviceName) return;
        setLoading(true);
        setError('');
        try {
            const [nodes, shortcuts] = await Promise.all([api.getDirectoryTree(serviceName), api.getResourceOverview(serviceName)]);
            setTree(nodes);
            setOverview(shortcuts);
        } catch (failure) {
            setError(failure instanceof Error ? failure.message : '资源加载失败');
        } finally {
            setLoading(false);
        }
    }, [serviceName]);
    useEffect(() => {
        void refresh();
    }, [refresh, revision]);
    return <>
        <ScriptResourceExplorer serviceName={serviceName} tree={tree} overview={overview}
                                loading={loading} selectedId={scriptId} selectedFolder={selectedFolder}
                                onFolderSelect={folder => setSelectedFolderId(folder?.id || '')}
                                onSelect={id => onScriptSelect ? onScriptSelect(id) : navigate(`/workspace/${id}`)}
                                onCreate={parent => setDialog({
                                    kind: 'create',
                                    node: parent,
                                    name: '',
                                    nodeType: 'script',
                                    forceDelete: false
                                })}
                                onImport={() => setImportOpen(true)}
                                onRename={async node => {
                                    try {
                                        const detail = node.type === 'script' ? await api.getScriptDetail(node.id) : undefined;
                                        setDialog({
                                            kind: 'rename',
                                            node,
                                            name: node.name,
                                            nodeType: node.type,
                                            expectedVersion: detail?.version,
                                            forceDelete: false
                                        });
                                    } catch (failure) {
                                        setError(failure instanceof Error ? failure.message : '无法重命名');
                                    }
                                }}
                                onDelete={node => setDialog({
                                    kind: 'delete',
                                    node,
                                    name: node.name,
                                    nodeType: node.type,
                                    forceDelete: false
                                })} onMove={async (nodeId, parentId) => {
            if (saving) return;
            setSaving(true);
            setError('');
            try {
                await api.moveTreeNode(nodeId, parentId);
                await refresh();
            } catch (failure) {
                setError(failure instanceof Error ? failure.message : '移动失败');
            } finally {
                setSaving(false);
            }
        }}/>
        {error && <p className="resource-error" role="alert">{error}</p>}
        {importOpen && <ScriptImportDialog defaultServiceName={serviceName}
                                           defaultParentId={selectedFolder?.id}
                                           defaultEnvironment={environment}
                                           initialServices={services}
                                           environments={environments}
                                           onClose={() => setImportOpen(false)}/>}
        {dialog &&
            <Modal title={dialog.kind === 'create' ? '新建资源' : dialog.kind === 'rename' ? '重命名资源' : '删除资源'}
                   onClose={() => setDialog(undefined)}
                   footer={<>
                       <button onClick={() => setDialog(undefined)}>取消</button>
                       <button className={dialog.kind === 'delete' ? 'danger-button' : 'primary'}
                               disabled={saving || !dialog.name.trim()} onClick={async () => {
                           setSaving(true);
                           setError('');
                           try {
                               if (dialog.kind === 'delete') {
                                   await api.deleteTreeNode(dialog.node!.id, dialog.forceDelete);
                                   if (dialog.node!.id === selectedFolderId) setSelectedFolderId('');
                                   if (dialog.node!.id === scriptId) navigate('/workspace');
                               } else {
                                   const template = TOOL_TEMPLATES.table;
                                   const id = await api.saveTreeNode({
                                       nodeType: dialog.nodeType,
                                       nodeName: dialog.name.trim(),
                                       serviceName,
                                       nodeId: dialog.kind === 'rename' ? dialog.node!.id : undefined,
                                       parentId: dialog.kind === 'create' ? dialog.node?.id : undefined,
                                       expectedVersion: dialog.expectedVersion,
                                       ...(dialog.kind === 'create' && dialog.nodeType === 'script' ? {
                                           content: template.content,
                                           parameterSchema: template.schema,
                                           description: template.description,
                                           allowedEnvironments: environment ? [environment] : [],
                                           toolMetadata: {operationType: OPERATION_TYPES.QUERY}
                                       } : {}),
                                   });
                                   if (dialog.nodeType === 'script') navigate(`/workspace/${id}`);
                               }
                               setDialog(undefined);
                               await refresh();
                           } catch (failure) {
                               setError(failure instanceof Error ? failure.message : '操作失败');
                           } finally {
                               setSaving(false);
                           }
                       }}>{saving ? '处理中…' : '确认'}</button>
                   </>}>
                {dialog.kind === 'delete' ? <><p>确认删除「{dialog.name}」？</p>{dialog.nodeType === 'folder' && <label>
                        <input type="checkbox" checked={dialog.forceDelete}
                               onChange={event => setDialog({...dialog, forceDelete: event.target.checked})}/>包含目录内的资源（仍逐项校验管理权限）</label>}</> :
                    <div className="form-stack">
                        {dialog.kind === 'create' && <p className="resource-create-location">创建位置：{serviceName}
                            {dialog.node ? ` / ${dialog.node.name}` : ' / 根目录'}</p>}
                        {dialog.kind === 'create' && <label><span>类型</span><select value={dialog.nodeType}
                                                                                     onChange={event => setDialog({
                                                                                         ...dialog,
                                                                                         nodeType: event.target.value as DirectoryNode['type']
                                                                                     })}>
                            <option value="script">脚本</option>
                            <option value="folder" disabled={Boolean(dialog.node?.parentId)}>文件夹
                                {dialog.node?.parentId ? '（已达目录层级上限）' : ''}</option>
                        </select></label>}
                        <label><span>名称</span><input autoFocus value={dialog.name} onChange={event => setDialog({
                            ...dialog,
                            name: event.target.value
                        })}/></label>
                    </div>}
                {error && <p role="alert" className="safety-note">{error}</p>}
            </Modal>}
    </>;
}
