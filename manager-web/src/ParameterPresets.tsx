import {useState} from 'react';
import {defaultParameterValues, parameterError, safeParameterValues} from './parameters';
import type {ParameterDefinition} from './types';

interface Preset {
    name: string;
    values: Record<string, string>
}

export default function ParameterPresets({userId, scriptId, environment, definitions, values, onChange}: {
    userId: string; scriptId: string; environment: string; definitions: ParameterDefinition[];
    values: Record<string, string>; onChange: (values: Record<string, string>) => void;
}) {
    const key = 'maintain-presets:' + JSON.stringify([userId, scriptId, environment]);
    const [name, setName] = useState('');
    const [notice, setNotice] = useState('');
    const [presets, setPresets] = useState<Preset[]>(() => {
        try {
            const stored = JSON.parse(localStorage.getItem(key) || '[]');
            return Array.isArray(stored) ? stored.slice(0, 5).filter(item => typeof item.name === 'string' && item.values && typeof item.values === 'object') : [];
        } catch {
            return [];
        }
    });
    return <details className="parameter-presets">
        <summary>默认值与个人预设</summary>
        <div className="preset-controls">
            <button type="button" onClick={() => onChange(defaultParameterValues(definitions))}>重置默认值</button>
            <select aria-label="加载个人参数预设" value="" onChange={event => {
                const preset = presets.find(item => item.name === event.target.value);
                if (!preset) return;
                const restored = {...defaultParameterValues(definitions), ...safeParameterValues(definitions, preset.values)};
                onChange(restored);
                const errors = definitions.filter(definition => parameterError(definition, restored[definition.name] || ''));
                setNotice(errors.length ? '已回填，请按当前参数定义重新检查：' + errors.map(item => item.label || item.name).join('、') : '已按当前参数定义回填，尚未执行');
            }}>
                <option value="">加载预设…</option>
                {presets.map(item => <option key={item.name}>{item.name}</option>)}</select>
        </div>
        <div className="preset-controls"><input aria-label="预设名称" maxLength={40} value={name}
                                                onChange={event => setName(event.target.value)}
                                                placeholder="给本组参数起个名字"/>
            <button type="button" disabled={!name.trim() || !environment} onClick={() => {
                const next = [{
                    name: name.trim(),
                    values: safeParameterValues(definitions, values)
                }, ...presets.filter(item => item.name !== name.trim())].slice(0, 5);
                try {
                    localStorage.setItem(key, JSON.stringify(next));
                    setPresets(next);
                    setName('');
                    setNotice('已保存到此浏览器，不包含敏感参数');
                } catch {
                    setNotice('浏览器存储不可用，预设未保存');
                }
            }}>保存预设
            </button>
        </div>
        <small>按当前用户、工具和环境隔离，最多保留 5 组；敏感值不回填、不持久化。</small>
        {notice && <p role="status">{notice}</p>}
    </details>;
}
