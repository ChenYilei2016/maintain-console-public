import {useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import DirectoryTree from '../DirectoryTree';
import {navigate} from '../navigation';
import {filterTree} from '../tree';
import type {DirectoryNode, ScriptShortcut} from '../types';
import '../tools/tools.css';
import './workspace.css';

export default function WorkspaceHome() {
    const [services, setServices] = useState<string[]>([]);
    const [service, setService] = useState('');
    const [tree, setTree] = useState<DirectoryNode[]>([]);
    const [recent, setRecent] = useState<ScriptShortcut[]>([]);
    const [search, setSearch] = useState('');
    const [error, setError] = useState('');
    useEffect(() => {
        api.listServices().then(items => {
            setServices(items);
            setService(current => current || items[0] || '');
        }).catch(failure => setError(failure.message));
    }, []);
    useEffect(() => {
        if (!service) return;
        Promise.all([api.getDirectoryTree(service), api.getResourceOverview(service)]).then(([nodes, overview]) => {
            setTree(nodes);
            setRecent(overview.recent);
        }).catch(failure => setError(failure.message));
    }, [service]);
    const visibleTree = useMemo(() => filterTree(tree, search), [tree, search]);
    return <main className="workspace-home">
        <header>
            <div><p className="eyebrow">脚本工作台</p><h1>继续处理脚本</h1>
                <p>目录对所有登录用户可见；打开后按脚本 JSON 展示查看、运行或编辑能力。</p></div>
            <button className="primary" onClick={() => navigate('/workspace/new')}>＋ 新建脚本</button>
        </header>
        <section className="workspace-home-controls"><label><span>应用服务</span><select value={service}
                                                                                         onChange={event => setService(event.target.value)}>
            {services.map(item => <option key={item}>{item}</option>)}</select></label>
            <input aria-label="搜索脚本" value={search} onChange={event => setSearch(event.target.value)}
                   placeholder="搜索当前应用中的脚本"/></section>
        {error && <p role="alert" className="safety-note">{error}</p>}
        <section className="workspace-recent"><h2>最近编辑</h2>
            <div>{recent.slice(0, 6).map(item => <button key={item.id}
                                                         onClick={() => navigate(`/workspace/${item.id}`)}>
                <strong>{item.name}</strong><small>{item.lastOpenTime?.replace('T', ' ') || item.serviceName}</small>
            </button>)}</div>
            {!recent.length && <p>暂无最近编辑记录，可以从下面选择脚本。</p>}</section>
        <section className="workspace-script-list"><h2>完整目录</h2>
            {visibleTree.length ? <div className="workspace-directory-card">
                <DirectoryTree nodes={visibleTree} searching={Boolean(search.trim())}
                               onSelect={item => navigate(`/workspace/${item.id}`)}/>
            </div> : <p>没有匹配的目录或脚本。</p>}</section>
    </main>;
}
