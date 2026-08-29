import {describe, expect, it} from 'vitest';
import {executionParameters, parameterDefinitions, parseParameterSchema} from './parameters';

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
});
