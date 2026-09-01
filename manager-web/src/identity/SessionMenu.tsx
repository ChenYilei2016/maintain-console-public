import {api} from '../api';
import type {LoginInfo} from '../types';

export default function SessionMenu({login, logoutSupported, onLogout}: {
    login: LoginInfo; logoutSupported: boolean; onLogout: () => void;
}) {
    return <details className="session-menu">
        <summary aria-label={`当前账号：${login.employeeName}`}>{login.employeeName.slice(0, 1)}</summary>
        <div className="session-popover" aria-label="当前登录用户">
            <span><strong>{login.employeeName}</strong><small>{login.employeeNo}</small></span>
            <a href="/workspace">工作台</a>
            {login.administrator && <a href="/admin">管理</a>}
            {logoutSupported ? <button onClick={async () => {
                await api.logout();
                for (const storage of [sessionStorage, localStorage]) {
                    Object.keys(storage).filter(key => key.startsWith('maintain-')).forEach(key => storage.removeItem(key));
                }
                onLogout();
            }}>退出</button> : <small>身份由接入层提供</small>}
        </div>
    </details>;
}
