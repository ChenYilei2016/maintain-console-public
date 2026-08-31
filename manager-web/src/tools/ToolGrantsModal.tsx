import {useEffect, useState} from 'react';
import Modal from '../Modal';
import type {EnvironmentOption} from '../types';
import type {ToolGrants, ToolPermissions} from './toolApi';
import {loadGrants, saveGrants} from './toolApi';

const CAPABILITIES = {readerNo: '查看代码', editorNo: '编辑脚本', invokerNo: '运行工具'} as const;
const PRESETS = {
    run: ['invokerNo'],
    readRun: ['readerNo', 'invokerNo'],
    develop: ['readerNo', 'editorNo', 'invokerNo']
} as const;
type Capability = keyof typeof CAPABILITIES;
const employees = (text?: string) => (text || '').split(',').map(value => value.trim()).filter(Boolean);

export default function ToolGrantsModal({scriptId, environments, onSaved, onClose}: {
    scriptId: string; environments: EnvironmentOption[]; onSaved: () => void; onClose: () => void;
}) {
    const [grants, setGrants] = useState<ToolGrants>();
    const [employee, setEmployee] = useState('');
    const [preset, setPreset] = useState<keyof typeof PRESETS>('run');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);
    const [copied, setCopied] = useState(false);
    useEffect(() => {
        loadGrants(scriptId).then(value => setGrants({
            ...value, permissions: {
                ...value.permissions,
                allowedEnvironments: value.permissions.allowedEnvironments || []
            }
        })).catch(failure => setError(failure.message));
    }, [scriptId]);
    const patch = (value: Partial<ToolPermissions>) => setGrants(current => current ? {
        ...current,
        permissions: {...current.permissions, ...value}
    } : current);
    const ids = grants ? [...new Set(Object.keys(CAPABILITIES).flatMap(key => employees(grants.permissions[key as Capability])))] : [];
    return <Modal title="授权与分享" wide onClose={onClose} footer={<>
        <button type="button" onClick={onClose}>取消</button>
        <button className="primary" disabled={!grants || saving} onClick={async () => {
            if (!grants) return;
            if (grants.permissions.version === 1 && !window.confirm('保存后改为显式授权：空名单不再代表公开阅读。请确认当前名单和允许环境。')) return;
            setSaving(true);
            setError('');
            try {
                await saveGrants(scriptId, grants.version, grants.permissions);
                onSaved();
                onClose();
            } catch (failure) {
                setError(failure instanceof Error ? failure.message : '授权保存失败');
            } finally {
                setSaving(false);
            }
        }}>{saving ? '保存中…' : '保存授权'}</button>
    </>}>
        {error && <p className="safety-note" role="alert">{error}</p>}
        {grants && <div className="grants-panel">
            <p>创建者 <strong>{grants.ownerId}</strong> 与管理员负责授权。新配置中编辑包含查看源码，但不包含运行或授权管理；链接需要登录，不会自动运行。
            </p>
            {grants.permissions.version === 1 &&
                <p className="safety-note">这是旧版权限配置。公开工具的空阅读名单可能代表公开阅读，保存前请明确核对授权名单；不会批量改写其他工具。</p>}
            <div className="grant-add"><input aria-label="员工 ID" placeholder="输入员工 ID，不是姓名" value={employee}
                                              maxLength={80} onChange={event => setEmployee(event.target.value)}/>
                <select aria-label="授权组合" value={preset}
                        onChange={event => setPreset(event.target.value as keyof typeof PRESETS)}>
                    <option value="run">仅运行</option>
                    <option value="readRun">查看与运行</option>
                    <option value="develop">协作开发</option>
                </select>
                <button type="button" disabled={!employee.trim() || employee.includes(',')} onClick={() => {
                    const changes: Partial<ToolPermissions> = {};
                    for (const capability of PRESETS[preset]) changes[capability] = [...new Set([...employees(grants.permissions[capability]), employee.trim()])].join(',');
                    patch(changes);
                    setEmployee('');
                }}>添加授权
                </button>
            </div>
            <table className="grants-table">
                <thead>
                <tr>
                    <th>员工 ID</th>
                    {Object.values(CAPABILITIES).map(label => <th key={label}>{label}</th>)}
                    <th/>
                </tr>
                </thead>
                <tbody>{ids.map(id => <tr key={id}>
                    <td>{id}</td>
                    {Object.entries(CAPABILITIES).map(([key, label]) => <td key={key}>
                        <input type="checkbox" aria-label={`${id} ${label}`}
                               checked={employees(grants.permissions[key as Capability]).includes(id)}
                               onChange={event => patch({
                                   [key]: event.target.checked
                                       ? [...employees(grants.permissions[key as Capability]), id].join(',') : employees(grants.permissions[key as Capability]).filter(value => value !== id).join(',')
                               })}/>
                    </td>)}
                    <td>
                        <button type="button"
                                onClick={() => patch(Object.fromEntries(Object.keys(CAPABILITIES).map(key => [key, employees(grants.permissions[key as Capability]).filter(value => value !== id).join(',')])))}>移除
                        </button>
                    </td>
                </tr>)}</tbody>
            </table>
            {!ids.length && <p className="inline-empty">未向其他员工授权，仅创建者和管理员可用。</p>}
            <fieldset>
                <legend>允许环境</legend>
                {environments.map(environment => <label key={environment.value}>
                    <input type="checkbox"
                           checked={grants.permissions.allowedEnvironments?.includes(environment.value) || false}
                           onChange={event => patch({
                               allowedEnvironments: event.target.checked
                                   ? [...(grants.permissions.allowedEnvironments || []), environment.value]
                                   : (grants.permissions.allowedEnvironments || []).filter(value => value !== environment.value)
                           })}/>{environment.name}{environment.production && ' · 生产'}
                </label>)}</fieldset>
            <label><input type="checkbox" checked={grants.permissions.allowAllInstances}
                          onChange={event => patch({allowAllInstances: event.target.checked})}/>允许在当前请求中对全部实例执行（有数量与并发上限）</label>
            <label><input type="checkbox" checked={grants.permissions.enabled}
                          onChange={event => patch({enabled: event.target.checked})}/>工具启用；停用后拒绝新的运行，不终止已发出的操作</label>
            <div className="share-link"><code>{window.location.origin}/tools/{scriptId}</code>
                <button type="button" onClick={async () => {
                    try {
                        await navigator.clipboard.writeText(`${window.location.origin}/tools/${scriptId}`);
                        setCopied(true);
                    } catch {
                        setError('剪贴板不可用，请手动复制上面的链接');
                    }
                }}>{copied ? '链接已复制' : '复制工具链接'}</button>
            </div>
        </div>}
    </Modal>;
}
