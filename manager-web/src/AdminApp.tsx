import {Suspense} from 'react';
import AdminPage from './admin/AdminPage';
import AuthenticatedSurface from './identity/AuthenticatedSurface';
import {useNavigationPath} from './navigation';

export default function AdminApp() {
    useNavigationPath();
    return <AuthenticatedSurface fallback="/admin">{login =>
        <Suspense fallback={<div className="app-loading">管理端加载中…</div>}>
            <AdminPage login={login}/>
        </Suspense>
    }</AuthenticatedSurface>;
}

