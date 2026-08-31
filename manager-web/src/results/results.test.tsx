import {describe, expect, it} from 'vitest';
import {renderToStaticMarkup} from 'react-dom/server';
import ResultTable, {tableCsv} from './ResultTable';
import {defaultParameterValues, parameterError, safeParameterValues} from '../parameters';
import type {ParameterDefinition} from '../types';

describe('运行数据契约', () => {
    it('CSV 处理引号换行与公式，真实数字保持数值', () => {
        expect(tableCsv(['列'], [['a,"b\n'], [' =1+1'], ['@SUM(A1)'], [-5]]))
            .toBe('"列"\r\n"a,""b\n"\r\n"\' =1+1"\r\n"\'@SUM(A1)"\r\n"-5"');
    });
    it('大结果只渲染当前页，说明截断和本次数据范围', () => {
        const html = renderToStaticMarkup(<ResultTable block={{
            type: 'table', data: {
                columns: ['id'],
                rows: Array.from({length: 1000}, (_, index) => [index]), truncated: true, returnedRowCount: 1200
            }
        }}/>);
        expect(html).toContain('当前返回 1000 行');
        expect(html).toContain('结果已截断');
        expect(html.match(/<td>/g)).toHaveLength(20);
    });
    it('敏感参数不回填或进入预设，未知参数不恢复，恢复值按最新定义校验', () => {
        const definitions: ParameterDefinition[] = [{name: 'count', type: 'NUMBER', min: 1, max: 10, defaultValue: 3},
            {name: 'token', type: 'STRING', sensitive: true, defaultValue: 'secret'}];
        expect(defaultParameterValues(definitions)).toEqual({count: '3', token: ''});
        expect(safeParameterValues(definitions, {count: 20, token: 'secret', unknown: true})).toEqual({count: '20'});
        expect(parameterError(definitions[0], '20')).toContain('不能大于');
    });
});
