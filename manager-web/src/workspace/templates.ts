export const TOOL_TEMPLATES = {
    table: {
        name: '参数与表格入门', description: '根据姓名和数量生成示例表格，不访问业务数据。',
        content: [
            '// 参数作为数据表达式引用，不要额外添加引号',
            'def name = $${name}', 'def count = $${count}',
            "_log.info('开始生成示例表格')",
            "def rows = (1..count).collect { index -> [index, 'Hello, ' + name] }",
            "return resultTable('问候示例', ['序号', '内容'], rows)",
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
    dashboard: {
        name: '指标、趋势与明细', description: '用固定示例数据展示多个结构化结果，不调用业务服务。',
        content: [
            "_log.info('正在组装固定示例结果')",
            "def metrics = [成功: 18, 失败: 2, 平均耗时毫秒: 126]",
            "def labels = ['09:00', '10:00', '11:00', '12:00']",
            "def series = [[name: '成功数', data: [3, 5, 4, 6]], [name: '失败数', data: [0, 1, 0, 1]]]",
            "def rows = [['demo-1', 'SUCCESS'], ['demo-2', 'FAILED']]",
            "return result(",
            "    resultMetric('运行摘要', metrics),",
            "    resultChart('处理趋势', 'line', labels, series),",
            "    resultTable('示例明细', ['编号', '状态'], rows)",
            ")",
        ].join('\n'),
        schema: JSON.stringify({version: 1, parameters: []}, null, 2),
    },
    json: {
        name: 'JSON 与文本', description: '展示普通对象和说明文字的返回方式。',
        content: [
            "def data = [status: 'READY', caller: _caller.employeeNo, items: [[id: 'demo-1', value: 42]]]",
            "return result(resultText('说明', '这是固定示例数据'), [type: 'json', title: '对象结果', data: data])",
        ].join('\n'),
        schema: JSON.stringify({version: 1, parameters: []}, null, 2),
    },
    empty: {
        name: '空白脚本', description: '填写这个脚本的用途、输入要求和风险。',
        content: "// _caller 来自可信登录身份；业务数据范围仍需由业务能力校验\nreturn resultText('结果', 'Hello, Maintain Console')",
        schema: JSON.stringify({version: 1, parameters: []}, null, 2),
    },
} as const;
