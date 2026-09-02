import type {ParameterDefinition, ParameterSchema, ParameterType} from './types';
import {extractParameters} from './tree';

export const PARAMETER_TYPES: Record<ParameterType, string> = {
    STRING: '文本', NUMBER: '数字', BOOLEAN: '是 / 否', ENUM: '下拉选项',
    JSON: 'JSON', MULTILINE: '多行文本', DATETIME: '日期时间', SERVICE_INSTANCE: '服务实例',
};

const ISO_LOCAL_DATE_TIME = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.\d{1,9})?)?$/;

function isIsoLocalDateTime(value: string) {
    const match = ISO_LOCAL_DATE_TIME.exec(value);
    if (!match) return false;
    const [year, month, day, hour, minute, second] = match.slice(1, 7).map(Number);
    const date = new Date(0);
    date.setUTCFullYear(year, month - 1, day);
    date.setUTCHours(hour, minute, second || 0, 0);
    return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
        && date.getUTCHours() === hour && date.getUTCMinutes() === minute && date.getUTCSeconds() === (second || 0);
}

export function parameterValueText(value: unknown): string {
    return value == null ? '' : typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value);
}

export function parseParameterSchema(json?: string): ParameterSchema | undefined {
    if (!json?.trim()) return undefined;
    let schema: ParameterSchema;
    try {
        schema = JSON.parse(json) as ParameterSchema;
    } catch {
        throw new Error('参数 JSON 语法错误，请检查双引号、逗号与括号');
    }
    if (!schema || schema.version !== 1 || !Array.isArray(schema.parameters)) {
        throw new Error('参数 Schema 必须是 version=1 且包含 parameters 数组');
    }
    const names = new Set<string>();
    for (const parameter of schema.parameters) {
        if (!parameter || typeof parameter.name !== 'string' || !parameter.name.trim()) {
            throw new Error('每个参数都必须填写名称');
        }
        parameter.name = parameter.name.trim();
        if (/[{}\r\n]/.test(parameter.name)) throw new Error('参数名称不能包含大括号或换行');
        if (!Object.hasOwn(PARAMETER_TYPES, parameter.type)) throw new Error(`参数 ${parameter.name} 的类型不受支持`);
        if (names.has(parameter.name)) throw new Error(`参数名称重复：${parameter.name}`);
        if (parameter.type === 'ENUM' && (!Array.isArray(parameter.options) || !parameter.options.length
            || parameter.options.some((option) => typeof option !== 'string' || !option.trim()))) {
            throw new Error(`参数 ${parameter.name} 至少需要一个有效的下拉选项`);
        }
        if ((parameter.min != null && !Number.isFinite(parameter.min))
            || (parameter.max != null && !Number.isFinite(parameter.max))) throw new Error('参数范围必须是有效数字');
        if (parameter.min != null && parameter.max != null && parameter.min > parameter.max) {
            throw new Error(`参数 ${parameter.name} 的最小值不能大于最大值`);
        }
        const defaultValue = parameterValueText(parameter.defaultValue);
        if (defaultValue.trim()) {
            if (parameter.type === 'NUMBER') {
                const number = Number(defaultValue);
                if (!Number.isFinite(number) || (parameter.min != null && number < parameter.min)
                    || (parameter.max != null && number > parameter.max)) {
                    throw new Error(`参数 ${parameter.name} 的默认值必须是范围内的数字`);
                }
            }
            if (parameter.type === 'ENUM' && !parameter.options?.includes(defaultValue)) {
                throw new Error(`参数 ${parameter.name} 的默认值不在可选值中`);
            }
            if (parameter.type === 'BOOLEAN' && !['true', 'false'].includes(defaultValue.toLowerCase())) {
                throw new Error(`参数 ${parameter.name} 的默认值必须是 true 或 false`);
            }
            if (parameter.type === 'JSON') {
                try {
                    JSON.parse(defaultValue);
                } catch {
                    throw new Error(`参数 ${parameter.name} 的默认值不是合法 JSON`);
                }
            }
            if (parameter.type === 'DATETIME' && !isIsoLocalDateTime(defaultValue)) {
                throw new Error(`参数 ${parameter.name} 的默认值必须是 ISO-8601 本地日期时间`);
            }
        }
        names.add(parameter.name);
    }
    return schema;
}

export function parameterSchemaIssues(script: string, schema?: ParameterSchema): string[] {
    if (!schema) return [];
    const placeholders = extractParameters(script);
    const declared = schema.parameters.map((parameter) => parameter.name);
    const missing = placeholders.filter((name) => !declared.includes(name));
    const unused = declared.filter((name) => !placeholders.includes(name));
    return [
        ...(missing.length ? [`脚本中的 ${missing.join('、')} 尚未配置，请点击“从脚本识别”。`] : []),
        ...(unused.length ? [`${unused.join('、')} 尚未用于脚本，请插入对应的参数引用。`] : []),
    ];
}

export function parameterDefinitions(script: string, schema?: ParameterSchema): ParameterDefinition[] {
    return schema?.parameters ?? extractParameters(script).map((name) => ({name, type: 'STRING'}));
}

export function executionParameters(
    definitions: ParameterDefinition[],
    values: Record<string, string>,
    typed: boolean,
): Record<string, unknown> {
    return Object.fromEntries(definitions.flatMap((definition) => {
        const value = values[definition.name];
        if (typed && !value?.trim()) return [];
        return [[definition.name, value?.trim() ? value : 'null']];
    }));
}

export function parameterError(definition: ParameterDefinition, value: string): string {
    if (!value.trim()) return definition.required && definition.defaultValue == null ? '请填写此必填参数' : '';
    if (definition.type === 'NUMBER') {
        const number = Number(value);
        if (!Number.isFinite(number)) return '请输入有效数字';
        if (definition.min != null && number < definition.min) return `不能小于 ${definition.min}`;
        if (definition.max != null && number > definition.max) return `不能大于 ${definition.max}`;
    }
    if (definition.type === 'JSON') {
        try {
            JSON.parse(value);
        } catch {
            return '请输入合法 JSON';
        }
    }
    if (definition.type === 'ENUM' && !definition.options?.includes(value)) return '请选择当前可用选项';
    if (definition.type === 'BOOLEAN' && !['true', 'false'].includes(value)) return '请选择是或否';
    return '';
}

/** 缓存、预设和历史回填共用同一敏感值过滤规则。 */
export function safeParameterValues(definitions: ParameterDefinition[], values: Record<string, unknown>): Record<string, string> {
    return Object.fromEntries(definitions.filter(definition => !definition.sensitive && Object.hasOwn(values, definition.name))
        .map(definition => [definition.name, parameterValueText(values[definition.name])]));
}

export function defaultParameterValues(definitions: ParameterDefinition[]): Record<string, string> {
    return Object.fromEntries(definitions.map(definition => [definition.name,
        definition.sensitive ? '' : parameterValueText(definition.defaultValue)]));
}
