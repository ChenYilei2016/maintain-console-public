import {type ReactNode, useEffect, useState} from 'react';
import {api} from '../api';
import {navigate} from '../navigation';
import type {AuthState, LoginInfo} from '../types';
import LoginPage from './LoginPage';
import SessionMenu from './SessionMenu';

function returnTo(fallback: string) {
    const value = window.location.pathname + window.location.search + window.location.hash;
    return value.startsWith('/') && !value.startsWith('//') && !value.startsWith('/login') ? value : fallback;
}

export default function AuthenticatedSurface({fallback, children}: {
    fallback: string;
    children: (login: LoginInfo) => ReactNode;
}) {
    const [login, setLogin] = useState<LoginInfo>();
    const [auth, setAuth] = useState<AuthState>();
    const [error, setError] = useState('');
    useEffect(() => {
        let active = true;
        api.getAuthState().then(async state => {
            if (!active) return;
            setAuth(state);
            if (state.authenticated) {
                const user = await api.getLoginInfo();
                if (active) setLogin(user);
            }
        }).catch(failure => {
            if (active) setError(failure instanceof Error ? failure.message : '登录状态检查失败');
        });
        return () => {
            active = false;
        };
    }, []);
    if (!auth) return <main className="app-loading"><h1>Maintain Console</h1><p
        role="status">{error || '正在检查登录状态…'}</p></main>;
    if (!login) return <LoginPage returnTo={returnTo(fallback)} onLogin={(user, destination) => {
        navigate(destination, true);
        setLogin(user);
    }}/>;
    return <>{children(login)}<SessionMenu login={login} logoutSupported={auth.provider === 'LOCAL_PASSWORD'}
                                           onLogout={async () => {
                                               const state = await api.getAuthState();
                                               navigate(fallback, true);
                                               setAuth(state);
                                               setLogin(undefined);
                                           }}/></>;
}
