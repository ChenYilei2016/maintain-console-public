# 工具台重构实施记录

## 决策

采用 Matt Pocock 的深模块设计：比较了双入口、类型化命令、运行表单中心三种 Interface，选择 `runSaved / debugDraft`
双入口，共享一次性执行核心。工具运行页只消费最小工具信息，不接收源码、完整授权或敏感默认值。

权限与内容保存分离；编辑和恢复必须提交起始版本，恢复不回滚授权。新权限配置显式使用 v2，空名单没有授权；v1
的公开脚本空阅读名单保留兼容，私有脚本按显式授权发现。旧原样参数只保留开发兼容，类型化占位符必须位于独立表达式中。

执行结果用枚举区分成功、明确失败、结果未知、未开始；超时不宣称远端已终止。禁用自动重试，继续使用受控执行资源、签名与防重放。

## 任务／审批退出清单

已断开运行入口、Spring Bean、后台调度和前端订阅。用户两次确认后，以下 21 个 Git 已跟踪文件已删除，可从版本记录恢复。

后端路径均相对于 `manager/src/main/java/io/github/chenyilei2016/maintain/manager`：

- `controller/manager/ScriptExecutionTaskController.java`
- `controller/manager/ExecutionApprovalController.java`
- `controller/dto/ExecutionTaskCreateWebRequest.java`
- `controller/dto/ExecutionApprovalCreateWebRequest.java`
- `controller/dto/ExecutionApprovalDecisionWebRequest.java`
- `service/ScriptExecutionTaskService.java`
- `service/ExecutionRequestResolver.java`
- `service/ExecutionApprovalService.java`
- `service/ExecutionApprovalBinding.java`
- `pojo/entity/ScriptExecutionTask.java`
- `pojo/entity/ScriptExecutionTaskStatus.java`
- `pojo/entity/ScriptExecutionTargetResult.java`
- `pojo/entity/ExecutionApproval.java`
- `pojo/entity/ExecutionApprovalStatus.java`
- `pojo/dataobject/ScriptExecutionTaskDO.java`
- `pojo/dataobject/ExecutionApprovalDO.java`
- `pojo/mapper/ScriptExecutionTaskMapper.java`
- `pojo/mapper/ExecutionApprovalMapper.java`

对应旧测试与展示文件：

- `manager/src/test/java/io/github/chenyilei2016/maintain/manager/pojo/entity/ScriptExecutionTaskTest.java`
- `manager/src/test/java/io/github/chenyilei2016/maintain/manager/service/ExecutionApprovalBindingTest.java`
- `manager-web/src/ExecutionTaskPanel.tsx`

保留：V1–V5 历史迁移、任务和审批历史表、执行历史、审计、Client 签名、防重放、Groovy 执行器。现有 SQLite 及 WAL/SHM 不修改。本次仅新增
V6，使用独立测试数据库验证。

## 验收路径

1. 作者创建私有工具、配置类型化参数与用途、保存版本。
2. 作者授权仅执行用户与允许环境，复制入口；链接不自动运行。
3. 接收者只能填表运行保存版本，无法通过详情、旧接口、历史、版本或参数注入越权。
4. 编辑者可保存，不能更改授权；版本冲突拒绝覆盖；撤销权限后新请求拒绝。
5. 超时／断网只报告未知，不重试；历史不承担后台任务职责。
6. 工具首页、工作台、运行页和嵌入 JAR 直达／刷新，多参数与窄屏验证。

此文件记录实施中的设计和待确认清理范围；完成情况以最终测试结果及 README 为准。
