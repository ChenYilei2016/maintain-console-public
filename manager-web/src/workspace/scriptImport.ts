import {parameterSchemaIssues, parseParameterSchema} from '../parameters';
import type {OperationType, ParameterSchema, ToolMetadata, TreeNodeSaveRequest} from '../types';
import {OPERATION_TYPES} from '../types';

export const SCRIPT_IMPORT_FORMAT = 'maintain-console.script-import' as const;
export const SCRIPT_IMPORT_VERSION = 1 as const;
export const MAX_SCRIPT_IMPORT_SIZE = 1_400_000;
export const SCRIPT_IMPORT_LIMITS = {
    name: 200,
    content: 1_048_576,
    parameterSchema: 262_144,
    description: 4000,
    metadataText: 4000,
} as const;

export interface ScriptImportDocument {
    format: typeof SCRIPT_IMPORT_FORMAT;
    version: typeof SCRIPT_IMPORT_VERSION;
    script: {
        name: string;
        description: string;
        content: string;
        parameterSchema: ParameterSchema;
        toolMetadata: ToolMetadata;
    };
}

export interface ScriptImportCreateOptions {
    name: string;
    serviceName: string;
    parentId?: string;
    allowedEnvironments: string[];
}

export interface ScriptImportSource {
    name: string;
    description?: string;
    content: string;
    parameterSchema?: string;
    toolMetadata?: ToolMetadata;
}

const isObject = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null && !Array.isArray(value);
const isOperationType = (value: unknown): value is OperationType =>
    Object.values(OPERATION_TYPES).some(operationType => operationType === value);

function assertKnownFields(path: string, value: Record<string, unknown>, allowed: readonly string[]) {
    const unknown = Object.keys(value).find(field => !allowed.includes(field));
    if (unknown) throw new Error(`不支持的字段：${path}.${unknown}`);
}

function assertTextLimit(label: string, value: string, max: number) {
    if (value.length > max) throw new Error(`${label}不能超过 ${max} 个字符`);
}

