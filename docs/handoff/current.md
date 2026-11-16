# 当前项目 Handoff

更新于 2026-11-13（Asia/Shanghai）。业务结论以[唯一说明书 V2](../baseline/specificationV2.md)为准。

## 当前阶段与版本

- 当前处于需求分析与需求级架构文档阶段，工程实现及外部接入验证尚待开展。
- M1 场景定义与管理需求完成，具体接入交互待细化。M2 的 §4.2.1—§4.2.3 已经用户确认并写入当前工作分支候选稿，后续流程、节点与接口设计继续讨论。M3—M7 的细化仍待开展。
- 当前任务为 `module4.2_design`，负责人 `BlackWhaleK`，范围为执行上下文细化。
- 当前分支为 `docs/BlackWhaleK/module4.2_design`。任务起点 main 提交为 `be53720265c02373e903eacafa16588a3337584b`，本轮已刷新并比较的 `origin/main` 为 `fa681efa18b8f4ea58947c6176b0f58ab7a75714`。
- 本轮成果属于工作分支候选修订，后续合入 main 按 PR 审阅和人工确认流程进行。

## 本轮已确认并落稿

- **职责与协作**见 §4.2.1。M1 提供场景及版本，场景决定 Node 编排，Runner 负责执行和 Execution 状态，按需调用 M3、M4、M5，M6 收集运行信息。
- **核心对象**见 §4.2.2。确定 Node、Execution、Session、Runner，暂缓定义独立 Step。M6 逐次记录模型和工具调用，具体记录器接口待设计。
- **Execution 生命周期**见 §4.2.3。采用 RUNNING、WAITING_INPUT、SUCCEEDED、FAILED、CANCELLED 五状态。补充条件或选择口径通过同一 Session 续接原 Execution，Runner 统一管理状态转换及终态。
- **SSE 与续接**见 §4.2.3.3—§4.2.3.4。每轮输出完成后关闭 SSE，追问后 Execution 保持 WAITING_INPUT，后续通过 Session 查找当前执行。早期“断线即取消”的提案已撤回。
- **临时保存**见 §4.2.3.6。Session 及 Execution 状态采用缓存，按最近一次有效对话续期，连续 8 小时无有效对话后过期。后端重启后允许提示会话失效并重新发起。
- 已统一说明书标题层级和章节编号，保留 M1—M7、P/F 稳定标识。README 的唯一说明书入口已改为 V2。

## 下次讨论入口

从 **§4.2.4 核心对象关联关系、执行流程**继续。基于已确认的对象和生命周期，讨论 M1 进入 Runner 后，Session、Execution、Node 之间如何关联，以及一次执行和跨轮续接的推进过程。

后续章节依次为 §4.2.5 节点类型细化、§4.2.6 业务场景映射、§4.2.7 接口契约、§4.2.8 运行参数与验证。

## 待确认与未验证

- 循环、并行、汇聚及 ReAct Node 的控制规则，模型判断暂不推进时的行为。
- 执行超时、等待输入是否另设超时及具体时限。
- 终态结果的获取范围与清理时点、缓存续期和并发续接的实现细节。
- Node 输出与 Runner 调度的契约、Session 标识携带及归属校验。
- 工程实现、真实依赖的取消能力、模型接口及技术兼容性均待验证，技术验证范围见 §2.3.3。

## 协作与续接

- 用户要求先讨论并审阅拟稿，明确同意落稿后再修改说明书。远端推送每次须取得明确同意，本轮用户已授权整理 Handoff 并提交推送。
- 本轮检查覆盖文档差异、相对链接、需求编号、章节指引与确认状态。
- 继续本任务时读取[任务 Handoff](../../collaboration/tasks/module4.2_design/handoff.md)。本轮讨论依据在本任务[纪要](../../collaboration/tasks/module4.2_design/notes.md)和[提案](../../collaboration/tasks/module4.2_design/proposal.md)中。
- 新任务按 AGENTS、README、V2 和本文件的顺序建立上下文，追加材料范围以用户指定为准。
