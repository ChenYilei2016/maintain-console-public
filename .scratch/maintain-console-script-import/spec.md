# 支持由 Codex Skill 生成并导入工具 JSON

Status: ready-for-agent

## Problem Statement

当前制作 Maintain Console 工具时，作者需要分别编写 Groovy 脚本、参数定义、用途说明和操作类型。即使使用 Codex 辅助生成，也缺少一个稳定、可校验、可直接导入的交换契约，仍需要手工复制多个字段，容易产生参数定义与脚本占位符不一致、用途和风险缺失等问题。

脚本工作台也缺少统一的“导入 JSON”能力。用户无法从脚本资源区或正在编辑工具的上下文中快速导入生成结果，只能重新走手工创建流程。外部生成内容如果携带应用、目录、环境或权限，还可能越过 Maintain Console 对工具位置与脚本授权的现有边界。

## Solution

在仓库中新增 `maintain-console-script-author` Skill。Skill 根据用户描述的运维意图生成版本化的“工具导入文档”，内容包括脚本、参数定义、说明和操作类型。Skill 暂不安装到个人 Codex，只以仓库内容作为协议事实来源。

脚本工作台新增一个可复用的导入面板，并提供两个入口：

- 脚本资源区的“导入”入口，面向从目录开始创建工具的用户。
- 工具编辑区域的“导入 JSON”入口，面向正在编写工具、希望就近导入的用户。

两个入口使用同一个导入流程。用户粘贴 JSON 或选择 JSON 文件后，工作台即时校验并展示预览，再由用户选择所属应用、目录和允许环境。导入始终创建新的私有工具，不覆盖当前工具、不导入脚本授权、不自动执行；同名时阻止创建并要求用户在预览中修改名称。

## User Stories

1. As a 工具作者, I want to describe an operations goal to a repository Skill, so that I can obtain a Maintain Console compatible tool without manually assembling multiple fields.
2. As a 工具作者, I want the Skill to generate one versioned JSON document, so that I can copy one artifact into the script workbench.
3. As a 工具作者, I want the generated document to include Groovy content and parameter definitions, so that the imported tool is immediately editable.
4. As a 工具作者, I want the generated document to include description, operation type, risk note and usage example, so that other users understand its purpose before running it.
5. As a 工具作者, I want the Skill to use the existing parameter types and placeholder syntax, so that generated content follows the platform contract.
6. As a 工具作者, I want the Skill to ask for missing Bean names, method signatures and return structures, so that it does not invent non-runnable business calls.
7. As a 工具作者, I want the Skill to use confirmed built-in result helpers for demonstrations, so that generated tools can demonstrate text, metric, table and chart results safely.
8. As a 工具作者, I want sensitive parameters to omit real defaults and credentials, so that secrets are not embedded in an import document.
9. As a 登录用户, I want to open import from the script resource area, so that I can create a tool directly in the directory workflow.
10. As a 工具作者, I want an import entry inside the tool editing area, so that I do not need to leave the current work context.
11. As a 工具作者, I want both import entries to open the same interface, so that behavior and validation remain consistent.
12. As a 工具作者, I want the editor entry to default to the current tool's application and parent directory, so that common placement requires fewer selections.
13. As a 工具作者, I want opening the import panel to leave my current 草稿 unchanged, so that exploring an import cannot destroy unsaved work.
14. As a 登录用户, I want to paste JSON, so that I can transfer output directly from Codex.
15. As a 登录用户, I want to choose a local JSON file, so that saved import documents are equally easy to use.
16. As a 登录用户, I want invalid JSON and unsupported versions reported immediately, so that I can correct the source before creating anything.
17. As a 登录用户, I want validation errors to identify the affected field or parameter, so that I can understand and fix the problem.
18. As a 登录用户, I want a preview of the tool name, purpose and operation type, so that I understand what will be created.
19. As a 登录用户, I want a summary of parameter count, required parameters and sensitive parameters, so that I can review the generated form contract.
20. As a 登录用户, I want source content and raw parameter definitions available in a collapsed review area, so that I can inspect details without overwhelming the primary flow.
21. As a 登录用户, I want to choose the target application, directory and allowed environments during import, so that deployment placement remains under my control.
22. As a 登录用户, I want a clear notice that importing creates a private tool, so that I understand its initial visibility.
23. As a 登录用户, I want importing to exclude all script authorization assignments, so that external JSON cannot grant READ, EDIT, INVOKE or MANAGE capabilities.
24. As a 登录用户, I want importing to exclude IDs, creator information and versions, so that imported content cannot impersonate an existing tool.
25. As a 登录用户, I want importing to create a new tool rather than update the current one, so that existing saved tools cannot be overwritten accidentally.
26. As a 登录用户, I want a name conflict to block creation and keep the preview open, so that I can choose an intentional name.
27. As a 登录用户, I want a successful import to open the newly created tool in the script workbench, so that I can review and continue editing immediately.
28. As a 登录用户, I want importing never to execute or debug the tool automatically, so that reviewing generated code does not trigger remote operations.
29. As a 安全审查者, I want QUERY to remain a purpose declaration rather than a read-only guarantee, so that the interface does not overstate safety.
30. As a 维护者, I want imported tools to use the existing creation, version history, audit and private authorization path, so that import does not create a parallel persistence model.
31. As a 维护者, I want one parser and one reusable import panel behind both entry points, so that future fixes remain local.
32. As a 维护者, I want unsupported fields rejected rather than silently ignored, so that contract typos and privilege-bearing fields are visible.
33. As a 维护者, I want the repository Skill and workbench importer to share one golden V1 example, so that the generation contract and product behavior do not drift.
34. As a 未来集成者, I want the import format to be explicitly versioned, so that a later compatible version can be introduced without guessing document semantics.

