import {lazy, Suspense, useEffect, useState} from 'react';
import {api} from './api';
import LoginPage from './identity/LoginPage';
import SessionMenu from './identity/SessionMenu';
import {navigate, subscribeNavigation} from './navigation';
import type {AuthState, LoginInfo} from './types';

const ToolHome = lazy(() => import('./tools/ToolHome'));
const ToolRunPage = lazy(() => import('./tools/ToolRunPage'));
const WorkspaceTabs = lazy(() => import('./workspace/WorkspaceTabs'));
const CreateToolPage = lazy(() => import('./workspace/CreateToolPage'));
const AdminPage = lazy(() => import('./admin/AdminPage'));
const WorkspaceHome = lazy(() => import('./workspace/WorkspaceHome'));

function currentReturnTo() {
    const value = window.location.pathname + window.location.search + window.location.hash;
    return value.startsWith('/') && !value.startsWith('//') && !value.startsWith('/login') ? value : '/workspace';
}

export default function App() {
    const [login, setLogin] = useState<LoginInfo>();
    const [auth, setAuth] = useState<AuthState>();
    const [error, setError] = useState('');
    const [route, setRoute] = useState(() => window.location.pathname);
    useEffect(() => {
        let active = true;
        api.getAuthState().then(async state => {
            if (!active) return;
            setAuth(state);
            if (state.authenticated) setLogin(await api.getLoginInfo());
        }).catch(failure => {
            if (active) setError(failure.message);
        });
        return () => {
            active = false;
        };
    }, []);
    useEffect(() => subscribeNavigation(() => setRoute(window.location.pathname)), []);
    useEffect(() => {
        const followInternalLink = (event: MouseEvent) => {
            if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
            const anchor = event.target instanceof Element ? event.target.closest('a') : null;
            if (!(anchor instanceof HTMLAnchorElement) || anchor.origin !== window.location.origin || anchor.target || anchor.download) return;
            event.preventDefault();
            navigate(anchor.pathname + anchor.search + anchor.hash);
        };
        document.addEventListener('click', followInternalLink);
        return () => document.removeEventListener('click', followInternalLink);
    }, []);
    if (!auth) return <main className="app-loading"><h1>Maintain Console</h1><p
        role="status">{error || '正在检查登录状态…'}</p></main>;
    if (!login) return <LoginPage state={auth} returnTo={currentReturnTo()} onLogin={(user, returnTo) => {
        navigate(returnTo, true);
        setLogin(user);
    }}/>;
    const path = route;
    const run = path.match(/^\/tools\/([^/]+)\/?$/);
    const edit = path.match(/^\/workspace\/([^/]+)\/?$/);
    return <><Suspense fallback={<div className="app-loading">页面加载中…</div>}>
        {path === '/workspace/new' ? <CreateToolPage login={login}/> : run ?
            <ToolRunPage key={run[1]} id={run[1]} login={login}/> : edit ?
                <WorkspaceTabs scriptId={edit[1]} login={login}/>
                : path === '/workspace' ? <WorkspaceHome login={login}/>
                    : path.startsWith('/admin') ? <AdminPage login={login}/> : <ToolHome login={login}/>}
    </Suspense><SessionMenu login={login} logoutSupported={auth.provider === 'MOCK_SDK'} onLogout={async () => {
        const state = await api.getAuthState();
        navigate('/login', true);
        setAuth(state);
        setLogin(undefined);
    }}/></>;
}
