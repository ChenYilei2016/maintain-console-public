import {
    autocompletion,
    closeBrackets,
    closeBracketsKeymap,
    type CompletionContext,
    completionKeymap
} from '@codemirror/autocomplete';
import {defaultKeymap, history, historyKeymap, indentWithTab} from '@codemirror/commands';
import {bracketMatching, defaultHighlightStyle, indentOnInput, syntaxHighlighting} from '@codemirror/language';
import {java} from '@codemirror/lang-java';
import {type Diagnostic, linter, lintGutter, lintKeymap} from '@codemirror/lint';
import {EditorState} from '@codemirror/state';
import {
    crosshairCursor,
    drawSelection,
    dropCursor,
    EditorView,
    highlightActiveLine,
    highlightActiveLineGutter,
    highlightSpecialChars,
    keymap,
    lineNumbers,
    rectangularSelection
} from '@codemirror/view';
import {oneDark} from '@codemirror/theme-one-dark';
import {useEffect, useRef} from 'react';
import type {RuntimeMetadata} from './types';

const BUILT_IN_COMPLETIONS = [
    {label: 'ctx', type: 'variable', detail: '受控运行时上下文'},
    {label: '_log', type: 'variable', detail: '执行日志输出'},
    {label: 'toJson', type: 'function', apply: 'toJson(value)', detail: '转换为 JSON 字符串'},
    {label: 'result', type: 'function', apply: 'result(resultText("title", value))', detail: '组合结构化结果区块'},
    {label: 'resultText', type: 'function', apply: 'resultText("title", value)'},
    {label: 'resultTable', type: 'function', apply: 'resultTable("title", columns, rows)'},
    {label: 'resultMetric', type: 'function', apply: 'resultMetric("title", values)'},
    {label: 'resultChart', type: 'function', apply: 'resultChart("title", "line", labels, series)'},
    {
        label: 'resultFileContent', type: 'function',
        apply: 'resultFileContent("title", "report.csv", content.bytes, "text/csv")',
        detail: '生成不超过 1 MiB 的内联下载文件'
    },
];

const RISK_PATTERNS: Array<[RegExp, string]> = [
    [/\bSystem\.exit\s*\(/g, 'System.exit 可终止目标应用'],
    [/\bRuntime\.getRuntime\s*\(/g, '调用 Runtime 属于高风险操作'],
    [/\b(?:new\s+)?File\s*\(/g, '文件系统访问需要额外复核'],
    [/\bClass\.forName\s*\(/g, '反射加载类需要额外复核'],
    [/\bexecute(?:Update|LargeUpdate)\s*\(/g, '脚本包含数据写操作'],
];

function completionSource(parameterNames: string[], runtimeMetadata?: RuntimeMetadata) {
    return (context: CompletionContext) => {
        const word = context.matchBefore(/[\w$]*/);
        if (!word || (word.from === word.to && !context.explicit)) return null;
        return {
            from: word.from,
            options: [
                ...BUILT_IN_COMPLETIONS,
                ...parameterNames.map((name) => ({
                    label: '$${' + name + '}',
                    type: 'variable',
                    apply: '$${' + name + '}',
                    detail: '类型化脚本参数',
                })),
                ...(runtimeMetadata?.beans.flatMap((bean) => [
                    {
                        label: `ctx.getBean('${bean.name}')`,
                        type: 'variable',
                        apply: `ctx.getBean('${bean.name}')`,
                        detail: bean.type,
                    },
                    ...bean.methods.map((method) => ({
                        label: `${bean.name}.${method}`,
                        type: 'method',
                        apply: `ctx.getBean('${bean.name}').${method.replace(/\(.*/, '')}()`,
                        detail: bean.type,
                    })),
                ]) || []),
            ],
        };
    };
}

function scriptLinter(parameterNames: string[]) {
    return linter((view) => {
        const source = view.state.doc.toString();
        const diagnostics: Diagnostic[] = [];
        for (const [pattern, message] of RISK_PATTERNS) {
            for (const match of source.matchAll(pattern)) {
                diagnostics.push({from: match.index, to: match.index + match[0].length, severity: 'warning', message});
            }
        }
        for (const match of source.matchAll(/\$\$\{\s*([^}]+?)\s*}/g)) {
            const parameterName = match[1].trim();
            if (!parameterNames.includes(parameterName)) {
                diagnostics.push({
                    from: match.index,
                    to: match.index + match[0].length,
                    severity: 'error',
                    message: `参数 ${parameterName} 未在 Schema 中声明`,
                });
            }
        }
        return diagnostics;
    });
}

export default function CodeEditor({
                                       value,
                                       disabled,
                                       parameterNames,
                                       runtimeMetadata,
                                       onChange,
                                   }: {
    value: string;
    disabled: boolean;
    parameterNames: string[];
    runtimeMetadata?: RuntimeMetadata;
    onChange: (value: string) => void;
}) {
    const host = useRef<HTMLDivElement>(null);
    const editor = useRef<EditorView | null>(null);
    const onChangeRef = useRef(onChange);
    onChangeRef.current = onChange;

    useEffect(() => {
        if (!host.current) return;
        const view = new EditorView({
            parent: host.current,
            state: EditorState.create({
                doc: value,
                extensions: [
                    lineNumbers(), highlightActiveLineGutter(), highlightSpecialChars(), history(), drawSelection(),
                    dropCursor(), EditorState.allowMultipleSelections.of(true), indentOnInput(), bracketMatching(),
                    closeBrackets(), rectangularSelection(), crosshairCursor(), highlightActiveLine(),
                    syntaxHighlighting(defaultHighlightStyle, {fallback: true}), java(), oneDark, lintGutter(),
                    scriptLinter(parameterNames), autocompletion({override: [completionSource(parameterNames, runtimeMetadata)]}),
                    keymap.of([...closeBracketsKeymap, ...defaultKeymap, ...historyKeymap, ...completionKeymap,
                        ...lintKeymap, indentWithTab]),
                    EditorState.readOnly.of(disabled), EditorView.editable.of(!disabled),
                    EditorView.contentAttributes.of({'aria-label': 'Groovy 脚本内容'}),
                    EditorView.updateListener.of((update) => {
                        if (update.docChanged) onChangeRef.current(update.state.doc.toString());
                    }),
                ],
            }),
        });
        editor.current = view;
        return () => {
            view.destroy();
            editor.current = null;
        };
    }, [disabled, parameterNames.join('\u0000'), JSON.stringify(runtimeMetadata)]);

    useEffect(() => {
        const view = editor.current;
        if (!view || view.state.doc.toString() === value) return;
        view.dispatch({changes: {from: 0, to: view.state.doc.length, insert: value}});
    }, [value]);

    return <div className="code-editor" ref={host}/>;
}
