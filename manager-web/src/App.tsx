import {lazy, Suspense} from 'react';
import AuthenticatedSurface from './identity/AuthenticatedSurface';
import {useNavigationPath} from './navigation';

const WorkspaceTabs = lazy(() => import('./workspace/WorkspaceTabs'));
const CreateToolPage = lazy(() => import('./workspace/CreateToolPage'));
const WorkspaceHome = lazy(() => import('./workspace/WorkspaceHome'));

export default function App() {
    const path = useNavigationPath();
    const edit = path.match(/^\/workspace\/([^/]+)\/?$/);
    return <AuthenticatedSurface fallback="/workspace">{login =>
        <Suspense fallback={<div className="app-loading">工作台加载中…</div>}>
            {path === '/workspace/new' ? <CreateToolPage login={login}/> : edit
                ? <WorkspaceTabs scriptId={edit[1]} login={login}/>
                : <WorkspaceHome/>}
        </Suspense>
    }</AuthenticatedSurface>;
}
