import {
    autocompletion,
    closeBrackets,
    closeBracketsKeymap,
    completionKeymap,
    snippet,
    startCompletion,
} from '@codemirror/autocomplete';
import {defaultKeymap, history, historyKeymap, indentWithTab} from '@codemirror/commands';
import {bracketMatching, defaultHighlightStyle, indentOnInput, syntaxHighlighting} from '@codemirror/language';
import {java} from '@codemirror/lang-java';
import {forceLinting, linter, lintGutter, lintKeymap} from '@codemirror/lint';
import {Compartment, EditorState} from '@codemirror/state';
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
import {SCRIPT_SNIPPETS, scriptCompletions, scriptDiagnostics} from './editorSupport';
import type {RuntimeMetadata} from './types';

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
    const readOnly = useRef(new Compartment());
    const assistance = useRef(new Compartment());
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
                    assistance.current.of([]),
                    keymap.of([...closeBracketsKeymap, ...defaultKeymap, ...historyKeymap, ...completionKeymap,
                        ...lintKeymap, indentWithTab]),
                    readOnly.current.of([]),
                    EditorView.cspNonce.of(document.querySelector<HTMLMetaElement>('meta[name="csp-nonce"]')?.content || ''),
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
    }, []);

    // 原位更新扩展，不重建视图；保留选择区、撤销历史和滚动位置。
    useEffect(() => {
        editor.current?.dispatch({
            effects: readOnly.current.reconfigure([
                EditorState.readOnly.of(disabled), EditorView.editable.of(!disabled),
            ])
        });
    }, [disabled]);

    useEffect(() => {
        const view = editor.current;
        if (!view) return;
        view.dispatch({
            effects: assistance.current.reconfigure([
                linter((current) => scriptDiagnostics(current.state.doc.toString(), parameterNames)),
                autocompletion({override: [(context) => scriptCompletions(context, parameterNames, runtimeMetadata)]}),
            ])
        });
        forceLinting(view);
    }, [parameterNames.join('\u0000'), runtimeMetadata]);

    useEffect(() => {
        const view = editor.current;
        if (!view || view.state.doc.toString() === value) return;
        view.dispatch({changes: {from: 0, to: view.state.doc.length, insert: value}});
    }, [value]);

    return <div className="editor-workbench">
        <div className="editor-toolbar">
            <span>Groovy <small>· {value.split('\n').length} 行</small></span>
            <button type="button" disabled={disabled} onClick={() => {
                if (!editor.current) return;
                editor.current.focus();
                startCompletion(editor.current);
            }}>代码补全 <kbd>Ctrl Space</kbd></button>
        </div>
        <div className="code-editor" ref={host}/>
        <div className="editor-insertions">
            <span>插入片段</span>
            {SCRIPT_SNIPPETS.map((item) => <button key={item.label} type="button" disabled={disabled}
                                                   onClick={() => {
                                                       const view = editor.current;
                                                       if (!view) return;
                                                       const {from, to} = view.state.selection.main;
                                                       snippet(item.template)(view, null, from, to);
                                                       view.focus();
                                                   }}>{item.label}</button>)}
            {parameterNames.length > 0 && <span>参数引用</span>}
            {parameterNames.map((name) => <button key={name} type="button" disabled={disabled}
                                                  title="在光标处插入参数，不需要额外加引号" onClick={() => {
                const view = editor.current;
                if (!view) return;
                view.dispatch(view.state.replaceSelection('$${' + name + '}'));
                view.focus();
            }}><code>{'$${' + name + '}'}</code></button>)}
        </div>
        <div className="editor-status">
            <span>输入自动提示 · Enter 选中 · Tab 切换片段字段 · Esc 关闭</span>
            <span>{runtimeMetadata ? `已连接 · ${runtimeMetadata.beans.length} 个可用 Bean` : '运行时提示未连接，内置补全可用'}</span>
        </div>
    </div>;
}
