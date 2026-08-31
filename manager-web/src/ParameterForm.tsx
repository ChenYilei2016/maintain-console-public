import type {ChangeEvent} from 'react';
import {useEffect, useId, useRef, useState} from 'react';
import {PARAMETER_TYPES, parameterError} from './parameters';
import type {ParameterDefinition, ServiceInstance} from './types';

interface ParameterFormProps {
    definitions: ParameterDefinition[];
    values: Record<string, string>;
    onChange: (name: string, value: string) => void;
    instances?: ServiceInstance[];
}

type Field = HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement;

export default function ParameterForm({definitions, values, onChange, instances = []}: ParameterFormProps) {
    const prefix = useId();
    const fields = useRef(new Map<string, Field>());
    const [invalid, setInvalid] = useState<Set<string>>(new Set());
    useEffect(() => {
        for (const definition of definitions) {
            fields.current.get(definition.name)?.setCustomValidity(parameterError(definition, values[definition.name] || ''));
        }
    }, [definitions, values]);
    if (!definitions.length) return <p className="inline-empty">这个工具无需填写参数，确认目标后即可运行。</p>;

    const renderField = (definition: ParameterDefinition) => {
        const value = values[definition.name] || '';
        const error = invalid.has(definition.name) ? parameterError(definition, value) : '';
        const common = {
            id: prefix + definition.name, value,
            ref: (node: Field | null) => {
                if (node) fields.current.set(definition.name, node); else fields.current.delete(definition.name);
            },
            required: definition.required && definition.defaultValue == null,
            'aria-label': `运行参数 ${definition.name}`,
            'aria-invalid': Boolean(error),
            'aria-describedby': prefix + definition.name + '-help',
            onInvalid: (event: React.FormEvent<Field>) => {
                const details = event.currentTarget.closest('details');
                if (details) details.open = true;
                setInvalid(current => new Set([...current, definition.name]));
            },
            onChange: (event: ChangeEvent<Field>) => {
                event.currentTarget.setCustomValidity(parameterError(definition, event.target.value));
                onChange(definition.name, event.target.value);
            },
        };
        return <label key={definition.name} htmlFor={common.id} className="parameter-field">
            <span>{definition.label || definition.name}{definition.required && <b className="required-mark"> *</b>}
                {definition.label && <code className="technical-name">{definition.name}</code>}
                <small
                    className="parameter-type">{PARAMETER_TYPES[definition.type]}{definition.sensitive && ' · 敏感值'}</small></span>
            {definition.type === 'BOOLEAN' ? <select {...common}>
                <option value="">请选择</option>
                <option value="true">是</option>
                <option value="false">否</option>
            </select> : definition.type === 'ENUM' ? <select {...common}>
                <option value="">请选择</option>
                {(definition.options || []).map(option => <option key={option}>{option}</option>)}
            </select> : definition.type === 'SERVICE_INSTANCE' ? <select {...common}>
                <option value="">请选择服务实例</option>
                {value && !instances.some(instance => instance.id === value) &&
                    <option value={value}>{value}（当前未发现）</option>}
                {instances.map(instance => <option key={instance.id}
                                                   value={instance.id}>{instance.host}:{instance.port}</option>)}
            </select> : definition.type === 'MULTILINE' || definition.type === 'JSON' ? <textarea {...common}
                                                                                                  rows={definition.type === 'JSON' ? 4 : 3}
                                                                                                  placeholder={definition.example || ''}
                                                                                                  autoComplete="off"/> :
                <input {...common}
                       type={definition.sensitive ? 'password' : definition.type === 'NUMBER' ? 'number' : definition.type === 'DATETIME' ? 'datetime-local' : 'text'}
                       min={definition.min} max={definition.max} step={definition.type === 'NUMBER' ? 'any' : undefined}
                       placeholder={definition.example || ''}
                       autoComplete={definition.sensitive ? 'new-password' : 'off'}/>}
            <small id={common.id + '-help'}>{definition.description || '填写本次运行使用的值'}
                {definition.example && !definition.sensitive && <span> · 示例：{definition.example}</span>}
                {definition.defaultValue != null && !definition.sensitive &&
                    <span> · 默认：{String(definition.defaultValue)}</span>}
            </small>
            {error && <small className="field-error" role="alert">{error}</small>}
        </label>;
    };
    const groups = new Map<string, ParameterDefinition[]>();
    for (const definition of definitions.filter(item => !item.advanced)) {
        const group = definition.group || '常用参数';
        groups.set(group, [...(groups.get(group) || []), definition]);
    }
    const advanced = definitions.filter(item => item.advanced);
    return <div className="parameter-groups">
        {[...groups].map(([name, items]) => <section key={name}>
            <h4>{name} <small>{items.length}</small></h4>
            <div className="parameter-grid">{items.map(renderField)}</div>
        </section>)}
        {advanced.length > 0 && <details className="advanced-parameters">
            <summary>高级参数 · {advanced.length} 项{advanced.some(item => item.required) && '（含必填项）'}</summary>
            <div className="parameter-grid">{advanced.map(renderField)}</div>
        </details>}
    </div>;
}
