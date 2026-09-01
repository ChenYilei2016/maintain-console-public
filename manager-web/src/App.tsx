import {lazy, Suspense} from 'react';
import AuthenticatedSurface from './identity/AuthenticatedSurface';
import {useNavigationPath} from './navigation';

const ScriptWorkspace = lazy(() => import('./workspace/ScriptWorkspace'));
const CreateToolPage = lazy(() => import('./workspace/CreateToolPage'));
const WorkspaceEntry = lazy(() => import('./workspace/WorkspaceEntry'));

export default function App() {
    const path = useNavigationPath();
    const edit = path.match(/^\/workspace\/([^/]+)\/?$/);
    return <AuthenticatedSurface fallback="/workspace">{login =>
        <Suspense fallback={<div className="app-loading">工作台加载中…</div>}>
            {path === '/workspace/new' ? <CreateToolPage login={login}/> : edit
                ? <ScriptWorkspace key={edit[1]} id={edit[1]} login={login}/>
                : <WorkspaceEntry login={login}/>}
        </Suspense>
    }</AuthenticatedSurface>;
}
