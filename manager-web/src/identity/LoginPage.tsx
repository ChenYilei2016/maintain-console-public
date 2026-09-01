import {useState} from 'react';
import {api} from '../api';
import type {AuthState, LoginInfo} from '../types';

export default function LoginPage({state, returnTo, onLogin}: {
    state: AuthState; returnTo: string; onLogin: (login: LoginInfo, returnTo: string) => void;
}) {
    const [selected, setSelected] = useState(state.accounts[0]?.id || '');
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');
    return <main className="login-page">
        <section className="login-panel">
            <div className="login-brand"><span>MC</span>
                <div><h1>Maintain Console</h1>
                    <p>远程脚本运维与自动化工作台</p></div>
            </div>
            <div className="login-copy"><p className="eyebrow">DEMO SDK 登录</p><h2>选择演示身份</h2>
                <p>每个账号都会经过身份校验、本地用户状态检查和系统会话建立；不会因为本地已有用户而跳过登录。</p></div>
            <div className="mock-account-list">{state.accounts.map(account => <label key={account.id}
                                                                                     className={selected === account.id ? 'selected' : ''}>
                <input type="radio" name="mock-account" value={account.id} checked={selected === account.id}
                       onChange={() => setSelected(account.id)}/><span><strong>{account.name}</strong>
            <small>{account.description}</small><code>{account.id}</code></span></label>)}</div>
            {error && <p role="alert" className="login-error">{error}</p>}
            <button className="login-submit" disabled={!selected || submitting} onClick={async () => {
                setSubmitting(true);
                setError('');
                try {
                    const destination = await api.login(selected, returnTo);
                    onLogin(await api.getLoginInfo(), destination);
                } catch (failure) {
                    setError(failure instanceof Error ? failure.message : '登录失败');
                } finally {
                    setSubmitting(false);
                }
            }}>{submitting ? '正在建立会话…' : '进入工作台'}</button>
            <small className="login-notice">Mock 登录仅供 local/demo 环境，不会在生产环境自动启用。</small>
        </section>
    </main>;
}
