# 工具导入文档 V1

## 文档结构

只接受本文列出的字段；未知字段会被拒绝。完整且可直接导入的唯一黄金样例见
[example-import-v1.json](example-import-v1.json)。生成前读取该样例，不复制维护第二份示例。

## 字段约束

- `format` 固定为 `maintain-console.script-import`，`version` 固定为 `1`。
- `script` 只包含 `name`、`description`、`content`、`parameterSchema`、`toolMetadata`。
- 工具名称最多 200 字符；说明、风险说明和使用示例分别最多 4000 字符；脚本最多 1 MiB；参数 Schema 序列化后最多 256 KiB。
- 参数 Schema 固定为 `version: 1`，参数类型只能是 `STRING`、`NUMBER`、`BOOLEAN`、`ENUM`、`JSON`、`MULTILINE`、`DATETIME`、`SERVICE_INSTANCE`。
- 参数可使用 `name`、`label`、`group`、`advanced`、`type`、`required`、`defaultValue`、`description`、`example`、`options`、`pattern`、`min`、`max`、`sensitive`。
- `ENUM` 提供非空 `options`；`min` 不大于 `max`；默认值符合类型、枚举和范围约束。
- `$${name}` 是数据表达式，不在脚本中额外添加引号。脚本占位符集合与参数名称集合必须完全相同。
- `sensitive: true` 的参数不包含 `defaultValue`，说明和示例也不包含真实秘密。
- `operationType` 只能是 `QUERY`、`OPERATION`、`UNSPECIFIED`；`OPERATION` 必须填写非空 `riskNote`。`QUERY` 是用途声明，不是代码只读证明。

## 可移植边界

导入文档不包含应用、目录、允许环境、目标实例、工具 ID、版本、创建人、运行参数值及 READ/EDIT/INVOKE/MANAGE 授权。Maintain Console 在导入时选择目标，并创建属于导入者的全新私有工具；导入不会覆盖或执行工具。
