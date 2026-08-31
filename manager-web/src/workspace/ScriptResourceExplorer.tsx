import {type ReactNode, useMemo, useState} from 'react';
import DirectoryTree from '../DirectoryTree';
import {filterTree} from '../tree';
import type {DirectoryNode, ScriptResourceOverview} from '../types';

interface Props {
    serviceName: string;
    tree: DirectoryNode[];
    overview: ScriptResourceOverview;
    loading: boolean;
    selectedId?: string;
    onSelect: (scriptId: string) => void;
    onCreate?: (parent?: DirectoryNode) => void;
    onRename: (node: DirectoryNode) => void;
    onDelete: (node: DirectoryNode) => void;
}

/** 资源筛选和视图切换留在资源模块，加载与写操作由应用层编排。 */
export default function ScriptResourceExplorer({
                                                   serviceName,
                                                   tree,
                                                   overview,
                                                   loading,
                                                   selectedId,
                                                   onSelect,
                                                   onCreate,
                                                   onRename,
                                                   onDelete,
                                               }: Props) {
    const [search, setSearch] = useState('');
    const [view, setView] = useState<'all' | 'favorites' | 'recent'>('all');
    const visibleTree = useMemo(() => filterTree(tree, search), [search, tree]);
    const visibleShortcuts = useMemo(() => {
        const items = view === 'favorites' ? overview.favorites : overview.recent;
        const keyword = search.trim().toLowerCase();
        return keyword ? items.filter((item) => item.name.toLowerCase().includes(keyword)) : items;
    }, [overview, view, search]);

    let resources: ReactNode;
    if (loading) {
        resources = <p className="empty-hint">目录加载中…</p>;
    } else if (!serviceName) {
        resources = <p className="empty-hint">选择应用服务后查看脚本</p>;
    } else if (view !== 'all') {
        resources = visibleShortcuts.length ? <div className="shortcut-list">
            {visibleShortcuts.map((item) => <button type="button" key={item.id}
                                                    className={selectedId === item.id ? 'active' : ''}
                                                    onClick={() => onSelect(item.id)}>
                <span>{item.favorite ? '★' : '◷'}</span><strong>{item.name}</strong>
                <small>{item.lastOpenTime ? item.lastOpenTime.replace('T', ' ') : '已收藏'}</small>
            </button>)}
        </div> : <p className="empty-hint">暂无{view === 'favorites' ? '收藏' : '最近使用'}脚本</p>;
    } else if (visibleTree.length) {
        resources = <DirectoryTree nodes={visibleTree} selectedId={selectedId} searching={Boolean(search.trim())}
                                   onSelect={(node) => onSelect(node.id)} onCreate={onCreate} onRename={onRename}
                                   onDelete={onDelete}/>;
    } else {
        resources = <p className="empty-hint">{search ? '没有匹配结果' : '暂无脚本资源'}</p>;
    }

    return <section className="explorer">
        <div className="explorer-heading">
            <span><small>脚本资源</small><strong>{serviceName || '未选择服务'}</strong></span>
            {onCreate && <button type="button" disabled={!serviceName} onClick={() => onCreate()}>+ 新建</button>}
        </div>
        <input className="search-input" value={search} onChange={(event) => setSearch(event.target.value)}
               placeholder="搜索文件夹或脚本" aria-label="搜索目录树"/>
        <div className="resource-tabs">
            <button className={view === 'all' ? 'active' : ''} type="button" onClick={() => setView('all')}>全部
            </button>
            <button className={view === 'favorites' ? 'active' : ''} type="button"
                    onClick={() => setView('favorites')}>收藏 {overview.favorites.length}</button>
            <button className={view === 'recent' ? 'active' : ''} type="button"
                    onClick={() => setView('recent')}>最近 {overview.recent.length}</button>
        </div>
        <div className="tree-scroll">{resources}</div>
    </section>;
}
