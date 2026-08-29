import type {ParameterDefinition, ParameterSchema} from './types';
import {extractParameters} from './tree';

export function parseParameterSchema(json?: string): ParameterSchema | undefined {
    if (!json?.trim()) return undefined;
    const schema = JSON.parse(json) as ParameterSchema;
    if (schema.version !== 1 || !Array.isArray(schema.parameters)) {
        throw new Error('参数 Schema 必须是 version=1 且包含 parameters 数组');
    }
    const names = new Set<string>();
    for (const parameter of schema.parameters) {
        if (!parameter.name?.trim() || !parameter.type) throw new Error('每个参数都必须包含 name 和 type');
        if (names.has(parameter.name)) throw new Error(`参数名称重复：${parameter.name}`);
        names.add(parameter.name);
    }
    return schema;
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
