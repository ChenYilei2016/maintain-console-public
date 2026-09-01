import {useEffect, useState} from 'react';
import {api} from '../api';
import type {
    ConsoleRole,
    ConsoleUser,
    ConsoleUserStatus,
    EnvironmentManagementOverview,
    LoginInfo,
    UsageStatistics,
    UsageWindow
} from '../types';

const ROLE_LABELS: Record<ConsoleRole, string> = {ADMIN: '进入管理端'};

export default function AdminPage({login}: { login: LoginInfo }) {
    const [users, setUsers] = useState<ConsoleUser[]>([]);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [view, setView] = useState<'users' | 'usage' | 'environments'>('users');
    const [environmentOverview, setEnvironmentOverview] = useState<EnvironmentManagementOverview>();
    const [usageWindow, setUsageWindow] = useState<UsageWindow>('MONTH');
    const [usage, setUsage] = useState<UsageStatistics>();
    const load = () => api.getUsers().then(result => setUsers(result.data)).catch(failure => setError(failure.message));
    useEffect(() => {
        void load();
    }, []);
    useEffect(() => {
        if (view === 'environments' && !environmentOverview) {
            api.getEnvironmentOverview().then(setEnvironmentOverview).catch(failure => setError(failure.message));
        }
    }, [view, environmentOverview]);
    useEffect(() => {
        if (view === 'usage') {
            setUsage(undefined);
            api.getUsageStatistics(usageWindow).then(setUsage).catch(failure => setError(failure.message));
        }
    }, [view, usageWindow]);
    if (!login.administrator) return <main className="admin-page">
        <section className="admin-empty"><h1>无管理权限</h1>
            <p>管理入口需要服务端授予 ADMIN 角色。</p><a href="/workspace">返回脚本工作台</a></section>
    </main>;
    return <main className="admin-page">
        <header className="admin-header">
            <div><a href="/">Maintain Console</a><h1>管理中心</h1>
                <p>管理本系统用户与访问状态；脚本读、编、执、管仍由各自 JSON 决定。</p></div>
            <nav>
                <button className={view === 'users' ? 'active' : ''} onClick={() => setView('users')}>用户与权限
                </button>
                <button className={view === 'usage' ? 'active' : ''} onClick={() => setView('usage')}>运行概览</button>
                <button className={view === 'environments' ? 'active' : ''}
                        onClick={() => setView('environments')}>环境与连接
                </button>
            </nav>
        </header>
        {error && <p role="alert" className="safety-note">{error}</p>}
        {notice && <p role="status" className="admin-notice">{notice}</p>}
        {view === 'users' && <>
            <CreateUserForm onCreated={user => {
                setNotice(`已创建账号 ${user.employeeNo}`);
                void load();
            }}/>
            <section className="admin-user-list" aria-label="系统用户">
                {users.map(user => <UserRow key={user.id} user={user} currentUserId={login.userId} onSaved={() => {
                    setNotice(`已更新 ${user.displayName}，新的受保护请求立即生效`);
                    void load();
                }}/>)}</section>
            {!users.length && !error && <p className="admin-empty">还没有登录过的用户。</p>}</>}
        {view === 'usage' && <UsageOverview usage={usage} window={usageWindow} onWindowChange={setUsageWindow}/>}
        {view === 'environments' && <EnvironmentOverview overview={environmentOverview}/>}
    </main>;
}

