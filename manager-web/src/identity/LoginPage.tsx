import {useState} from 'react';
import {api} from '../api';
import type {LoginInfo} from '../types';

export default function LoginPage({returnTo, onLogin}: {
    returnTo: string; onLogin: (login: LoginInfo, returnTo: string) => void;
}) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');
    const administration = returnTo === '/admin' || returnTo.startsWith('/admin/');
    const submit = async () => {
        setSubmitting(true);
        setError('');
        try {
            const destination = await api.login(username, password, returnTo);
            onLogin(await api.getLoginInfo(), destination);
        } catch (failure) {
            setError(failure instanceof Error ? failure.message : '登录失败');
        } finally {
            setSubmitting(false);
        }
    };
    return <main className="login-page">
        <section className="login-panel">
            <div className="login-brand"><span>MC</span>
                <div><h1>Maintain Console</h1>
                    <p>远程脚本运维与自动化工作台</p></div>
            </div>
            <div className="login-copy"><p className="eyebrow">独立账号登录</p>
                <h2>{administration ? '登录管理端' : '登录脚本工作台'}</h2>
                <p>{administration ? '仅 ADMIN 角色可进入；管理角色不会授予任何脚本能力。'
                    : '使用管理员分配的账号登录；脚本读、编、执、管能力全部来自脚本 JSON。'}</p></div>
            <form className="login-form" onSubmit={event => {
                event.preventDefault();
                void submit();
            }}>
                <label><span>用户名</span><input autoFocus autoComplete="username" value={username}
                                                 onChange={event => setUsername(event.target.value)}/></label>
                <label><span>密码</span><input type="password" autoComplete="current-password" value={password}
                                               onChange={event => setPassword(event.target.value)}/></label>
                {error && <p role="alert" className="login-error">{error}</p>}
                <button className="login-submit" disabled={!username.trim() || !password || submitting}>
                    {submitting ? '正在建立会话…' : '登录'}
                </button>
            </form>
            <small className="login-notice">首次启动需通过 MAINTAIN_ADMIN_INITIAL_PASSWORD 安全创建管理员。</small>
        </section>
    </main>;
}
