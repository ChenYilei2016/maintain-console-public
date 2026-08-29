import type {ChangeEvent} from 'react';
import type {ParameterDefinition} from './types';

interface ParameterFormProps {
    definitions: ParameterDefinition[];
    values: Record<string, string>;
    onChange: (name: string, value: string) => void;
}

export default function ParameterForm({definitions, values, onChange}: ParameterFormProps) {
    if (!definitions.length) return <p className="inline-empty">当前脚本没有动态参数</p>;

    return <div className="parameter-grid">
        {definitions.map((definition) => {
            const value = values[definition.name] ?? '';
            const common = {
                id: `parameter-${definition.name}`,
                value,
                required: definition.required,
                onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
                    onChange(definition.name, event.target.value),
            };
            return <label key={definition.name} htmlFor={common.id}>
                <span>{definition.name}{definition.required && <b className="required-mark"> *</b>}</span>
                {definition.type === 'BOOLEAN' ? <select {...common}>
                    <option value="">请选择</option>
                    <option value="true">是</option>
                    <option value="false">否</option>
                </select> : definition.type === 'ENUM' ? <select {...common}>
                    <option value="">请选择</option>
                    {(definition.options || []).map((option) => <option key={option} value={option}>{option}</option>)}
                </select> : definition.type === 'MULTILINE' || definition.type === 'JSON' ?
                    <textarea {...common} rows={definition.type === 'JSON' ? 5 : 3}
                              placeholder={definition.example || definition.description || `输入 ${definition.name}`}/> :
                    <input {...common}
                           type={definition.sensitive ? 'password' : definition.type === 'NUMBER' ? 'number' : definition.type === 'DATETIME' ? 'datetime-local' : 'text'}
                           min={definition.min} max={definition.max}
                           placeholder={definition.example || definition.description || `输入 ${definition.name}`}/>}
                {definition.description && <small>{definition.description}</small>}
            </label>;
        })}
    </div>;
}
