import {useState} from 'react';
import Modal from './Modal';
import {
    PARAMETER_TYPES,
    parameterDefinitions,
    parameterSchemaIssues,
    parameterValueText,
    parseParameterSchema
} from './parameters';
import {extractParameters} from './tree';
import type {ParameterDefinition, ParameterType} from './types';

interface Props {
    value: string;
    script: string;
    disabled: boolean;
    onChange: (value: string) => void;
    onEditScript: () => void;
    onLoadExample: () => void;
}

export default function ParameterSchemaEditor({value, script, disabled, onChange, onEditScript, onLoadExample}: Props) {
    const [showJson, setShowJson] = useState(false);
    const [showMigration, setShowMigration] = useState(false);
    const [editing, setEditing] = useState<{ index: number; parameter: ParameterDefinition }>();
    const [formError, setFormError] = useState('');
    let schema;
    let parseError = '';
    try {
        schema = parseParameterSchema(value);
    } catch (error) {
        parseError = error instanceof Error ? error.message : '参数 JSON 格式错误';
    }
    const definitions = parameterDefinitions(script, schema);
    const missing = extractParameters(script).filter((name) => !definitions.some((item) => item.name === name));
    const issues = parameterSchemaIssues(script, schema);
    const updateDefinitions = (parameters: ParameterDefinition[]) => onChange(JSON.stringify({
        version: 1,
        parameters
    }, null, 2));
    const updateDraft = (patch: Partial<ParameterDefinition>) => {
        setEditing((current) => current ? {...current, parameter: {...current.parameter, ...patch}} : current);
        setFormError('');
    };
    const saveDefinition = () => {
        if (!editing) return;
        try {
            const parameter = {...editing.parameter, name: editing.parameter.name.trim()};
            if (parameter.defaultValue === '') delete parameter.defaultValue;
            if (parameter.type === 'JSON' && typeof parameter.defaultValue === 'string') {
                parameter.defaultValue = JSON.parse(parameter.defaultValue);
            }
            const next = [...definitions];
            if (editing.index < 0) next.push(parameter);
            else next[editing.index] = parameter;
            parseParameterSchema(JSON.stringify({version: 1, parameters: next}));
            updateDefinitions(next);
            setEditing(undefined);
        } catch (error) {
            setFormError(error instanceof Error ? error.message : '参数配置无效');
        }
    };

    return <div className="schema-editor">
        <header className="schema-heading">
            <p>定义名称、类型和默认值，自动生成运行表单。</p>
            <div className="panel-actions">
                <button type="button" aria-expanded={showJson} onClick={() => setShowJson(!showJson)}>
                    高级 JSON
                </button>
                <button type="button" disabled={disabled} onClick={onLoadExample}>查看入门示例</button>
            </div>
        </header>
        <details className="parameter-help">
            <summary>参数怎么用？</summary>
            <div className="parameter-guide">
            <span><b>1</b> 添加参数：例如 count，类型选数字</span>
            <span><b>2</b> 脚本引用：<code>{'def count = $${count}'}</code></span>
                <span><b>3</b> 切换“运行填值”，再预览或执行</span>
            </div>
        </details>
        {showJson && <Modal title="高级参数 JSON" wide onClose={() => setShowJson(false)}
                            footer={<button type="button"
                                            onClick={() => setShowJson(false)}>完成，返回可视化配置</button>}>
            <label className="schema-json-field">
            <span>参数 Schema JSON <small>与可视化配置双向同步，修改后需保存脚本。</small></span>
            <textarea className="permission-editor" rows={12} value={value} disabled={disabled}
                      onChange={(event) => onChange(event.target.value)} spellCheck={false}/>
            </label>
            {parseError && <p className="field-error" role="alert">{parseError}</p>}
        </Modal>}
        {!parseError && <>
            <div className="schema-actions panel-actions">
                {!schema && <small>旧脚本兼容模式 · 编辑配置后启用类型化替换，请移除引用外层引号</small>}
                {!schema && <button type="button" disabled={disabled}
                                    onClick={() => setShowMigration(true)}>配置为类型化工具</button>}
                <button type="button" disabled={disabled || !missing.length} onClick={() => updateDefinitions([
                    ...definitions, ...missing.map((name): ParameterDefinition => ({name, type: 'STRING'})),
                ])}>从脚本识别{missing.length ? `（${missing.length}）` : ''}</button>
                <button className="primary" type="button" disabled={disabled} onClick={() => {
                    setFormError('');
                    setEditing({index: -1, parameter: {name: '', type: 'STRING'}});
                }}>＋ 添加参数
                </button>
            </div>
            {definitions.length ? <div className="parameter-definition-list">
                {definitions.map((parameter, index) => <article className="parameter-definition" key={parameter.name}>
                    <div className="parameter-definition-title"><strong>{parameter.name}</strong>
                        <span className="type-badge">{PARAMETER_TYPES[parameter.type]}</span>
                        {parameter.required && <span className="required-mark">必填</span>}
                        {parameter.sensitive && <span className="sensitive-badge">敏感值</span>}</div>
                    <p>{parameter.description || '未填写用途说明'}</p>
                    <div className="parameter-definition-meta">
                        <code>{'$${' + parameter.name + '}'}</code>
                        <span>默认值：{parameter.sensitive && parameter.defaultValue != null ? '已设置（隐藏）' : parameterValueText(parameter.defaultValue) || '未设置'}</span>
                        {parameter.type === 'ENUM' && <span>选项：{parameter.options?.join(' / ')}</span>}
                        {(parameter.min != null || parameter.max != null) &&
                            <span>范围：{parameter.min ?? '不限'} ～ {parameter.max ?? '不限'}</span>}
                    </div>
                    <div className="panel-actions">
                        <button type="button" disabled={disabled} onClick={() => {
                            setFormError('');
                            setEditing({index, parameter: {...parameter}});
                        }} aria-label={`编辑参数 ${parameter.name}`}>编辑
                        </button>
                        <button type="button" disabled={disabled}
                                onClick={() => updateDefinitions(definitions.filter((_, i) => i !== index))}
                                aria-label={`移除参数 ${parameter.name}`}>移除
                        </button>
                    </div>
                </article>)}
            </div> : <div className="parameter-empty">
                <strong>还没有可填写的参数</strong>
                <p>将每次运行会变化的值（订单号、查询数量、开关）配置为参数，使用者只需填表，不必修改脚本。</p>
            </div>}
        </>}
        {parseError &&
            <div className="field-error" role="alert">{parseError}。请打开高级 JSON 修正，已有内容不会被清空。</div>}
        {!parseError && issues.length > 0 && <div className="parameter-issues" role="status">
            {issues.map((issue) => <p key={issue}>{issue}</p>)}
            <button type="button" onClick={onEditScript}>前往编写脚本</button>
        </div>}
        <p className="schema-footnote">类型化引用不需要额外加引号；字符串会安全转义。配置修改保存在当前草稿中，点击“保存脚本”后持久化。</p>

        {showMigration && <Modal title="确认旧参数协议转换" wide onClose={() => setShowMigration(false)} footer={<>
            <button onClick={() => setShowMigration(false)}>取消</button>
            <button className="primary" onClick={() => {
                updateDefinitions(definitions);
                setShowMigration(false);
            }}>确认生成定义，继续逐项配置
            </button>
        </>}><p>旧协议把输入作为 Groovy 原文；新协议把输入作为数据。下面先按文本生成定义，数字、布尔、JSON
            等类型请逐项确认。</p>
            <p className="safety-note">代码不会自动改写。请手动移除占位符外层引号，并比较预览结果后再保存、授权。</p>
            <div className="source-comparison">
                <section><h3>当前代码（保持不变）</h3>
                    <pre>{script}</pre>
                </section>
                <section><h3>将新增的参数定义</h3>
                    <pre>{JSON.stringify({version: 1, parameters: definitions}, null, 2)}</pre>
                </section>
            </div>
        </Modal>}

        {editing && <Modal title={editing.index < 0 ? '添加参数' : `编辑参数 ${definitions[editing.index].name}`} wide
                           onClose={() => setEditing(undefined)} footer={<>
            <button type="button" onClick={() => setEditing(undefined)}>取消</button>
            <button className="primary" type="button" onClick={saveDefinition}>应用参数</button>
        </>}>
            <form className="parameter-definition-form" onSubmit={(event) => {
                event.preventDefault();
                saveDefinition();
            }}>
                <label><span>参数名称 <b className="required-mark">*</b></span>
                    <input autoFocus value={editing.parameter.name} placeholder="例如 orderId、count"
                           onChange={(event) => updateDraft({name: event.target.value})}/></label>
                <label><span>中文显示名</span><input value={editing.parameter.label || ''} placeholder="例如：订单编号"
                                                     onChange={event => updateDraft({label: event.target.value})}/></label>
                <label><span>参数分组</span><input value={editing.parameter.group || ''} placeholder="例如：筛选条件"
                                                   onChange={event => updateDraft({group: event.target.value})}/></label>
                <label><span>参数类型</span><select value={editing.parameter.type} onChange={(event) => updateDraft({
                    type: event.target.value as ParameterType, defaultValue: undefined, options: undefined,
                    min: undefined, max: undefined, pattern: undefined,
                })}>{Object.entries(PARAMETER_TYPES).map(([type, label]) => <option key={type}
                                                                                    value={type}>{label}</option>)}</select></label>
                <label className="full-width"><span>用途说明</span><input value={editing.parameter.description || ''}
                                                                          placeholder="告诉使用者需要填写什么，例如：要查询的订单编号"
                                                                          onChange={(event) => updateDraft({description: event.target.value})}/></label>
                {editing.parameter.type === 'ENUM' && <label className="full-width"><span>可选值（每行一个）</span>
                    <textarea rows={4} value={editing.parameter.options?.join('\n') || ''}
                              onChange={(event) => updateDraft({options: event.target.value.split('\n')})}/></label>}
                <label className="full-width"><span>默认值 <small>留空表示未设置</small></span>
                    {editing.parameter.type === 'BOOLEAN' ?
                        <select value={parameterValueText(editing.parameter.defaultValue)}
                                onChange={(event) => updateDraft({defaultValue: event.target.value})}>
                            <option value="">未设置</option>
                            <option value="true">是</option>
                            <option value="false">否</option>
                        </select> : editing.parameter.type === 'ENUM' ?
                            <select value={parameterValueText(editing.parameter.defaultValue)}
                                    onChange={(event) => updateDraft({defaultValue: event.target.value})}>
                                <option value="">未设置</option>
                                {editing.parameter.options?.filter(Boolean).map((option, i) => <option key={i}
                                                                                                       value={option}>{option}</option>)}
                            </select> : ['JSON', 'MULTILINE'].includes(editing.parameter.type) ?
                                <textarea rows={4} value={parameterValueText(editing.parameter.defaultValue)}
                                          onChange={(event) => updateDraft({defaultValue: event.target.value})}/> :
                                <input
                                    type={editing.parameter.sensitive ? 'password' : editing.parameter.type === 'NUMBER' ? 'number' : editing.parameter.type === 'DATETIME' ? 'datetime-local' : 'text'}
                                    step="any" value={parameterValueText(editing.parameter.defaultValue)}
                                    onChange={(event) => updateDraft({defaultValue: event.target.value})}/>}
                </label>
                {editing.parameter.type === 'NUMBER' && <>
                    <label><span>最小值</span><input type="number" step="any" value={editing.parameter.min ?? ''}
                                                     onChange={(event) => updateDraft({min: event.target.value === '' ? undefined : Number(event.target.value)})}/></label>
                    <label><span>最大值</span><input type="number" step="any" value={editing.parameter.max ?? ''}
                                                     onChange={(event) => updateDraft({max: event.target.value === '' ? undefined : Number(event.target.value)})}/></label>
                </>}
                {['STRING', 'MULTILINE'].includes(editing.parameter.type) &&
                    <label><span>格式校验（Java 正则，可选）</span>
                        <input value={editing.parameter.pattern || ''} placeholder="例如 ^[A-Za-z0-9_-]+$"
                               onChange={(event) => updateDraft({pattern: event.target.value})}/></label>}
                <label><span>输入示例（可选）</span><input value={editing.parameter.example || ''}
                                                         onChange={(event) => updateDraft({example: event.target.value})}/></label>
                <div className="parameter-flags full-width">
                    <label><input type="checkbox" checked={Boolean(editing.parameter.advanced)}
                                  onChange={event => updateDraft({advanced: event.target.checked})}/>放入高级参数区</label>
                    <label><input type="checkbox" checked={Boolean(editing.parameter.required)}
                                  onChange={(event) => updateDraft({required: event.target.checked})}/>必填参数</label>
                    <label><input type="checkbox" checked={Boolean(editing.parameter.sensitive)}
                                  onChange={(event) => updateDraft({sensitive: event.target.checked})}/>敏感值（执行记录中脱敏）</label>
                </div>
                {formError && <p className="field-error full-width" role="alert">{formError}</p>}
            </form>
        </Modal>}
    </div>;
}
