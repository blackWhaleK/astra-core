# 当前项目 Handoff

更新于 2026-11-18（Asia/Shanghai）。当前处于需求与架构文档阶段，业务结论见[唯一说明书 V2](../baseline/specificationV2.md)。

## 新对话从这里继续

下一专题为 M1 场景服务设计，负责人为 `BlackWhaleK`。按用户指定，继续使用 `docs/BlackWhaleK/M2_FINISH` 分支及其同名远端分支。本机 worktree 为 `D:/astraCore-K/.worktrees/M2_FINISH`，也可用 `git worktree list` 定位。任务来源 main 为 `a17ccc75163d269aa3da9555dce285306191a953`。续接应以该工作分支的候选说明书为起点。

在该 worktree 中依次读取 AGENTS.md、README.md、[说明书](../baseline/specificationV2.md)及本交接。M1 的业务入口是 §4.1.1—§4.1.2，P-001—P-003 给出平台能力范围。如需续接任务记录，见 [M1 任务 Handoff](../../collaboration/tasks/M1_scene_design/handoff.md)。

## 已确认并落入候选稿

- **M1 职责与协作（§4.1.1）。** 新调用由调用方明确提供场景标识和版本号。M1 保留各版本记录，查找指定版本、校验输入并将编排交给 M2。指定版本不存在时，M1 在入口返回提示。人工反馈关联原 Execution，并沿用启动时的编排。
- **M1 核心对象（§4.1.2）。** SceneDefinition、SceneVersion 描述场景及版本。SceneManagementService 登记和查询，SceneInvocationService 对外提供 start、reply、cancel。SceneRequest 与 SceneResponse 表达请求及响应，一次 Execution 可关联多次请求。
- **M2 已有运行契约（§4.2）。** Session、Execution、Node 的协作及接口见 §4.2.1—§4.2.7。§4.2.8 已完成首期运行规则、参数及七项验证预期，覆盖人工反馈、循环和 Agent 限制、超时、重试、并行汇聚、刷新与结果交付。8 小时用于会话空闲有效期，实际运行时限可配置。页面刷新开启新 Session，SSE 单纯断线由原 Execution 按生命周期继续推进。

## M1 接下来可讨论的内容

建议先用一次 `start`、一次 `reply`、一次 `cancel` 对照 §4.1.2，细化每种操作需要的输入、身份及关联标识、M1 返回的响应，以及向 M2 交付的内容。可用 F-002 的追问与回复验证请求和 Execution 的关联，再用 F-001 或 F-003 检查一次性调用的结果交付。具体接口字段、传输路径、SSE 输出与页面交互仍待讨论，以上顺序为建议。

新方案先在对话中展示，待用户明确确认落稿后再修改候选说明书。当前成果为工作分支文档，实际业务实现、接口接入和运行验证尚待开展。
