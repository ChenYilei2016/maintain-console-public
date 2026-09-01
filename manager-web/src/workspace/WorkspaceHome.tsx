import {useEffect, useMemo, useState} from 'react';
import {api} from '../api';
import {navigate} from '../navigation';
import type {DirectoryNode, LoginInfo, ScriptShortcut} from '../types';
import '../tools/tools.css';

function scripts(nodes: DirectoryNode[]): DirectoryNode[] {
    return nodes.flatMap(node => node.type === 'script' ? [node] : scripts(node.children || []));
}

export default function WorkspaceHome({login}: { login: LoginInfo }) {
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
    const visible = useMemo(() => {
        const keyword = search.trim().toLowerCase();
        return scripts(tree).filter(item => !keyword || item.name.toLowerCase().includes(keyword));
    }, [tree, search]);
    if (!login.canCreateTools) return <main className="workspace-home">
        <section className="workspace-empty">
            <h1>当前账号没有开发工作台权限</h1><p>普通使用者可以在工具库中运行获得授权的工具。</p><a
            href="/">打开工具库</a>
        </section>
    </main>;
    return <main className="workspace-home">
        <header>
            <div><p className="eyebrow">开发工作台</p><h1>继续编辑脚本</h1>
                <p>选择已有资源直接进入编辑，不需要先经过工具卡片。</p></div>
            <button className="primary" onClick={() => navigate('/workspace/new')}>＋ 新建工具</button>
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
        <section className="workspace-script-list"><h2>全部脚本</h2>
            <div>{visible.map(item => <button key={item.id}
                                              onClick={() => navigate(`/workspace/${item.id}`)}>
                <span
                    className="node-icon script"/><strong>{item.name}</strong><small>{item.permissionType === 'private' ? '私有' : '已共享'}</small>
            </button>)}</div>
            {!visible.length && <p>没有匹配的脚本。</p>}</section>
    </main>;
}
