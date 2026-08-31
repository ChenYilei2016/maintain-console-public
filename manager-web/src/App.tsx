import {lazy, Suspense, useEffect, useState} from 'react';
import {api} from './api';
import type {LoginInfo} from './types';

const ToolHome = lazy(() => import('./tools/ToolHome'));
const ToolRunPage = lazy(() => import('./tools/ToolRunPage'));
const ScriptWorkspace = lazy(() => import('./workspace/ScriptWorkspace'));
const CreateToolPage = lazy(() => import('./workspace/CreateToolPage'));

export default function App() {
    const [login, setLogin] = useState<LoginInfo>();
    const [error, setError] = useState('');
    useEffect(() => {
        let active = true;
        api.getLoginInfo().then(info => {
            if (active) setLogin(info);
        })
            .catch(failure => {
                if (active) setError(failure.message);
            });
        return () => {
            active = false;
        };
    }, []);
    if (!login) return <main className="app-loading"><h1>Maintain Console</h1><p
        role="status">{error || '正在检查登录身份…'}</p>
        {error && <><p>请通过公司的登录入口登录，再返回当前链接。工具不会自动运行。</p>
            <button onClick={() => window.location.reload()}>重新检查登录</button>
        </>}</main>;
    const path = window.location.pathname;
    const run = path.match(/^\/tools\/([^/]+)\/?$/);
    const edit = path.match(/^\/workspace\/([^/]+)\/?$/);
    return <Suspense fallback={<div className="app-loading">页面加载中…</div>}>
        {run ? <ToolRunPage key={run[1]} id={run[1]} login={login}/> : edit ?
            <ScriptWorkspace key={edit[1]} id={edit[1]} login={login}/>
            : path === '/workspace' ? <CreateToolPage login={login}/> : <ToolHome login={login}/>}
    </Suspense>;
}
