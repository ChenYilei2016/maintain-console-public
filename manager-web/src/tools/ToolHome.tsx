import {useEffect, useState} from 'react';
import {api} from '../api';
import type {LoginInfo} from '../types';
import type {CatalogView, ToolPage} from './toolApi';
import {CATALOG_VIEWS, OPERATION_LABELS, toolApi} from './toolApi';
import './tools.css';

export default function ToolHome({login}: { login: LoginInfo }) {
    const [view, setView] = useState<CatalogView>('ALL');
    const [search, setSearch] = useState('');
    const [query, setQuery] = useState('');
    const [service, setService] = useState('');
    const [services, setServices] = useState<string[]>([]);
    const [cursor, setCursor] = useState(0);
    const [previous, setPrevious] = useState<number[]>([]);
    const [page, setPage] = useState<ToolPage>();
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        api.listServices().then(setServices).catch(() => setServices([]));
    }, []);
    useEffect(() => {
        let active = true;
        setLoading(true);
        setError('');
        toolApi.list(view, query, service, cursor).then(result => {
            if (active) setPage(result);
        })
            .catch(failure => {
                if (active) setError(failure.message);
            })
            .finally(() => {
                if (active) setLoading(false);
            });
        return () => {
            active = false;
        };
    }, [view, query, service, cursor]);
    const resetPage = () => {
        setCursor(0);
        setPrevious([]);
    };
    return <main className="tool-home">
        <header className="tool-app-header"><a href="/">Maintain
            Console</a><span>{login.employeeName} · {login.employeeNo}</span></header>
        <section className="catalog-intro">
            <div><p className="eyebrow">应用脚本工具台</p><h1>找到工具，填表就能用</h1>
                <p>查数、诊断与经授权的手动操作。每次运行使用你自己的身份，并留下执行记录。</p></div>
            {login.canCreateTools && <a className="button primary" href="/workspace">＋ 制作新工具</a>}</section>
        <section className="catalog-panel">
            <nav className="catalog-tabs" aria-label="工具分类">{Object.entries(CATALOG_VIEWS).map(([key, label]) =>
                <button type="button" key={key} aria-pressed={view === key} onClick={() => {
                    setView(key as CatalogView);
                    resetPage();
                }}>{label}</button>)}</nav>
            <form className="catalog-search" onSubmit={event => {
                event.preventDefault();
                setQuery(search);
                resetPage();
            }}>
                <input aria-label="搜索工具" maxLength={200} value={search}
                       onChange={event => setSearch(event.target.value)} placeholder="按名称或用途搜索，例如：订单查询"/>
                <select aria-label="所属服务" value={service} onChange={event => {
                    setService(event.target.value);
                    resetPage();
                }}>
                    <option value="">全部服务</option>
                    {services.map(name => <option key={name}>{name}</option>)}
                </select>
                <button type="submit">搜索</button>
            </form>
            {error && <p role="alert" className="safety-note">{error}</p>}
            {loading ? <p className="inline-empty" role="status">正在查找可用工具…</p> : <>
                {!page?.items.length && <div className="catalog-empty"><h2>这里还没有可展示的工具</h2>
                    <p>可以调整搜索条件，或请作者为你的员工 ID
                        授权。{page?.nextCursor != null && '后面还有候选工具，可继续查找。'}</p></div>}
                <div className="tool-grid">{page?.items.map(tool => <article key={tool.id} className="tool-card">
                    <header><span
                        className={'operation-badge ' + tool.metadata.operationType.toLowerCase()}>{OPERATION_LABELS[tool.metadata.operationType]}</span>
                        <button type="button" aria-label={(tool.favorite ? '取消收藏 ' : '收藏 ') + tool.name}
                                onClick={async () => {
                                    try {
                                        await api.setFavorite(tool.id, !tool.favorite);
                                        setPage(current => current ? {...current,
                                            items: current.items.map(item => item.id === tool.id ? {
                                                ...item,
                                                favorite: !item.favorite
                                            } : item)
                                        } : current);
                                    } catch (failure) {
                                        setError(failure instanceof Error ? failure.message : '收藏失败');
                                    }
                                }}>{tool.favorite ? '★' : '☆'}</button>
                    </header>
                    <h2>{tool.name}</h2><p>{tool.description || '作者尚未填写用途说明'}</p>
                    <div className="tool-card-meta">
                        <span>{tool.serviceName}</span><span>负责人：{tool.owner || tool.ownerId}</span></div>
                    <footer>{tool.canInvoke &&
                        <a className="button primary" href={`/tools/${tool.id}`}>打开运行页 →</a>}
                        {tool.canRead && <a className="button"
                                            href={`/workspace/${tool.id}`}>{tool.canEdit ? '编辑工具' : '查看代码'}</a>}
                        <small>v{tool.version}</small></footer>
                </article>)}</div>
                <footer className="catalog-pagination">
                    <button type="button" disabled={!previous.length} onClick={() => {
                        setCursor(previous[previous.length - 1]);
                        setPrevious(current => current.slice(0, -1));
                    }}>上一页
                    </button>
                    <span>仅展示当前身份有权访问的工具</span>
                    <button type="button" disabled={page?.nextCursor == null} onClick={() => {
                        setPrevious(current => [...current, cursor]);
                        setCursor(page!.nextCursor!);
                    }}>下一页
                    </button>
                </footer>
            </>}
        </section>
    </main>;
}
