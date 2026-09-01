import {type DragEvent, useEffect, useState} from 'react';
import type {DirectoryNode} from './types';

export const DIRECTORY_NODE_DRAG_TYPE = 'application/x-maintain-console-node';

interface DirectoryTreeProps {
    nodes: DirectoryNode[];
    selectedId?: string;
    selectedFolderId?: string;
    searching: boolean;
    onSelect: (node: DirectoryNode) => void;
    onFolderSelect?: (node: DirectoryNode) => void;
    onCreate?: (parent: DirectoryNode) => void;
    onRename?: (node: DirectoryNode) => void;
    onDelete?: (node: DirectoryNode) => void;
    onMove?: (nodeId: string, parentId?: string) => void;
    onDraggingChange?: (dragging: boolean) => void;
}

interface TreeNodeProps extends Omit<DirectoryTreeProps, 'nodes'> {
    node: DirectoryNode;
}

function TreeNodeRow({
                         node,
                         selectedId,
                         selectedFolderId,
                         searching,
                         onSelect,
                         onFolderSelect,
                         onCreate,
                         onRename,
                         onDelete,
                         onMove,
                         onDraggingChange
                     }: TreeNodeProps) {
    const [expanded, setExpanded] = useState(node.level === 0);
    const [dropActive, setDropActive] = useState(false);
    const isFolder = node.type === 'folder';
    const capabilityKnown = !isFolder && [node.canRead, node.canEdit, node.canInvoke, node.canManage]
        .some(value => typeof value === 'boolean');
    const canOpen = !capabilityKnown || Boolean(node.canRead || node.canEdit || node.canInvoke || node.canManage);
    const capabilityLabel = !capabilityKnown ? '' : node.canEdit && node.canInvoke ? '可调试'
        : node.canInvoke ? '可运行' : node.canEdit ? '可编辑' : node.canRead ? '只读'
            : node.canManage ? '可授权' : '仅目录';
    const capabilityTitle = [node.canRead && '查看', node.canEdit && '编辑', node.canInvoke && '运行',
        node.canManage && '授权'].filter(Boolean).join('、') || '仅目录可见，无脚本访问能力';
    const movable = Boolean(onMove) && node.canRename !== false;
    const selectedParentId = selectedFolderId || undefined;
    const canMoveToSelected = movable && node.id !== selectedParentId
        && (node.parentId || undefined) !== selectedParentId;

    const drop = (event: DragEvent) => {
        if (!isFolder || !onMove) return;
        event.preventDefault();
        event.stopPropagation();
        setDropActive(false);
        const nodeId = event.dataTransfer.getData(DIRECTORY_NODE_DRAG_TYPE);
        if (nodeId && nodeId !== node.id) onMove(nodeId, node.id);
    };

    useEffect(() => {
        if (searching) setExpanded(true);
    }, [searching]);

    const activate = () => {
        if (isFolder) {
            setExpanded((value) => !value);
            onFolderSelect?.(node);
        }
        else onSelect(node);
    };

    return (
        <li>
            <div
                className={`tree-row ${selectedId === node.id || selectedFolderId === node.id ? 'selected' : ''} ${dropActive ? 'drop-target' : ''}`}
                onDragOver={event => {
                    if (isFolder && onMove) {
                        event.preventDefault();
                        event.stopPropagation();
                        event.dataTransfer.dropEffect = 'move';
                    }
                }} onDragEnter={event => {
                if (isFolder && onMove) {
                    event.stopPropagation();
                    setDropActive(true);
                }
            }}
                onDragLeave={event => {
                    event.stopPropagation();
                    if (!event.currentTarget.contains(event.relatedTarget as Node)) setDropActive(false);
                }} onDrop={drop}>
                <button className="tree-label" type="button" onClick={activate} disabled={!isFolder && !canOpen}
                        draggable={movable} onDragStart={event => {
                    event.dataTransfer.effectAllowed = 'move';
                    event.dataTransfer.setData(DIRECTORY_NODE_DRAG_TYPE, node.id);
                    onDraggingChange?.(true);
                }} onDragEnd={() => {
                    setDropActive(false);
                    onDraggingChange?.(false);
                }}
                        title={`${capabilityKnown ? `${node.name} · ${capabilityTitle}` : node.name}${movable ? ' · 可拖拽移动' : ''}`}>
          <span className={`tree-arrow ${isFolder && expanded ? 'expanded' : ''}`} aria-hidden="true">
            {isFolder ? '›' : ''}
          </span>
                    <span className={`node-icon ${node.type}`} aria-hidden="true"/>
                    <span className="tree-name">{node.name}</span>
                    {capabilityLabel && <span className={`tree-capability ${canOpen ? '' : 'locked'}`}>
                        {capabilityLabel}</span>}
                </button>
                {(onCreate || onRename || onDelete || onMove) && <span className="tree-actions">
          {onCreate && isFolder && node.canCreateChild !== false && (
              <button type="button" aria-label={`在 ${node.name} 下新建`} onClick={() => onCreate(node)}>+</button>
          )}
                    {canMoveToSelected && <button type="button"
                                                  aria-label={`将 ${node.name} 移动到${selectedFolderId ? '当前目录' : '根目录'}`}
                                                  onClick={() => onMove?.(node.id, selectedParentId)}>⇢</button>}
                    {onRename && node.canRename !== false && <button type="button" aria-label={`重命名 ${node.name}`}
                                         onClick={() => onRename(node)}>✎</button>}
                    {onDelete && node.canDelete !== false &&
                        <button className="danger" type="button" aria-label={`删除 ${node.name}`}
                                         onClick={() => onDelete(node)}>×</button>}
                </span>}
            </div>
            {isFolder && expanded && node.children && node.children.length > 0 && (
                <ul className="tree-children">
                    {node.children.map((child) => (
                        <TreeNodeRow
                            key={child.id}
                            node={child}
                            selectedId={selectedId}
                            selectedFolderId={selectedFolderId}
                            searching={searching}
                            onSelect={onSelect}
                            onFolderSelect={onFolderSelect}
                            onCreate={onCreate}
                            onRename={onRename}
                            onDelete={onDelete}
                            onMove={onMove}
                            onDraggingChange={onDraggingChange}
                        />
                    ))}
                </ul>
            )}
        </li>
    );
}

export default function DirectoryTree({nodes, ...props}: DirectoryTreeProps) {
    return (
        <ul className="directory-tree">
            {nodes.map((node) => <TreeNodeRow key={node.id} node={node} {...props} />)}
        </ul>
    );
}