## Implementation Decisions

- 引入领域术语“工具导入文档”表示可移植 JSON 契约。产品界面可以显示“导入 JSON”，实现命名使用 `ScriptImportDocument`，避免使用含义模糊的 `ImportJson`。
- V1 文档由固定格式标识、格式版本和单个工具载荷组成。
- 工具载荷包含名称、说明、Groovy 内容、参数定义对象和工具元数据。
- 参数定义在导入文档中保持为对象，映射到现有工具创建请求时仅序列化一次。
- 参数定义版本与外层导入格式版本相互独立。
- 参数名称集合必须与脚本占位符集合完全一致，现有占位符位置规则继续作为权威规则。
- 参数类型继续限定为 `STRING`、`NUMBER`、`BOOLEAN`、`ENUM`、`JSON`、`MULTILINE`、`DATETIME` 和 `SERVICE_INSTANCE`。
- 工具元数据继续使用 `QUERY`、`OPERATION` 或 `UNSPECIFIED`。`OPERATION` 必须提供非空风险说明；`QUERY` 只是用途声明，不构成代码只读证明。
- 敏感参数不得包含默认值；Skill 不得在示例或说明中输出真实秘密。
- 导入文档不得包含工具 ID、已保存版本、创建人、操作人、目标实例、应用位置、目录位置、允许环境或脚本授权。
- 应用、父目录和允许环境由用户在导入面板中选择，不消费生成 JSON 中的目标建议。
- V1 仅创建新的私有工具，不更新、合并或覆盖现有工具。
- 同名冲突在原导入面板中反馈；用户修改名称时保留文档内容和目标选择。
- 脚本资源区入口和工具编辑区入口渲染同一个导入面板。
- 工具编辑区入口默认使用当前工具的应用和父目录，但不修改当前工具及其草稿。
- 导入面板采用单页交互，不使用多步骤向导；同一页面展示文档输入、决策型预览、目标位置和一个固定主操作。
- 主操作命名为“创建为私有工具并打开”，并明确说明不会执行工具或导入授权。
- 粘贴使用现有文本输入能力；文件选择使用浏览器原生 JSON 文件选择，不引入上传或拖拽依赖。
- 前端校验 JSON 语法、格式和版本、字段白名单、长度、枚举、敏感默认值及面向预览的参数问题。
- 现有服务端参数定义和脚本占位符校验继续作为最终权威信任边界。
- 创建复用现有目录树节点保存请求及服务；V1 不新增导入 Controller、持久化服务、数据表或授权实现。
- 仓库保存 `maintain-console-script-author` Skill 源码及精简的 V1、运行时参考；本次不安装到个人 Codex Skill 目录。
- 业务 Bean 或 API 详情无法从用户输入或仓库代码中确认时，Skill 停止生成，只询问缺失的 Bean 名称、方法签名、参数、返回结构和操作语义。
- Skill 完成后的输出只有一个合法 JSON 代码块，可以直接粘贴到脚本工作台。
- 格式标识、格式版本、操作类型和校验码等关键值使用命名常量或既有枚举，不散落魔法字符串。
- Skill 文档和导入器测试尽可能共用一个 V1 黄金样例，不引入代码生成或 JSON Schema 框架。

