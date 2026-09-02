import {CompletionContext} from '@codemirror/autocomplete';
import {EditorState} from '@codemirror/state';
import {describe, expect, it} from 'vitest';
import {SCRIPT_SNIPPETS, scriptCompletions, scriptDiagnostics} from './editorSupport';

describe('脚本编辑辅助', () => {
    it('补全完整参数引用，不留下重复的大括号', () => {
        const source = 'def value = $${co}';
        const context = new CompletionContext(EditorState.create({doc: source}), source.length - 1, false);
        const completion = scriptCompletions(context, ['count']);
        expect(completion?.from).toBe(source.indexOf('$$'));
        expect(completion?.to).toBe(source.length);
        const label = completion!.options[0].label;
        expect(source.slice(0, completion!.from) + label + source.slice(completion!.to)).toBe('def value = $${count}');
    });

    it('提供手动补全入口和日志成员提示', () => {
        const empty = scriptCompletions(new CompletionContext(EditorState.create(), 0, true), []);
        expect(empty?.options.map((option) => option.label)).toContain('resultTable');
        const log = scriptCompletions(new CompletionContext(EditorState.create({doc: '_log.'}), 5, false), []);
        expect(log?.from).toBe(5);
        expect(log?.options.map((option) => option.label)).toEqual(['info', 'warn', 'error', 'debug']);
    });

    it('未声明参数和高风险调用都有定位信息', () => {
        const diagnostics = scriptDiagnostics('return $${missing}\nSystem.exit(0)', []);
        expect(diagnostics.map((item) => item.severity)).toEqual(['warning', 'error']);
        expect(scriptDiagnostics('return $${count}', ['count'])).toEqual([]);
    });

    it('在受控 Bean 之后仅补全方法，避免插入重复的 getBean 调用', () => {
        const source = "ctx.getBean('orders').fi";
        const completion = scriptCompletions(new CompletionContext(EditorState.create({doc: source}), source.length, false), [], {
            protocolVersion: 1, beans: [{name: 'orders', type: 'OrderService', methods: ['find(String)']}],
        });
        expect(completion?.from).toBe(source.lastIndexOf('.') + 1);
        expect(completion?.options[0].apply).toBe('find()');
    });

    it('单结果片段直接返回结果块，不添加多余协议包装', () => {
        expect(SCRIPT_SNIPPETS.filter(item => item.label.endsWith('结果')).map(item => item.template)).toEqual([
            "return resultText('${title}', ${value})",
            "return resultTable('${title}', ['${column}'], [[${value}]])",
            "return resultMetric('${title}', [${name}: ${value}])",
        ]);
    });
});
