import type {DirectoryNode} from './types';

export function filterTree(nodes: DirectoryNode[], query: string): DirectoryNode[] {
    const keyword = query.trim().toLocaleLowerCase();
    if (!keyword) return nodes;

    return nodes.flatMap((node) => {
        const children = filterTree(node.children || [], keyword);
        return node.name.toLocaleLowerCase().includes(keyword) || children.length
            ? [{...node, ...(node.children ? {children} : {})}]
            : [];
    });
}

export function extractParameters(script: string): string[] {
    return [...script.matchAll(/\$\$\{([^}]+)}/g)]
        .map((match) => match[1].trim())
        .filter((name, index, names) => name && names.indexOf(name) === index);
}
