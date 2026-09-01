import {useCallback, useEffect, useState} from 'react';
import {api} from '../api';
import Modal from '../Modal';
import type {DirectoryNode, ScriptResourceOverview} from '../types';
import ScriptResourceExplorer from './ScriptResourceExplorer';
import {TOOL_TEMPLATES} from './templates';
import {navigate} from '../navigation';

type ResourceDialog = {
    kind: 'create' | 'rename' | 'delete'; node?: DirectoryNode; name: string;
    nodeType: DirectoryNode['type']; expectedVersion?: number; forceDelete: boolean
};

/** 资源的请求、筛选及变更状态集中在资源 Module，不由编辑页面代管。 */
export default function WorkspaceResources({
                                               serviceName,
                                               scriptId,
                                               environment,
                                               revision,
                                               onScriptSelect
                                           }: {
    serviceName: string; scriptId: string; environment: string; revision: number;
    onScriptSelect?: (id: string) => void;
}) {
    const [tree, setTree] = useState<DirectoryNode[]>([]);
    const [overview, setOverview] = useState<ScriptResourceOverview>({favorites: [], recent: []});
    const [dialog, setDialog] = useState<ResourceDialog>();
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const refresh = useCallback(async () => {
        setLoading(true);
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
        <ScriptResourceExplorer serviceName={serviceName} tree={tree} overview={overview} loading={loading}
                                selectedId={scriptId}
                                onSelect={id => onScriptSelect ? onScriptSelect(id) : navigate(`/workspace/${id}`)}
                                onCreate={parent => setDialog({
                                    kind: 'create',
                                    node: parent,
                                    name: '',
                                    nodeType: 'script',
                                    forceDelete: false
                                })}
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
                                })}/>
        {error && <p className="resource-error" role="alert">{error}</p>}
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
                                   if (dialog.node!.id === scriptId) navigate('/workspace');
                               } else {
                                   const template = TOOL_TEMPLATES.table;
                                   const id = await api.saveTreeNode({
                                       nodeType: dialog.nodeType, nodeName: dialog.name.trim(), serviceName,
                                       nodeId: dialog.kind === 'rename' ? dialog.node!.id : undefined,
                                       parentId: dialog.kind === 'create' ? dialog.node?.id : undefined,
                                       expectedVersion: dialog.expectedVersion,
                                       ...(dialog.kind === 'create' && dialog.nodeType === 'script' ? {
                                           content: template.content,
                                           parameterSchema: template.schema,
                                           description: template.description,
                                           allowedEnvironments: environment ? [environment] : [],
                                           toolMetadata: {operationType: 'QUERY' as const}
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
                        {dialog.kind === 'create' && <label><span>类型</span><select value={dialog.nodeType}
                                                                                     onChange={event => setDialog({
                                                                                         ...dialog,
                                                                                         nodeType: event.target.value as DirectoryNode['type']
                                                                                     })}>
                            <option value="script">脚本</option>
                            <option value="folder">文件夹</option>
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
