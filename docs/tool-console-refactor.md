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

## 实施与验证结果

前端页面与状态按 `tools / workspace / execution / results` 归属，不再由 App 代管保存、目录和执行状态；共享表单、编辑器和结构化结果继续复用。
新增制作权限校验：只获运行权限的用户不能通过“新建一个自己的脚本”获得任意代码执行能力。开发者名单与全局管理员名单分开。
运行结果状态、目标选择、操作类型和可信角色使用枚举/类型化映射；没有新建通用策略工厂或引入前端依赖。

已运行：

```bash
mvn -pl manager -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false package
cd manager-web
pnpm test
pnpm build
```

- 前端 23 项测试通过，TypeScript 检查与 Vite 构建通过；编译资源写入 `manager/src/main/resources/static/console`，打包 JAR
  的入口 hash 与该目录一致。
- 后端 44 项通过（Manager 37、Client 公共协议 2、Groovy 支持 5），保留的 2 项外部 HTTP
  手工联调测试跳过；包含作者、协作编辑者、仅运行用户、陌生用户的真实业务入口回归。
- 权限回归覆盖创建绕过、私有发现/收藏、源码与版本拒绝、草稿/旧入口拒绝、撤销、保存冲突、恢复不改授权、历史按执行者过滤。
- 执行回归覆盖超时/空响应结果未知且只调用一次、多实例上限、部分成功、原样参数仅开发兼容；类型化回归包含引号/反斜杠/换行/JSON/布尔/未知参数与敏感值脱敏。
- 独立 SQLite V5→V7 升级通过，旧源码、权限与历史数据未改写，任务/审批表仍存在。MySQL 迁移已提供，未在真实 MySQL 实例执行。
- 新增 Client 日志类 `javap -verbose` 显示 `major version: 52`，保持 Java 8 字节码。

浏览器使用最终打包 JAR、`local`、独立临时 SQLite 与端口 10007，AI
关闭。实际完成三身份登录入口、开发者原链接返回、私有工具创建、多脚本 Tab 切换、草稿执行、指标/趋势/表格/日志、管理运行概览及环境脱敏视图。
使用 12 个参数、87 行代码、45 行表格验证参数滚动提示、固定目标/运行入口、表格分页/排序/空筛选；390px
窄屏表单按顺序排列、工作台参数抽屉可用，没有页面横向溢出。编辑器和参数区约 56/44，结果默认展开，用户菜单不遮挡结果操作。
行号与代码对齐；多 Tab 返回后结果仍保留。保存后补全连接恢复的回归已通过。浏览器 local 管理员演示不替代上述多身份后端测试。

## 登录、工作区与多注册中心阶段

- `local/demo` 固定提供 admin、developer、operator 三个经 Provider 校验的 Mock 身份；系统用户、角色、启用状态和会话落在独立边界，V7
  同时提供 SQLite/MySQL 迁移。
- 未登录直达页面保留安全的相对返回地址；admin 默认管理中心，developer 默认开发工作台。生产签名身份入口继续可用，不会降级到
  Mock。
- 工作台首页恢复直接编辑路径，资源树和站内链接使用 History API；最多挂载 5 个脚本会话，按用户隔离恢复索引，不持久化运行值和结果。
- 工具栏按主要操作、回溯、工具设置、开发辅助分组；结果默认展开；示例库增加指标、趋势、明细、JSON、文本与空结果。
- 管理中心包含用户状态/系统角色、7/30/90 天有界运行聚合、环境与连接脱敏视图。个人设置因尚无明确可配置项未建设空页面。
- 多注册中心通过 `SPRING_CLOUD` / `MULTI_NACOS` 枚举切换；连接按环境显式绑定并复用 Nacos Client，不跨环境自动故障转移。

## 兼容与已知边界

- 旧执行调用方必须提交自己已看到的版本；没有 Schema 的原样参数脚本需作者明确迁移后才能分享运行。源码/ACL/历史不批量改写。
- 当前请求没有后台续跑、任务恢复或自动重试；超时、断网、撤销权限都不能保证已经发出的远端操作停止。
- 参数类型和敏感值匹配脱敏不等于 SQL 参数化、数据权限或通用污点追踪；开发者仍需用可信 `_caller` 和业务 API 落实数据范围。
- 版本对比是并排内容查看，不建设自动合并；编辑器补全不是完整 Groovy LSP。过程日志随结果返回，不承诺异常终止时收集完整远端日志。
- 原工程 SQLite/WAL/SHM 及用户运行日志保留；测试使用独立库。所有代码删除均在两次确认的 21 文件清单内，没有删除历史表或原库。
- 生产 IdP 登录后返回、实际 MySQL 升级、真实注册中心与跨进程 Client/Worker 联调需在对应环境验收；本次没有调用生产脚本。
