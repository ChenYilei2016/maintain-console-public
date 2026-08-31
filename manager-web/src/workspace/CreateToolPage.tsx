import {useEffect, useState} from 'react';
import {api} from '../api';
import type {LoginInfo} from '../types';
import {TOOL_TEMPLATES} from './templates';
import '../tools/tools.css';

export default function CreateToolPage({login}: { login: LoginInfo }) {
    const [services, setServices] = useState<string[]>([]);
    const [service, setService] = useState('');
    const [environment, setEnvironment] = useState(login.availableEnvironments[0]?.value || '');
    const [template, setTemplate] = useState<keyof typeof TOOL_TEMPLATES>('table');
    const [name, setName] = useState('');
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    useEffect(() => {
        api.listServices().then(setServices).catch(failure => setError(failure.message));
    }, []);
    return <main className="tool-home">
        <header className="tool-app-header"><a href="/">← 工具首页</a><strong>制作新工具</strong></header>
        <form className="new-tool-form form-stack" onSubmit={async event => {
            event.preventDefault();
            if (saving) return;
            setSaving(true);
            setError('');
            try {
                const selected = TOOL_TEMPLATES[template];
                const id = await api.saveTreeNode({
                    nodeType: 'script',
                    nodeName: name.trim(),
                    serviceName: service,
                    content: selected.content,
                    parameterSchema: selected.schema,
                    description: selected.description,
                    allowedEnvironments: [environment],
                    toolMetadata: {operationType: template === 'table' ? 'QUERY' : 'UNSPECIFIED'}
                });
                window.location.assign(`/workspace/${id}`);
            } catch (failure) {
                setError(failure instanceof Error ? failure.message : '创建失败');
                setSaving(false);
            }
        }}><h1>先做一个私有工具</h1>
            <p>保存后可以配置参数与用途，再通过授权面板分享给同事。新建不会继承其他人的授权，也不会自动运行。</p>
            <label><span>工具名称</span><input required maxLength={100} value={name}
                                               onChange={event => setName(event.target.value)}
                                               placeholder="例如：查询订单处理状态"/></label>
            <label><span>所属应用</span><select required value={service}
                                                onChange={event => setService(event.target.value)}>
                <option value="">请选择应用服务</option>
                {services.map(item => <option key={item}>{item}</option>)}</select></label>
            <label><span>初始允许环境</span><select required value={environment}
                                                    onChange={event => setEnvironment(event.target.value)}>{login.availableEnvironments.map(item =>
                <option value={item.value} key={item.value}>{item.name}</option>)}</select></label>
            <label><span>起步模板</span><select value={template}
                                                onChange={event => setTemplate(event.target.value as keyof typeof TOOL_TEMPLATES)}>{Object.entries(TOOL_TEMPLATES).map(([key, value]) =>
                <option value={key} key={key}>{value.name}</option>)}</select></label>
            {error && <p role="alert" className="safety-note">{error}</p>}
            <button className="primary" disabled={saving}>{saving ? '创建中…' : '创建并打开工作台'}</button>
        </form>
    </main>;
}
