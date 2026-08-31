import type {ChangeEvent} from 'react';
import {PARAMETER_TYPES, parameterValueText} from './parameters';
import type {ParameterDefinition, ServiceInstance} from './types';

interface ParameterFormProps {
    definitions: ParameterDefinition[];
    values: Record<string, string>;
    onChange: (name: string, value: string) => void;
    instances?: ServiceInstance[];
}

export default function ParameterForm({definitions, values, onChange, instances = []}: ParameterFormProps) {
    if (!definitions.length) return <p
        className="inline-empty">当前脚本无需填写参数，可直接运行。需要动态输入？打开上方“配置参数”。</p>;

    return <div className="parameter-grid">
        {definitions.map((definition) => {
            const value = values[definition.name] ?? parameterValueText(definition.defaultValue);
            const common = {
                id: `parameter-${definition.name}`,
                value,
                required: definition.required && definition.defaultValue == null,
                'aria-label': `运行参数 ${definition.name}`,
                onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
                    onChange(definition.name, event.target.value),
            };
            return <label key={definition.name} htmlFor={common.id}>
                <span>{definition.name}{definition.required && <b className="required-mark"> *</b>}
                    <small
                        className="parameter-type">{PARAMETER_TYPES[definition.type]}{definition.sensitive && ' · 敏感值'}</small></span>
                {definition.type === 'BOOLEAN' ? <select {...common}>
                    <option value="">请选择</option>
                    <option value="true">是</option>
                    <option value="false">否</option>
                </select> : definition.type === 'ENUM' ? <select {...common}>
                    <option value="">请选择</option>
                    {(definition.options || []).map((option) => <option key={option} value={option}>{option}</option>)}
                </select> : definition.type === 'SERVICE_INSTANCE' ? <select {...common}>
                    <option value="">请选择服务实例</option>
                    {value && !instances.some((instance) => instance.id === value) &&
                        <option value={value}>{value}（当前未发现）</option>}
                    {instances.map((instance) => <option key={instance.id}
                                                         value={instance.id}>{instance.host}:{instance.port}</option>)}
                </select> : definition.type === 'MULTILINE' || definition.type === 'JSON' ?
                    <textarea {...common} rows={definition.type === 'JSON' ? 5 : 3}
                              placeholder={definition.example || definition.description || `输入 ${definition.name}`}/> :
                    <input {...common}
                           type={definition.sensitive ? 'password' : definition.type === 'NUMBER' ? 'number' : definition.type === 'DATETIME' ? 'datetime-local' : 'text'}
                           min={definition.min} max={definition.max}
                           step={definition.type === 'NUMBER' ? 'any' : undefined}
                           placeholder={definition.example || definition.description || `输入 ${definition.name}`}/>}
                <small>{definition.description || (definition.type === 'JSON' ? '输入合法 JSON，脚本中接收 JSON 字符串' : '填写本次运行使用的值')}
                    {definition.defaultValue != null &&
                        <span> · 默认：{definition.sensitive ? '已设置（隐藏）' : parameterValueText(definition.defaultValue)}</span>}
                    {(definition.min != null || definition.max != null) &&
                        <span> · 范围 {definition.min ?? '不限'} ～ {definition.max ?? '不限'}</span>}
                </small>
            </label>;
        })}
    </div>;
}