export function parseScriptImport(json: string): ScriptImportDocument {
    if (json.length > MAX_SCRIPT_IMPORT_SIZE) {
        throw new Error(`工具导入文档不能超过 ${MAX_SCRIPT_IMPORT_SIZE} 个字符`);
    }
    let value: unknown;
    try {
        value = JSON.parse(json);
    } catch {
        throw new Error('导入 JSON 语法错误，请检查双引号、逗号与括号');
    }
    if (!isObject(value) || value.format !== SCRIPT_IMPORT_FORMAT || value.version !== SCRIPT_IMPORT_VERSION
        || !isObject(value.script)) {
        throw new Error(`工具导入文档必须使用 ${SCRIPT_IMPORT_FORMAT} v${SCRIPT_IMPORT_VERSION}`);
    }
    assertKnownFields('$', value, ['format', 'version', 'script']);
    const script = value.script;
    assertKnownFields('script', script, ['name', 'description', 'content', 'parameterSchema', 'toolMetadata']);
    if (typeof script.name !== 'string' || !script.name.trim()
        || typeof script.description !== 'string' || !script.description.trim()
        || typeof script.content !== 'string' || !script.content.trim()
        || !isObject(script.parameterSchema) || !isObject(script.toolMetadata)
        || !isOperationType(script.toolMetadata.operationType)) {
        throw new Error('工具导入文档缺少有效的名称、说明、脚本、参数定义或操作类型');
    }
    assertKnownFields('script.toolMetadata', script.toolMetadata, ['operationType', 'riskNote', 'usageExample']);
    assertKnownFields('script.parameterSchema', script.parameterSchema, ['version', 'parameters']);
    if (Array.isArray(script.parameterSchema.parameters)) {
        script.parameterSchema.parameters.forEach((parameter, index) => {
            if (!isObject(parameter)) return;
            const path = `script.parameterSchema.parameters[${index}]`;
            assertKnownFields(path, parameter,
                ['name', 'label', 'group', 'advanced', 'type', 'required', 'defaultValue', 'description', 'example',
                    'options', 'pattern', 'min', 'max', 'sensitive']);
            for (const field of ['name', 'label', 'group', 'type', 'description', 'example', 'pattern'] as const) {
                if (parameter[field] != null && typeof parameter[field] !== 'string') {
                    throw new Error(`${path}.${field} 必须是文本`);
                }
            }
            for (const field of ['advanced', 'required', 'sensitive'] as const) {
                if (parameter[field] != null && typeof parameter[field] !== 'boolean') {
                    throw new Error(`${path}.${field} 必须是布尔值`);
                }
            }
            if (parameter.options != null && (!Array.isArray(parameter.options)
                || parameter.options.some(option => typeof option !== 'string'))) {
                throw new Error(`${path}.options 必须是文本数组`);
            }
        });
    }
    if (script.toolMetadata.operationType === OPERATION_TYPES.OPERATION
        && (typeof script.toolMetadata.riskNote !== 'string' || !script.toolMetadata.riskNote.trim())) {
        throw new Error('操作类工具必须填写风险与影响范围');
    }
    for (const field of ['riskNote', 'usageExample'] as const) {
        const text = script.toolMetadata[field];
        if (text != null && typeof text !== 'string') throw new Error(`script.toolMetadata.${field} 必须是文本`);
        if (typeof text === 'string') assertTextLimit(field === 'riskNote' ? '风险说明' : '使用示例', text,
            SCRIPT_IMPORT_LIMITS.metadataText);
    }
    assertTextLimit('工具名称', script.name, SCRIPT_IMPORT_LIMITS.name);
    assertTextLimit('工具说明', script.description, SCRIPT_IMPORT_LIMITS.description);
    assertTextLimit('脚本内容', script.content, SCRIPT_IMPORT_LIMITS.content);
    assertTextLimit('参数 Schema', JSON.stringify(script.parameterSchema), SCRIPT_IMPORT_LIMITS.parameterSchema);
    const parameterSchema = parseParameterSchema(JSON.stringify(script.parameterSchema));
    if (!parameterSchema) throw new Error('工具导入文档缺少参数定义');
    for (const parameter of parameterSchema.parameters) {
        if (parameter.sensitive && Object.hasOwn(parameter, 'defaultValue')) {
            throw new Error(`敏感参数 ${parameter.name} 不能包含默认值`);
        }
    }
    const issues = parameterSchemaIssues(script.content, parameterSchema);
    if (issues.length) throw new Error(issues.join(' '));
    return {
        format: SCRIPT_IMPORT_FORMAT,
        version: SCRIPT_IMPORT_VERSION,
        script: {
            name: script.name.trim(),
            description: script.description.trim(),
            content: script.content,
            parameterSchema,
            toolMetadata: {
                operationType: script.toolMetadata.operationType,
                riskNote: typeof script.toolMetadata.riskNote === 'string' ? script.toolMetadata.riskNote.trim() : undefined,
                usageExample: typeof script.toolMetadata.usageExample === 'string' ? script.toolMetadata.usageExample.trim() : undefined,
            },
        },
    };
}

export function createScriptImportJson(source: ScriptImportSource): string {
    const document = parseScriptImport(JSON.stringify({
        format: SCRIPT_IMPORT_FORMAT,
        version: SCRIPT_IMPORT_VERSION,
        script: {
            name: source.name,
            description: source.description?.trim() || source.name,
            content: source.content,
            parameterSchema: parseParameterSchema(source.parameterSchema) || {version: 1, parameters: []},
            toolMetadata: source.toolMetadata || {operationType: OPERATION_TYPES.UNSPECIFIED},
        },
    }));
    return JSON.stringify(document, null, 2);
}

export function toTreeNodeSaveRequest(document: ScriptImportDocument,
                                      options: ScriptImportCreateOptions): TreeNodeSaveRequest {
    const name = options.name.trim();
    if (!name) throw new Error('工具名称不能为空');
    assertTextLimit('工具名称', name, SCRIPT_IMPORT_LIMITS.name);
    if (!options.serviceName.trim()) throw new Error('请选择所属应用');
    if (!options.allowedEnvironments.length) throw new Error('请至少选择一个允许环境');
    return {
        nodeType: 'script',
        nodeName: name,
        serviceName: options.serviceName,
        parentId: options.parentId,
        allowedEnvironments: options.allowedEnvironments,
        content: document.script.content,
        parameterSchema: JSON.stringify(document.script.parameterSchema),
        description: document.script.description,
        toolMetadata: document.script.toolMetadata,
    };
}
