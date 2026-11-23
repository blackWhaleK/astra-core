# 任务 Handoff：M3_DESIGN

- 负责人：BlackWhaleK
- 分支：`docs/BlackWhaleK/M3_DESIGN`
- 来源 main 完整提交：`14825e5d945b24e052ed7410eebaff3a5c47cbe2`
- 最近比较的 main：`14825e5d945b24e052ed7410eebaff3a5c47cbe2`（2026-11-19 拉取 origin/main）
- 任务目标与完成标准：讨论 M3 模块设计，覆盖 P-008、P-009、P-010、P-025；获用户同意落稿后才修改候选说明书。非目标为业务实现和接入验证。
- 当前状态：已确认范围已落稿，待后续审阅。未经用户允许不推送。
- 本任务允许读取材料：根 AGENTS.md、README.md、[说明书](../../../docs/baseline/specificationV2.md)、[项目 Handoff](../../../docs/handoff/current.md)、collaboration/templates 与本任务目录；不加载历史归档及其他任务过程材料。

## 已完成与当前候选改动

§2.3.1 保留 JDK 17、Spring Boot 3.5.x、Spring AI 1.1.x。BlackWhaleK 审阅预览及 ChatModel 接入说明后明确「同意，落稿吧」。已将 P-008 和 §4.3.1—§4.3.2 写入[候选说明书](../../../docs/baseline/specificationV2.md)，包括七个对象、同步结果与流式片段、输入组织、输出校验和三个场景边界。确认依据见[纪要](notes.md)与[提案](proposal.md)。

## 检查与确认

核对章节、平台能力编号、向量化关联引用和相对链接；本次只交付设计文档，不代表已实现或完成接入验证。

## 阻塞和未决事项

无本次落稿阻塞。向量生成提供方与实现方式未定；公司网关的同步、流式和协议兼容性尚未验证。

## 下一步

继续设计时从 §4.3.2 的对象与同步、流式交付关系出发。候选稿仅在任务分支本地保存；未经用户允许不推送。
