import {
    type Completion,
    type CompletionContext,
    type CompletionResult,
    snippetCompletion
} from '@codemirror/autocomplete';
import type {Diagnostic} from '@codemirror/lint';
import type {RuntimeMetadata} from './types';

export const SCRIPT_SNIPPETS = [
    {label: '日志', template: "_log.info('${message}')", completion: 'logInfo', detail: '输出运行日志'},
    {
        label: '文本结果',
        template: "return result(resultText('${title}', ${value}))",
        completion: 'textResult',
        detail: '返回文本区块'
    },
    {
        label: '表格结果',
        template: "return result(resultTable('${title}', ['${column}'], [[${value}]]))",
        completion: 'tableResult',
        detail: '返回表格区块'
    },
];

const BUILT_IN_COMPLETIONS: Completion[] = [
    {label: 'ctx', type: 'variable', detail: '受控运行时上下文'},
    {label: '_log', type: 'variable', detail: '日志：info / warn / error / debug'},
    {label: '_caller', type: 'variable', detail: '可信调用者身份，由 Manager 注入；不能用表单身份代替授权'},
    ...[
        ['toJson', 'toJson(${value})', '对象转换为 JSON 字符串'],
        ['result', 'result(${blocks})', '组合一个或多个结果区块'],
        ['resultText', "resultText('${title}', ${value})", '文本结果'],
        ['resultTable', "resultTable('${title}', ${columns}, ${rows})", '表格：列名列表、行列表'],
        ['resultMetric', "resultMetric('${title}', [${name}: ${value}])", '指标：名称与数值'],
        ['resultChart', "resultChart('${title}', '${line}', ${labels}, ${series})", '图表：类型、标签、数据系列'],
        ['resultFileContent', "resultFileContent('${title}', '${report.csv}', ${content}.bytes, '${text/csv}')", '下载文件，最多 1 MiB'],
    ].map(([label, template, detail]) => snippetCompletion(template, {label, detail, type: 'function'})),
    ...SCRIPT_SNIPPETS.map((item) => snippetCompletion(item.template, {
        label: item.completion,
        detail: item.detail,
        type: 'text'
    })),
    ...['def', 'return', 'if', 'else', 'for', 'in', 'new', 'try', 'catch', 'throw', 'true', 'false', 'null', 'import']
        .map((label) => ({label, type: 'keyword'})),
];

/** 与替换协议一致：参数引用整体替换，不能只替换大括号后的半个单词。 */
export function scriptCompletions(context: CompletionContext, parameterNames: string[], metadata?: RuntimeMetadata): CompletionResult | null {
    const parameter = context.matchBefore(/\$\$\{[^{}\n]*/);
    if (parameter) {
        const hasClosingBrace = context.state.sliceDoc(context.pos, context.pos + 1) === '}';
        return {
            from: parameter.from, to: context.pos + (hasClosingBrace ? 1 : 0),
            options: parameterNames.map((name) => ({
                label: '$${' + name + '}',
                type: 'variable',
                detail: '执行时从参数表单填入'
            })),
            validFor: /\$\$\{[^{}\n]*/,
        };
    }
    const beanMember = context.matchBefore(/ctx\.getBean\(['"][^'"]+['"]\)\.\w*/);
    if (beanMember) {
        const beanName = /getBean\(['"]([^'"]+)['"]\)/.exec(beanMember.text)?.[1];
        const bean = metadata?.beans.find((item) => item.name === beanName);
        return {
            from: beanMember.from + beanMember.text.lastIndexOf('.') + 1,
            options: (bean?.methods || []).map((method) => ({
                label: method, type: 'method',
                apply: method.replace(/\(.*/, '') + '()', detail: bean?.type
            })),
        };
    }
    const member = context.matchBefore(/(?:_caller|_log|ctx)\.\w*/);
    if (member) {
        const receiver = member.text.split('.')[0];
        if (receiver === '_caller') return {
            from: member.from + receiver.length + 1,
            options: ['employeeNo', 'employeeName'].map(label => ({
                label,
                type: 'property',
                detail: '服务端可信登录身份'
            }))
        };
        return {
            from: member.from + receiver.length + 1,
            options: receiver === '_log'
                ? ['info', 'warn', 'error', 'debug'].map((label) => snippetCompletion(label + "('${message}')", {
                    label,
                    type: 'method'
                }))
                : [snippetCompletion("getBean('${beanName}')", {
                    label: 'getBean',
                    type: 'method',
                    detail: '仅限客户端白名单 Bean'
                }),
                    ...(metadata?.beans || []).map((bean) => ({
                        label: `getBean('${bean.name}')`, type: 'method', detail: bean.type,
                    }))],
        };
    }
    const word = context.matchBefore(/[\w$]*/);
    if (!word || word.from === word.to && !context.explicit) return null;
    return {
        from: word.from,
        options: [
            ...BUILT_IN_COMPLETIONS,
            ...parameterNames.map((name) => ({label: '$${' + name + '}', type: 'variable', detail: '脚本参数'})),
            ...(metadata?.beans.flatMap((bean) => [
                {label: bean.name, apply: `ctx.getBean('${bean.name}')`, type: 'variable', detail: bean.type},
                ...bean.methods.map((method) => ({
                    label: `${bean.name}.${method}`, type: 'method', detail: bean.type,
                    apply: `ctx.getBean('${bean.name}').${method.replace(/\(.*/, '')}()`,
                })),
            ]) || []),
        ],
    };
}

const RISK_PATTERNS: Array<[RegExp, string]> = [
    [/\bSystem\.exit\s*\(/g, 'System.exit 可终止目标应用'],
    [/\bRuntime\.getRuntime\s*\(/g, '调用 Runtime 属于高风险操作'],
    [/\b(?:new\s+)?File\s*\(/g, '文件系统访问需要额外复核'],
    [/\bClass\.forName\s*\(/g, '反射加载类需要额外复核'],
    [/\bexecute(?:Update|LargeUpdate)\s*\(/g, '脚本包含数据写操作'],
];

export function scriptDiagnostics(source: string, parameterNames: string[]): Diagnostic[] {
    const diagnostics: Diagnostic[] = [];
    for (const [pattern, message] of RISK_PATTERNS) {
        for (const match of source.matchAll(pattern)) {
            diagnostics.push({from: match.index, to: match.index + match[0].length, severity: 'warning', message});
        }
    }
    for (const match of source.matchAll(/\$\$\{\s*([^}]+?)\s*}/g)) {
        if (!parameterNames.includes(match[1].trim())) {
            diagnostics.push({
                from: match.index, to: match.index + match[0].length, severity: 'error',
                message: `参数 ${match[1].trim()} 尚未配置，请在“配置参数”中从脚本识别`
            });
        }
    }
    return diagnostics;
}