function CreateUserForm({onCreated}: { onCreated: (user: ConsoleUser) => void }) {
    const [username, setUsername] = useState('');
    const [displayName, setDisplayName] = useState('');
    const [initialPassword, setInitialPassword] = useState('');
    const [roles, setRoles] = useState<ConsoleRole[]>([]);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    return <form className="admin-create-user" onSubmit={async event => {
        event.preventDefault();
        setSaving(true);
        setError('');
        try {
            const user = await api.createUser(username, displayName, initialPassword, roles);
            setUsername('');
            setDisplayName('');
            setInitialPassword('');
            setRoles([]);
            onCreated(user);
        } catch (failure) {
            setError(failure instanceof Error ? failure.message : '创建账号失败');
        } finally {
            setSaving(false);
        }
    }}>
        <header>
            <div><strong>创建登录账号</strong><span>用户名创建后不可修改，也是脚本 ACL 的稳定主体标识</span></div>
            <button className="primary" disabled={saving}>{saving ? '创建中…' : '创建账号'}</button>
        </header>
        <div className="admin-create-user-fields">
            <label><span>用户名</span><input required minLength={3} maxLength={64} value={username}
                                             pattern="[a-z0-9][a-z0-9._-]{2,63}"
                                             onChange={event => setUsername(event.target.value.toLowerCase())}/></label>
            <label><span>显示名称</span><input required maxLength={64} value={displayName}
                                               onChange={event => setDisplayName(event.target.value)}/></label>
            <label><span>初始密码</span><input required type="password" minLength={12} maxLength={72}
                                               autoComplete="new-password" value={initialPassword}
                                               onChange={event => setInitialPassword(event.target.value)}/></label>
        </div>
        <fieldset>
            <legend>管理端角色</legend>
            {(Object.keys(ROLE_LABELS) as ConsoleRole[]).map(role => <label key={role}>
                <input type="checkbox" checked={roles.includes(role)}
                       onChange={event => setRoles(current => event.target.checked
                           ? [...current, role] : current.filter(item => item !== role))}/>{ROLE_LABELS[role]}</label>)}
        </fieldset>
        {error && <small role="alert" className="login-error">{error}</small>}
    </form>;
}

function UsageOverview({usage, window, onWindowChange}: {
    usage?: UsageStatistics;
    window: UsageWindow;
    onWindowChange: (value: UsageWindow) => void;
}) {
    if (!usage) return <p className="admin-empty">正在汇总执行历史…</p>;
    const stats = [
        ['执行次数', usage.summary.totalExecutions],
        ['成功次数', usage.summary.successfulExecutions],
        ['失败次数', usage.summary.failedExecutions],
        ['活跃用户', usage.summary.activeUsers],
        ['活跃工具', usage.summary.activeTools],
        ['平均耗时', `${Math.round(usage.summary.averageDurationMillis)} ms`],
    ];
    return <section className="admin-usage-overview">
        <header>
            <div><strong>运行概览</strong><span>基于已落库的执行历史实时聚合</span></div>
            <label><span>统计周期</span><select value={window}
                                                onChange={event => onWindowChange(event.target.value as UsageWindow)}>
                <option value="WEEK">最近 7 天</option>
                <option value="MONTH">最近 30 天</option>
                <option value="QUARTER">最近 90 天</option>
            </select></label></header>
        <div className="admin-stat-grid">{stats.map(([label, value]) => <article key={label}>
            <span>{label}</span><strong>{value}</strong></article>)}</div>
        <div className="admin-usage-table">
            <div className="admin-table-heading"><strong>高频工具</strong>
                <span>最多展示 10 个，避免管理查询无界加载</span></div>
            {!usage.tools.length ? <p className="admin-empty">这个周期内还没有执行记录。</p> :
                <div className="admin-table-scroll">
                    <table>
                        <thead>
                        <tr>
                            <th>工具</th>
                            <th>服务</th>
                            <th>执行</th>
                            <th>成功率</th>
                            <th>平均耗时</th>
                            <th>最后运行</th>
                        </tr>
                        </thead>
                        <tbody>{usage.tools.map(tool => {
                            const successRate = tool.totalExecutions ? Math.round(tool.successfulExecutions * 100 / tool.totalExecutions) : 0;
                            return <tr key={tool.scriptId}>
                                <td><a href={`/workspace/${tool.scriptId}`}>{tool.scriptName}</a></td>
                                <td><code>{tool.serviceName}</code></td>
                                <td>{tool.totalExecutions}</td>
                                <td><span className="success-rate"><progress max="100" value={successRate}/>
                                    {successRate}%</span></td>
                                <td>{Math.round(tool.averageDurationMillis)} ms</td>
                                <td>{tool.lastRunTime?.replace('T', ' ') || '—'}</td>
                            </tr>;
                        })}</tbody>
                    </table>
                </div>}</div>
    </section>;
}

