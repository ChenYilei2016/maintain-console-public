import {useEffect, useRef, useState} from 'react';
import type {DirectoryNode} from './types';

const POINTER_DRAG_THRESHOLD_PX = 6;

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
}

interface TreeNodeProps extends Omit<DirectoryTreeProps, 'nodes'> {
    node: DirectoryNode;
    pointerDropTarget?: string;
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
                         pointerDropTarget,
                     }: TreeNodeProps) {
    const [expanded, setExpanded] = useState(node.level === 0);
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
                data-folder-id={isFolder ? node.id : undefined}
                className={`tree-row ${selectedId === node.id || selectedFolderId === node.id ? 'selected' : ''} ${pointerDropTarget === node.id ? 'drop-target' : ''}`}>
                <button className="tree-label" type="button" onClick={activate} disabled={!isFolder && !canOpen}
                        title={capabilityKnown ? `${node.name} · ${capabilityTitle}` : node.name}>
          <span className={`tree-arrow ${isFolder && expanded ? 'expanded' : ''}`} aria-hidden="true">
            {isFolder ? '›' : ''}
          </span>
                    <span className={`node-icon ${node.type}`} aria-hidden="true"/>
                    <span className="tree-name">{node.name}</span>
                    {capabilityLabel && <span className={`tree-capability ${canOpen ? '' : 'locked'}`}>
                        {capabilityLabel}</span>}
                </button>
                {movable && <span className="tree-drag-handle" role="button" tabIndex={0}
                                  data-drag-node-id={node.id}
                                  aria-label={`拖动 ${node.name}${canMoveToSelected
                                      ? `，或点击移动到${selectedFolderId ? '当前目录' : '根目录'}` : ''}`}
                                  title={canMoveToSelected
                                      ? `拖到目标目录，或点击移动到${selectedFolderId ? '当前目录' : '根目录'}`
                                      : '拖到目标目录'}
                                  onClick={() => {
                                      if (canMoveToSelected) onMove?.(node.id, selectedParentId);
                                  }} onKeyDown={event => {
                    if (canMoveToSelected && (event.key === 'Enter' || event.key === ' ')) {
                        event.preventDefault();
                        onMove?.(node.id, selectedParentId);
                    }
                }}>⠿</span>}
                {(onCreate || onRename || onDelete) && <span className="tree-actions">
          {onCreate && isFolder && node.canCreateChild !== false && (
              <button type="button" aria-label={`在 ${node.name} 下新建`} onClick={() => onCreate(node)}>+</button>
          )}
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
                            pointerDropTarget={pointerDropTarget}
                        />
                    ))}
                </ul>
            )}
        </li>
    );
}

export default function DirectoryTree({nodes, onMove, ...props}: DirectoryTreeProps) {
    const pointerDrag = useRef<{
        nodeId: string;
        pointerId: number;
        startX: number;
        startY: number;
        moved: boolean;
    } | undefined>(undefined);
    const suppressClick = useRef(false);
    const [pointerDragging, setPointerDragging] = useState(false);
    const [pointerDropTarget, setPointerDropTarget] = useState<string>();

    const dropTargetAt = (clientX: number, clientY: number) =>
        document.elementFromPoint(clientX, clientY)?.closest<HTMLElement>('[data-folder-id], [data-tree-root-drop]');

    const finishDrag = (clientX: number, clientY: number) => {
        const dragging = pointerDrag.current;
        pointerDrag.current = undefined;
        setPointerDragging(false);
        setPointerDropTarget(undefined);
        if (!dragging?.moved) return;
        suppressClick.current = true;
        window.setTimeout(() => {
            suppressClick.current = false;
        }, 0);
        const target = dropTargetAt(clientX, clientY);
        if (target?.hasAttribute('data-tree-root-drop')) onMove?.(dragging.nodeId);
        else {
            const parentId = target?.dataset.folderId;
            if (parentId && parentId !== dragging.nodeId) onMove?.(dragging.nodeId, parentId);
        }
    };

    return (
        <ul className="directory-tree" onClickCapture={event => {
            if (suppressClick.current && (event.target as Element).closest('[data-drag-node-id]')) {
                suppressClick.current = false;
                event.preventDefault();
                event.stopPropagation();
            }
        }} onPointerDown={event => {
            const handle = (event.target as Element).closest<HTMLElement>('[data-drag-node-id]');
            const nodeId = handle?.dataset.dragNodeId;
            if (!onMove || !handle || !nodeId || event.button !== 0) return;
            pointerDrag.current = {
                nodeId,
                pointerId: event.pointerId,
                startX: event.clientX,
                startY: event.clientY,
                moved: false,
            };
            handle.setPointerCapture?.(event.pointerId);
        }} onPointerMove={event => {
            const dragging = pointerDrag.current;
            if (!dragging || dragging.pointerId !== event.pointerId) return;
            if (!dragging.moved && Math.hypot(event.clientX - dragging.startX, event.clientY - dragging.startY)
                >= POINTER_DRAG_THRESHOLD_PX) {
                dragging.moved = true;
                setPointerDragging(true);
            }
            if (!dragging.moved) return;
            event.preventDefault();
            const targetId = dropTargetAt(event.clientX, event.clientY)?.dataset.folderId;
            setPointerDropTarget(targetId === dragging.nodeId ? undefined : targetId);
        }} onPointerUp={event => finishDrag(event.clientX, event.clientY)} onPointerCancel={() => {
            pointerDrag.current = undefined;
            setPointerDragging(false);
            setPointerDropTarget(undefined);
        }}>
            {nodes.map((node) => <TreeNodeRow key={node.id} node={node} onMove={onMove}
                                              pointerDropTarget={pointerDropTarget} {...props} />)}
            {pointerDragging && <li className="tree-root-drop" data-tree-root-drop>拖到这里移至根目录</li>}
        </ul>
    );
}
