# 任务 Handoff：M3_DESIGN

- 负责人：BlackWhaleK
- 分支：`docs/BlackWhaleK/M3_DESIGN`
- 来源 main 完整提交：`14825e5d945b24e052ed7410eebaff3a5c47cbe2`
- 最近比较的 main：`14825e5d945b24e052ed7410eebaff3a5c47cbe2`（2026-11-19 拉取 origin/main）
- 任务目标与完成标准：讨论 M3 模块设计，覆盖 P-008、P-009、P-010、P-025；获用户同意落稿后才修改候选说明书。非目标为业务实现和接入验证。
- 当前状态：讨论中。未经用户允许不推送。
- 本任务允许读取材料：根 AGENTS.md、README.md、[说明书](../../../docs/baseline/specificationV2.md)、[项目 Handoff](../../../docs/handoff/current.md)、collaboration/templates 与本任务目录；不加载历史归档及其他任务过程材料。

## 已完成与当前候选改动

从已拉取的 origin/main 建立独立工作树和任务分支；[纪要](notes.md)记录确认依据，[提案](proposal.md)保留备选方案。候选说明书 §2.3.1 仅调整基础框架版本：Spring Boot 3.4.x → 3.5.x、Spring AI 1.0.x → 1.1.x，保留 JDK 17。P-008 及其他模块职责未变。

## 检查与确认

2026-11-23，BlackWhaleK 确认公司允许 Boot 3.5，并要求调整候选稿。已核对说明书全文版本引用与 P-008；具体补丁、驱动兼容组合及实际模型接入仍属 §2.3.3 待验证项。未运行模型或业务测试。

## 阻塞和未决事项

待负责人判断：场景允许的「无法识别」与违反输出契约是否分为两类返回。其他 M3 接口细节保持开放。

## 下一步

记录用户对首个取舍的答复。随后讨论 M2 传入的文本、必要上下文及提示词版本引用，再用 F-002 分段文字与 F-003 报告片段检查契约是否适用。