function EnvironmentOverview({overview}: { overview?: EnvironmentManagementOverview }) {
    if (!overview) return <p className="admin-empty">正在读取环境配置…</p>;
    return <section className="admin-environment-overview">
        <header><strong>发现模式：{overview.mode}</strong><span>页面不会返回 serverAddr、用户名或密码</span></header>
        <div className="admin-registry-grid">{overview.registries.map(registry => <article key={registry.id}>
            <strong>{registry.name || registry.id}</strong><code>{registry.id}</code>
            <span>namespaceId：{registry.namespaceId || 'public'}</span>
            <span>默认 group：{registry.defaultGroup}</span>
            <small>{registry.authenticationConfigured ? '已配置访问身份' : '未配置访问身份'}</small>
        </article>)}</div>
        <h2>目标环境</h2>
        <div className="admin-environment-list">{overview.environments.map(environment => <article key={environment.id}>
            <strong>{environment.name}{environment.production && <em>生产</em>}</strong><code>{environment.id}</code>
            <span>registry：{environment.registryId || 'Spring Cloud 默认连接'}</span>
            <span>group：{environment.groupName || '连接默认值'}</span>
            <small>实例集群：{environment.instanceClusters.length ? environment.instanceClusters.join('、') : '全部'}</small>
        </article>)}</div>
        <p className="admin-boundary">配置存在不等于 Client
            可达。真实连接、实例发现和协议兼容应分别诊断；不会在连接失败时自动切换环境。</p>
    </section>;
}

function UserRow({user, currentUserId, onSaved}: { user: ConsoleUser; currentUserId: string; onSaved: () => void }) {
    const [status, setStatus] = useState<ConsoleUserStatus>(user.status);
    const [roles, setRoles] = useState<ConsoleRole[]>(user.roles);
    const [saving, setSaving] = useState(false);
    const [newPassword, setNewPassword] = useState('');
    const [resettingPassword, setResettingPassword] = useState(false);
    const [notice, setNotice] = useState('');
    const [error, setError] = useState('');
    const changed = status !== user.status || [...roles].sort().join() !== [...user.roles].sort().join();
    return <article className="admin-user-card">
        <div className="admin-user-identity">
            <strong>{user.displayName}{user.id === currentUserId && ' · 当前账号'}</strong>
            <span>{user.employeeNo} · {user.provider}</span><small>最近登录：{user.lastLoginTime?.replace('T', ' ') || '—'}</small>
        </div>
        <label><span>状态</span><select value={status}
                                        onChange={event => setStatus(event.target.value as ConsoleUserStatus)}>
            <option value="ACTIVE">正常</option>
            <option value="DISABLED">停用</option>
        </select></label>
        <fieldset>
            <legend>系统角色</legend>
            {(Object.keys(ROLE_LABELS) as ConsoleRole[]).map(role => <label key={role}>
                <input type="checkbox" checked={roles.includes(role)}
                       onChange={event => setRoles(current => event.target.checked
                           ? [...current, role] : current.filter(item => item !== role))}/>{ROLE_LABELS[role]}</label>)}
        </fieldset>
        <div className="admin-user-actions">
            <button disabled={!changed || saving} onClick={async () => {
                setSaving(true);
                setError('');
                setNotice('');
                try {
                    await api.updateUser(user.id, status, roles);
                    onSaved();
                } catch (failure) {
                    setError(failure instanceof Error ? failure.message : '保存失败');
                } finally {
                    setSaving(false);
                }
            }}>{saving ? '保存中…' : '保存变更'}</button>
            <details>
                <summary>重置密码</summary>
                <form onSubmit={async event => {
                    event.preventDefault();
                    setResettingPassword(true);
                    setError('');
                    setNotice('');
                    try {
                        await api.resetUserPassword(user.id, newPassword);
                        setNewPassword('');
                        setNotice('密码已重置，下一次登录立即使用新密码');
                    } catch (failure) {
                        setError(failure instanceof Error ? failure.message : '密码重置失败');
                    } finally {
                        setResettingPassword(false);
                    }
                }}>
                    <input aria-label={`为 ${user.displayName} 设置新密码`} type="password" required minLength={12}
                           maxLength={72} autoComplete="new-password" value={newPassword}
                           placeholder="至少 12 个字符"
                           onChange={event => setNewPassword(event.target.value)}/>
                    <button disabled={resettingPassword}>{resettingPassword ? '重置中…' : '确认重置'}</button>
                </form>
            </details>
        </div>
        {notice && <small role="status" className="admin-action-notice">{notice}</small>}
        {error && <small role="alert" className="login-error">{error}</small>}
    </article>;
}
