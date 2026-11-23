# 任务提案：M3_DESIGN

状态：基础框架版本已写入分支候选稿；F-001 输出边界仍待确认。本文不构成主线基线。

## 目标与依据

按[说明书](../../../docs/baseline/specificationV2.md) §3.1.3，以 P-008、P-009、P-010、P-025 为范围，从 F-001 的一次意图识别讨论 M2 与 M3 的分工。具体意图取值由场景提供，不写入通用 M3。实际模型接口能力未验证。

## 候选修订

| 需求/决策或章节 | 当前结论 | 拟议结论 | 理由与影响 | 确认状态 |
| --- | --- | --- | --- | --- |
| P-010、F-001、§4.2 | M3 校验结构化输出；F-001 有无法识别意图的提示；Execution 按规则处理节点结果。 | 场景契约允许显式返回「无法识别」。通过校验时 M3 交回正常结果，由场景映射提示。响应不可解析、缺少必填意图或取值越界时，M3 交回失败原因，不伪造业务结果。 | M2 可区分业务分支与调用错误；M3 不决定后续节点，不另设重试策略。 | 待确认 |

## 需要人判断

### 基础栈与 M3 模型适配（版本线已落稿）

本轮结果：用户复核公司文档后指定采用 Boot 3.5；§2.3.1 已按前轮配套建议调整为 JDK 17、Spring Boot 3.5.x、Spring AI 1.1.x。以下是先前讨论，不代表已验证的兼容组合或公司维护承诺。

2026-11-23 更新：用户准备将平台提升至 Boot 3.5+。下述普通 SDK 优先的取舍针对此前 3.3.x 限制，不是新前提的首选。在保留 JDK 17、以 3.5.x 为公司维护线的条件下，建议 M3 优先 Spring AI 1.1.x，不并用 LangChain4j。若允许 Boot 4，按对应 Spring AI 2.x 代际另评估内部组件。M2 编排、M4 授权和 M5 的 ES 7.17 检索不变。需确认补丁来源并验证内网模型服务。[Spring AI 发布依据](https://spring.io/blog/2026/06/12/spring-ai-1-1-8-1-0-9-avaialble-now/)；[Boot 3.5 开源维护状态](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/)。

关于 M2 编排选型的独立讨论：建议首期维持 §2.3.1 的轻量执行器，不切换到外部服务。若共享复杂子流程与调度逻辑明显增多，可对照 F-002 和并行失败契约评估 [LangGraph4j Core](https://github.com/langgraph4j/langgraph4j)。它是 Java 图执行库，与 M3 客户端可独立选型；JDK 17 基线不代表 Boot 3.3.x 组合或业务语义已验证。此建议不改变 F-001 的未决问题。

以用户指定 Boot 3.3.x、JDK 17 为前提。首选由 M3 封装框架无关的 openai-java SDK，显式指向内网兼容端点；若协议差异较大，改用 HTTP 适配器。提示词、输出契约、编排、工具授权和重试预算不交由 SDK 决定。多种非兼容供应商成为确定需求时再评估 LangChain4j 普通 Java 模块。以上不改变现有基线。

依据：[Spring AI 1.0 支持范围](https://docs.spring.io/spring-ai/reference/1.0/getting-started.html)、[Boot 3.3 开源维护状态](https://spring.io/blog/2025/06/19/spring-boot-3-3-13-available-now/)、[SDK 配置和重试](https://github.com/openai/openai-java)、[LangChain4j Starter 支持范围](https://docs.langchain4j.dev/tutorials/spring-boot-integration/)。需核对内部维护线、模型接口与依赖组合，未验证。

### F-001 结果边界（待确认）

是否同意区分这两类返回？例如，「帮我订会议室」被有效判定为三类意图之外，与「我想请假」却收到缺少意图字段的模型响应，是否应分别走业务提示与技术错误路径？这是讨论示例，不是运行结果。