## Testing Decisions

- 主要测试 seam 是脚本工作台导入流程：从任一入口提供工具导入文档，观察预览，选择目标，提交后验证既有工具创建请求，并观察新工具导航。
- 测试只断言用户可观察行为，不依赖组件内部状态、私有方法或实现结构。
- 复用现有工作台操作、API stub、参数定义校验和导航测试方式。
- 合法的粘贴 V1 文档必须展示决策型预览，并正确映射到现有创建请求，不得对整个文档进行双重编码。
- 选择 JSON 文件必须得到与粘贴文本相同的预览和创建请求。
- 两个入口必须打开同一导入行为；工具编辑区入口默认当前应用和父目录。
- 从工具编辑区打开并取消导入，不得改变当前草稿。
- 非法 JSON、不支持的格式或版本、未知字段及权限、身份、位置等禁止字段必须阻止创建，并指出相关位置。
- 参数定义和占位符不一致时，发送任何保存请求前必须阻止创建。
- 枚举选项、数字边界、非法正则和不支持的参数类型继续通过既有参数校验行为反馈。
- 敏感参数包含默认值时必须拒绝导入。
- `OPERATION` 缺少风险说明时必须拒绝；`QUERY` 仍须展示现有的非安全保证说明。
- 服务端返回同名冲突或目标无效时，导入面板和用户选择必须保留。
- 创建请求不得包含导入文档中的权限、ID、版本、创建人或目标实例。
- 导入成功必须通过现有后端路径创建私有工具，且不得调用任何执行或草稿调试接口。
- 既有后端创建 seam 继续覆盖私有授权、版本历史及权威参数定义/占位符校验。
- V1 黄金样例必须始终能被导入器接受，作为 Skill 契约与产品行为之间的低成本防漂移检查。

## Out of Scope

- 将 Skill 安装到用户个人 Codex Skill 目录。
- 批量或多工具导入。
- 更新、合并或覆盖已有工具。
- 导入 READ、EDIT、INVOKE 或 MANAGE 分配。
- 将应用、目录、允许环境或目标实例作为权威导入内容。
- 导入完成后自动执行、草稿调试或预览执行。
- YAML、ZIP、远程 URL 及依赖剪贴板权限的导入方式。
- 独立后端导入 API、CLI 或第三方集成接口。
- 运行时 Bean 依赖清单及目标环境自动兼容性检查。
- 将现有工具导出为相同格式。
- 定时自动化、审批流、包签名、校验和或包仓库。
- 引入 JSON Schema 框架、代码生成器、表单库或文件上传依赖。

## Further Notes

- 两个 UI 入口刻意收敛到一个深模块和一个既有持久化 seam。
- 仓库 Skill 是生成指南；脚本工作台导入器和服务端是校验与执行边界。
- 如果 CLI、批量迁移或外部系统集成成为真实需求，再将 `ScriptImportDocument` 提升为服务端正式 DTO，并新增专用导入接口。
- 如果未来需要往返可移植性，再使用同一 V1 可移植字段实现导出，并继续排除 ID、目标位置和脚本授权。
