# Groovy 运行时能力

只使用本页能力和目标应用明确开放的 Bean。不能确认的业务调用先向用户补问。

## 平台变量

- `_caller.employeeNo`、`_caller.employeeName`：服务端注入的可信登录身份。业务数据范围仍由目标 Bean 落实，不能使用表单中的 employeeNo 或 tenantId 代替授权。
- `_log.info/warn/error/debug/trace(...)`：过程日志随结果返回；不是实时日志流。
- `ctx.getBean('beanName')`：访问客户端白名单 Bean。Bean 名称、方法签名和返回结构必须来自代码、运行时元数据或用户提供的可靠契约。

## 结构化结果

- `result(blocks...)`：组合多个结果区块；单个结果块直接返回对应 helper。
- `resultText(title, value)`：说明文字或单值结果。
- `resultMetric(title, map)`：少量关键指标。
- `resultTable(title, columns, rows)`：结构化明细，最多 1000 行。
- `resultChart(title, type, labels, series)`：趋势数据，每组最多 1000 个点；常用类型为 `line`、`bar`、`pie`。
- `resultFileContent(title, filename, bytes, contentType)`：小文件下载，解码后最多 1 MiB。
- `toJson(value)`：将对象转换为 JSON 字符串。

普通字符串和普通 JSON 可以兼容显示，但新工具优先返回能表达操作者决策的结构化结果。完整结果协议上限为 2 MiB。

单结果写法：`return resultText('说明', value)`。多结果写法：
`return result(resultMetric('摘要', metrics), resultTable('明细', columns, rows))`。
执行器会把单个 helper 结果归一化为完整协议；已经返回 `result(...)` 时不会重复包装。
