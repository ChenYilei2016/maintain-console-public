import {describe, expect, it} from 'vitest';
import {createScriptImportJson, MAX_SCRIPT_IMPORT_SIZE, parseScriptImport, toTreeNodeSaveRequest} from './scriptImport';
import validImportDocument from '../../../skills/maintain-console-script-author/references/example-import-v1.json';

const validImport = JSON.stringify(validImportDocument);

describe('工具导入文档', () => {
    it('把当前草稿生成可再导入的 JSON', () => {
        const json = createScriptImportJson({
            name: '问候工具',
            description: '根据称呼生成问候。',
            content: "return resultText('问候', 'Hello, ' + $${name})",
            parameterSchema: JSON.stringify(validImportDocument.script.parameterSchema),
            toolMetadata: {operationType: 'QUERY', riskNote: '只生成内存结果。'},
        });

        expect(parseScriptImport(json).script).toEqual({
            name: '问候工具',
            description: '根据称呼生成问候。',
            content: "return resultText('问候', 'Hello, ' + $${name})",
            parameterSchema: validImportDocument.script.parameterSchema,
            toolMetadata: {operationType: 'QUERY', riskNote: '只生成内存结果。'},
        });
    });

    it('把合法 V1 文档映射为新的私有工具创建请求', () => {
        const document = parseScriptImport(validImport);

        expect(toTreeNodeSaveRequest(document, {
            name: '问候工具',
            serviceName: 'demo-service',
            parentId: 'folder-1',
            allowedEnvironments: ['test'],
        })).toEqual({
            nodeType: 'script',
            nodeName: '问候工具',
            serviceName: 'demo-service',
            parentId: 'folder-1',
            allowedEnvironments: ['test'],
            content: "def name = $${name}\nreturn resultText('问候', 'Hello, ' + name)",
            parameterSchema: JSON.stringify(validImportDocument.script.parameterSchema),
            description: '根据称呼生成结构化文本结果。',
            toolMetadata: {
                operationType: 'QUERY',
                riskNote: '只生成内存结果。',
                usageExample: '输入称呼后查看文本结果。',
            },
        });
    });

    it('拒绝文档携带脚本授权等非可移植字段', () => {
        const value = JSON.parse(validImport);
        value.script.permissions = '{"readers":["someone"]}';

        expect(() => parseScriptImport(JSON.stringify(value))).toThrow('script.permissions');
    });

    it('操作类工具必须说明风险与影响范围', () => {
        const value = JSON.parse(validImport);
        value.script.toolMetadata.operationType = 'OPERATION';
        value.script.toolMetadata.riskNote = '';

        expect(() => parseScriptImport(JSON.stringify(value))).toThrow('操作类工具必须填写风险与影响范围');
    });

    it('敏感参数不能把默认值带入导入文档', () => {
        const value = JSON.parse(validImport);
        value.script.parameterSchema.parameters[0].sensitive = true;
        value.script.parameterSchema.parameters[0].defaultValue = 'secret';

        expect(() => parseScriptImport(JSON.stringify(value))).toThrow('敏感参数 name 不能包含默认值');
    });

    it('严格校验每层字段，避免拼写错误被静默忽略', () => {
        const value = JSON.parse(validImport);
        value.script.parameterSchema.parameters[0].require = true;

        expect(() => parseScriptImport(JSON.stringify(value)))
            .toThrow('script.parameterSchema.parameters[0].require');
    });

    it('在提交前执行现有工具字段长度限制', () => {
        const value = JSON.parse(validImport);
        value.script.name = 'a'.repeat(201);

        expect(() => parseScriptImport(JSON.stringify(value))).toThrow('工具名称不能超过 200 个字符');
    });

    it('拒绝参数属性使用错误的 JSON 类型', () => {
        const value = JSON.parse(validImport);
        value.script.parameterSchema.parameters[0].required = 'yes';

        expect(() => parseScriptImport(JSON.stringify(value)))
            .toThrow('script.parameterSchema.parameters[0].required 必须是布尔值');
    });

    it('拒绝超过导入上限的文档', () => {
        expect(() => parseScriptImport(' '.repeat(MAX_SCRIPT_IMPORT_SIZE + 1)))
            .toThrow('工具导入文档不能超过');
    });

});
