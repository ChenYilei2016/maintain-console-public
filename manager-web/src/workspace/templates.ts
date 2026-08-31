export const TOOL_TEMPLATES = {
    table: {
        name: '参数与表格入门', description: '根据姓名和数量生成示例表格，不访问业务数据。',
        content: [
            '// 参数作为数据表达式引用，不要额外添加引号',
            'def name = $${name}', 'def count = $${count}',
            "_log.info('开始生成示例表格')",
            "def rows = (1..count).collect { index -> [index, 'Hello, ' + name] }",
            "return result(resultTable('问候示例', ['序号', '内容'], rows))",
        ].join('\n'),
        schema: JSON.stringify({
            version: 1, parameters: [
                {
                    name: 'name',
                    label: '称呼',
                    type: 'STRING',
                    required: true,
                    defaultValue: 'Maintain Console',
                    description: '希望向谁问好'
                },
                {
                    name: 'count',
                    label: '生成行数',
                    type: 'NUMBER',
                    required: true,
                    defaultValue: 3,
                    min: 1,
                    max: 20,
                    description: '示例表格的数据量'
                },
            ]
        }, null, 2),
    },
    empty: {
        name: '空白工具', description: '填写这个工具的用途、输入要求和风险。',
        content: "// _caller 来自可信登录身份；业务数据范围仍需由业务能力校验\nreturn result(resultText('结果', 'Hello, Maintain Console'))",
        schema: JSON.stringify({version: 1, parameters: []}, null, 2),
    },
} as const;
