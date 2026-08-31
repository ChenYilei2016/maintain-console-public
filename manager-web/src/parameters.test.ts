import {describe, expect, it} from 'vitest';
import {
    executionParameters,
    parameterDefinitions,
    parameterSchemaIssues,
    parameterValueText,
    parseParameterSchema
} from './parameters';

describe('类型化参数 Schema', () => {
    it('解析 Schema 并保留旧占位符回退', () => {
        const schema = parseParameterSchema('{"version":1,"parameters":[{"name":"count","type":"NUMBER"}]}');
        expect(parameterDefinitions('return $${legacy}', schema).map((item) => item.name)).toEqual(['count']);
        expect(parameterDefinitions('return $${legacy}').map((item) => item.name)).toEqual(['legacy']);
    });

    it('类型化参数省略空值，旧参数继续传 null', () => {
        const definitions = [{name: 'name', type: 'STRING' as const}];
        expect(executionParameters(definitions, {name: ''}, true)).toEqual({});
        expect(executionParameters(definitions, {name: ''}, false)).toEqual({name: 'null'});
    });

    it('参数配置与脚本不一致时给出可操作提示', () => {
        const schema = parseParameterSchema('{"version":1,"parameters":[{"name":"count","type":"NUMBER"}]}');
        expect(parameterSchemaIssues('return $${other}', schema)).toEqual([
            '脚本中的 other 尚未配置，请点击“从脚本识别”。',
            'count 尚未用于脚本，请插入对应的参数引用。',
        ]);
        expect(parameterSchemaIssues('return $${count}', schema)).toEqual([]);
    });

    it('拒绝无效类型、重复名称、空枚举选项与倒置范围', () => {
        for (const parameters of [
            [{name: 'name', type: 'UNKNOWN'}],
            [{name: 'name', type: 'STRING'}, {name: ' name ', type: 'STRING'}],
            [{name: 'choice', type: 'ENUM', options: []}],
            [{name: 'count', type: 'NUMBER', min: 10, max: 1}],
            [{name: 'count', type: 'NUMBER', min: 1, max: 20, defaultValue: 99}],
            [{name: 'choice', type: 'ENUM', options: ['low'], defaultValue: 'unknown'}],
            [{name: 'body', type: 'JSON', defaultValue: '{'}],
        ]) expect(() => parseParameterSchema(JSON.stringify({version: 1, parameters}))).toThrow();
    });

    it('对象默认值与历史参数按 JSON 展示而非 object Object', () => {
        expect(parameterValueText({enabled: true})).toBe('{\n  "enabled": true\n}');
        expect(parameterValueText(false)).toBe('false');
        expect(parameterValueText(0)).toBe('0');
    });
});
