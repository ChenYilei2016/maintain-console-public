import {useEffect, useState} from 'react';
import type {DirectoryNode} from './types';

interface DirectoryTreeProps {
    nodes: DirectoryNode[];
    selectedId?: string;
    searching: boolean;
    onSelect: (node: DirectoryNode) => void;
    onCreate?: (parent: DirectoryNode) => void;
    onRename?: (node: DirectoryNode) => void;
    onDelete?: (node: DirectoryNode) => void;
}

interface TreeNodeProps extends Omit<DirectoryTreeProps, 'nodes'> {
    node: DirectoryNode;
}

function TreeNodeRow({node, selectedId, searching, onSelect, onCreate, onRename, onDelete}: TreeNodeProps) {
    const [expanded, setExpanded] = useState(node.level === 0);
    const isFolder = node.type === 'folder';

    useEffect(() => {
        if (searching) setExpanded(true);
    }, [searching]);

    const activate = () => {
        if (isFolder) setExpanded((value) => !value);
        else onSelect(node);
    };

    return (
        <li>
            <div className={`tree-row ${selectedId === node.id ? 'selected' : ''}`}>
                <button className="tree-label" type="button" onClick={activate} title={node.name}>
          <span className={`tree-arrow ${isFolder && expanded ? 'expanded' : ''}`} aria-hidden="true">
            {isFolder ? '›' : ''}
          </span>
                    <span className={`node-icon ${node.type}`} aria-hidden="true"/>
                    <span className="tree-name">{node.name}</span>
                </button>
                {(onCreate || onRename || onDelete) && <span className="tree-actions">
          {onCreate && isFolder && (node.level ?? 0) <= 1 && (
              <button type="button" aria-label={`在 ${node.name} 下新建`} onClick={() => onCreate(node)}>+</button>
          )}
                    {onRename && <button type="button" aria-label={`重命名 ${node.name}`}
                                         onClick={() => onRename(node)}>✎</button>}
                    {onDelete && <button className="danger" type="button" aria-label={`删除 ${node.name}`}
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
                            searching={searching}
                            onSelect={onSelect}
                            onCreate={onCreate}
                            onRename={onRename}
                            onDelete={onDelete}
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
