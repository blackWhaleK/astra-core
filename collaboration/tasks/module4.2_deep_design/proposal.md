# 任务提案：module4.2_deep_design

状态：用户已确认并落稿“§4.2.7 最新落稿审阅稿”及全部关联修改。此前的四对象方案和 Java 示例作为讨论来源保留，当前设计采用 Session、Execution、Node 及 M2 入口。业务内容见[唯一说明书 V2](../../../docs/baseline/specificationV2.md)。

## 目标与依据

以 `ab2dbc03842fed774812c5a03e890a4008c70af0` 的 §4.2.1—§4.2.3 为依据，先补足 §4.2.4 的对象关系与执行流程。M1 选择场景版本并将输入交给 Runner。Runner 创建固定版本的 Execution，按编排执行 Node。多轮 Session 关联至多一个未结束的 Execution。F-002 的有效澄清回复续接原 Execution，终态后新的独立问题创建新 Execution。F-001、F-003 的一次性 Session 随当次执行结束。

以 F-002 为例，用户提出查询，Runner 执行到需要选择指标口径时进入 WAITING_INPUT。用户提交关联的选择后，Runner 在原场景版本下从续接位置继续。执行完成后，用户问“去年同期呢”，Runner 创建新 Execution，并按场景规则沿用仍适用的查询条件。

## 候选修订

| 需求或章节 | 当前结论 | 拟议结论 | 理由与影响 | 确认状态 |
| --- | --- | --- | --- | --- |
| P-005、P-006，§4.2.4 取舍 1 | WAITING_INPUT 接收与当前 Execution 关联的回复。新的独立问题须等待当前执行结束。 | 由调用方显式标明输入是针对当前澄清的回复或新的独立问题。新问题在等待期间被拦截，用户可先取消再发起。澄清关联的具体结构留待 §4.2.7。 | 用户意图可预期，也能处理过期选项误投。接入方需提供对应交互。由场景根据文本判断可简化入口，但可能把新问题当成补充条件。 | 用户已确认 |
| P-005，§4.2.4 取舍 2 | 同一 Session 有未结束的 Execution 时，新的独立问题须等待，当前未明确由哪一方等待。 | RUNNING 时即时反馈当前执行仍在进行，由用户在结束后重新提交，或先显式取消。首期由用户操作决定是否重发。 | 用户可以决定是否继续，且后续请求不会在上下文已变化时自动启动。服务端排队可减少重发动作，但需要定义排队顺序、取消和上下文读取时点。 | 用户已确认 |
| P-006、F-002，§4.2.4 取舍 3 | Session 可保存消息、查询条件和用户选择。有效条件可跨轮继承，失败或取消后的继承边界尚未规定。 | 用户输入与提示可供会话展示。本次新提取的查询条件与口径选择先用于当前 Execution，场景正常完成时才更新后续可继承条件，正常返回“暂无数据”也适用。失败或取消后沿用上一次正常完成时确定的条件。 | 降低下一问误用未完成查询条件的概率。代价是用户在失败后需要重述刚选的条件。保留失败前已明确提交的条件可减少重输，但要约定哪些输入可安全用于下一问。 | 用户已确认 |

## 用户确认

2026-11-16 用户逐项同意上表三项取舍，依据见[纪要](notes.md)。说明书的所有修改须在用户确认后更新。

## 用户明确的对象层级与职责方向

用户本轮提出“Session应该在execution的层级之上，可能会包含多次execution。node的触发要依赖execution内的规则。runner只负责执行，属于无状态对象”。以下据此整理，具体职责描述仍待审阅。

| 对象 | 拟议职责与关系 |
| --- | --- |
| Session | 会话层对象，包含本会话下的一次或多次 Execution，保存跨次执行可用的上下文和当前未结束执行关联。 |
| Execution | 一次执行的状态与控制规则所在。关联固定的场景版本，依据本次规则、节点运行结果和当前状态确定可执行节点、等待输入或结束。保存节点运行状态、中间结果和续接位置。 |
| Node | 承担具体处理，接受输入并产出结果。是否触发及何时触发取决于所属 Execution 的规则和当前状态。 |
| Runner | 无状态执行组件，按 Execution 给出的可执行节点及输入调用 Node，将结果交回 Execution，供其按规则推进。跨节点、跨请求所需状态由 Execution 或 Session 承载。 |

规则来源建议与 §4.2.1 对齐：场景版本提供编排定义，Execution 固定引用该版本的规则，并结合自己的运行状态应用这些规则。Runner 的执行实例复用方式、并发控制和调度入口仍待具体设计。

三功能示例随之调整。F-001 的 Execution 根据意图和查询结果决定触发哪些业务节点。F-002 的 Execution 在缺参时等待，收到有效回复后按自身续接规则确定可执行节点。F-003 的 Execution 根据解析结果和制度依据推进检查及汇总。Runner 在各阶段执行已具备触发条件的节点并交回结果。

本轮层级定义表示业务对象的归属关系。已完成 Execution 的临时结果与状态保留多久，仍沿用 §4.2.3.6 的待设计项。

## 对象关系与流程定位

以下为此前分析。其中 Runner 调度及管理状态的措辞需按上节用户方向重新整理。

- **Session 与 Execution。** 每次 Execution 关联一个 Session。多轮 Session 可以先后关联多个 Execution，同时只关联一个未结束的 Execution。Session 保存跨轮可用的会话上下文，当前节点、中间结果和续接位置由 Execution 保存。
- **场景版本与 Node。** Execution 使用启动时固定的场景版本。Node 由场景版本配置，同一个 Node 可被不同 Execution 执行，也可在同一次 Execution 中运行多次。每次运行的状态记录在对应 Execution 的临时上下文中。首期暂缓定义独立 Step。
- **Runner 与三个对象。** Runner 根据场景版本调度 Node，并根据节点输出及编排推进 Execution。需要追问时，后续请求可通过 Session 找到当前 Execution。Runner 读取该 Execution 中的续接信息，继续调度。
- **F-002 示例。** 用户先问“本月福州分行离职率是多少”，同一 Session 中创建 Execution A。用户选择口径时，新的请求通过 Session 续接 A 并使用 A 原有的场景版本。A 完成后，用户问“去年同期呢”，同一 Session 创建 Execution B，读取仍适用的查询条件，重新执行本次查询。
- **待讨论。** 若 A 和 B 之间发布了新版场景，同一 Session 的 B 是由 M1 按新调用重新选版本，还是沿用 Session 首次选定的版本？建议每次新建 Execution 时由 M1 选版本，同一次 Execution 的澄清续接继续使用已固定的版本。跨版本查询条件是否仍适用，需由场景契约判断。

## F-001、F-002、F-003 联合分析

以下依据 §3.2 和 §4.2.1—§4.2.3 分析对象关系。节点串联为逻辑示意，节点拆分粒度和具体编排待确认。

| 关系或流程位置 | F-001 辅助办理 | F-002 查询分析 | F-003 报告检查 |
| --- | --- | --- | --- |
| Session | 一次性调用关联一个临时 Session，随当次 Execution 结束 | 多轮共用 Session，保存跨轮消息及可继承条件，关联当前未结束执行 | 一次报告检查关联一个临时 Session，随当次 Execution 结束 |
| Execution | 一次文本处理，贯穿意图识别、业务查询及结果生成 | 一次查询分析可跨首问和澄清回复，完成后的下一问新建 Execution | 一次上传报告的检查，贯穿解析、制度检索、逐项分析及汇总 |
| Node 逻辑串联 | 意图识别与提取 → 分支 → 人员或余额查询及判断 → JSON 或提示 | 问题理解与口径检索 → 条件判断 → 必要时澄清续接 → 接口取数 → 分析输出 | 文档解析 → 制度检索 → 对照检查 → 汇总输出 |
| Execution 临时上下文 | 本次提取参数、人员和余额查询结果、分支结果 | 本次查询条件、中间数据、待选口径、待补内容及续接位置 | 解析内容或临时引用、制度依据、各项检查结果及汇总过程 |
| 用户后续操作 | 按提示重新提交完整输入，建立新的 Session 和 Execution | 澄清回复续接原 Execution，终态后的独立问题在同一有效 Session 下新建 Execution | 修改报告后重新上传，建立新的 Session 和 Execution |

Runner 对三个功能承担相同的执行管理职责，按固定场景版本调度 Node，保存当前 Execution 的状态和中间结果，通过 M1 返回提示或结果。Node 按所需能力调用 M3、M4、M5。逻辑上多个 Execution 可使用相同版本的节点定义，各次运行数据归各自 Execution。节点调用模型或工具期间，Execution 仍处于 RUNNING。

F-001 的信息不足提示直接结束当次处理。F-002 的补参追问使当前 Execution 进入 WAITING_INPUT。F-003 的“无法判断”可作为逐项检查结果进入最终汇总。按 §4.2.3 对 SUCCEEDED 的定义，正常输出余额不足提示或包含无法判断项的检查报告，可以对应执行按约定完成。依赖调用重试耗尽等无法继续的错误对应 FAILED。

待讨论的关系包括 Session 创建与查找由 M1 还是 M2 负责，以及 Node 如何向 Runner 表达需要用户输入和后续续接位置。建议 M1 提供已核验的调用身份及 Session 关联信息，由 M2 统一管理 Session 和 Execution 的关联。建议 Node 返回结构化的待补内容，Runner 统一保存续接信息并切换状态。上述两项为本轮 AI 建议。

三个功能的跨场景会话共享尚无确认依据。本次联合分析分别展示其在同一套 M2 对象模型中的用法。前述新 Execution 的版本选择仍待讨论。

## §4.2.4 拟稿

原拟稿由下方第 4 章审阅稿替代。

## 第 4 章审阅稿

状态为用户已确认并落稿。确认依据为用户回复“同意 写入吧 一并调整”。修订依据为本任务三项交互取舍确认、用户关于 Session 层级与无状态 Runner 的明确方向，以及来源提交中的 §3.2、§4.2.1—§4.2.3。

以下列明拟替换或补入的准确范围和文字。对 Execution 应用规则、Runner 交回结果的具体描述为本轮整理，随本稿一起审阅。采用 §4.2.4 描述通用对象关系与执行流程、§4.2.6 承载三功能映射的章节安排。

### §4.2.1 替换第二段

M2 提供预先定义的 Node 类型，例如并行 Node、循环 Node 和 ReAct Node。场景版本定义节点及编排规则，Execution 固定引用启动时选定版本的规则，并结合本次状态和节点输出确定后续可执行的 Node。Runner 是无状态执行组件，按 Execution 确定的节点及输入执行调用，将输出交回 Execution。场景可以配置具备运行时判断能力的 Node。例如，循环 Node 可以通过 M3 调用模型形成判断结果，Execution 根据该结果及本次执行规则决定继续循环或推进到后续 Node。

### §4.2.2 替换对象表与紧随其后的第一段

| 对象 | 定义 | 最小信息或职责 |
| --- | --- | --- |
| Session | 会话层临时对象，包含同一会话下的一次或多次 Execution。一次性调用的 Session 随当次 Execution 结束。 | 会话标识、跨次执行可用的会话上下文、当前未结束 Execution 的关联标识及有效期信息。多轮会话可保存对话消息、查询条件和用户选择。 |
| Execution | 属于一个 Session 的一次执行，固定使用启动时选定的场景版本，承载本次执行的规则、状态和上下文。 | 执行标识、场景及版本、所属 Session、本次输入、执行规则引用、执行状态、临时上下文和最终结果。根据规则与当前状态确定节点触发、等待输入及结束。临时上下文包含各次节点运行状态、中间结果、待补充内容和续接位置。 |
| Node | 场景版本中配置的节点。M2 预定义 Node 类型及其行为，场景版本确定节点配置及执行关系。 | 节点标识、节点类型、节点配置。接受运行输入，完成具体处理，返回输出及供 Execution 应用规则的判断结果。触发条件由 Execution 内的规则及当前状态确定。 |
| Runner | 执行 Node 的无状态组件。 | 接收 Execution 确定的可执行节点及输入，执行节点调用，将输出或调用错误交回 Execution。跨节点、跨请求所需的执行状态与续接信息由 Execution 承载。 |

Session 位于 Execution 之上，多轮 Session 可以先后包含多个 Execution，同一时刻最多存在一个未结束的 Execution。Execution 使用所选场景版本中的 Node 定义。同一个 Node 可以在不同 Execution 中运行，也可以在一次 Execution 中运行多次。各次节点运行通过所属 Execution 的临时上下文区分，首期暂缓将这些运行建模为独立的 Step 对象。Session 提供跨次执行可用的会话上下文，Execution 保存本次处理所需的运行信息。临时状态保存、过期和清理规则见 §4.2.3。

### §4.2.3 替换引导段

本节细化 P-005、P-006。Execution 承载本次执行的状态与生命周期规则，根据当前状态、节点输出、关联回复、取消及超时条件确定后续处理。Runner 按 Execution 确定的可执行节点执行调用并交回结果。

### §4.2.3.1 替换 CANCELLED 状态说明

| 状态 | 含义 |
| --- | --- |
| CANCELLED | 用户手动结束，Execution 已停止触发后续节点并确认本次执行结束。 |

### §4.2.3.2 替换全文

M2 接收 M1 提供的场景及版本、本次输入和 Session 关联信息，校验启动条件。同一 Session 存在未结束的 Execution 时，新的独立问题即时收到当前执行尚未结束的提示。用户可以在当前执行结束后重新提交，也可以先显式取消，再提交新问题。

校验通过后，在 Session 下创建 Execution，固定场景版本及执行规则，进入 RUNNING。Execution 根据规则和当前状态确定可执行节点及输入，Runner 执行节点并交回结果。Execution 保存节点运行结果，并据此决定后续节点或状态转换。启动条件不满足时，通过 M1 返回相应提示。

等待模型或工具返回、循环内继续执行、允许范围内的重试均维持 RUNNING。

### §4.2.3.3 替换全文

需要用户澄清或选择时，Execution 按规则保存待补充内容、续接位置和必要的中间结果，进入 WAITING_INPUT，并通过 M1 提示用户。Session 保存当前未结束 Execution 的关联标识，实际执行状态和续接信息保存在 Execution 中。

后续请求携带同一 Session 标识，并由调用方显式标明输入是针对当前澄清的回复或新的独立问题。服务端核对会话归属及回复关联后，找到当前 Execution。有效的关联回复使同一个 Execution 回到 RUNNING，继续使用原执行标识、Session 和场景版本。Execution 根据续接信息及规则确定后续可执行节点，由 Runner 执行。

回复仍不足时保持 WAITING_INPUT，继续提示用户。重复提交的同一回复仅触发一次续接。等待期间也可接收用户手动结束请求。用户希望改问新的独立问题时，可以先取消当前执行再提交。关联回复的具体接口契约见 §4.2.7。

### §4.2.3.4 替换第一段最后一句

Execution 根据本次生命周期规则处理执行结果、关联回复、显式取消或适用超时，并确定状态转换。

### §4.2.3.5 替换全文

场景按约定完成后，Execution 确定最终结果并进入 SUCCEEDED。例如查询正常返回“暂无数据”，可以完成本次执行。

出现无法继续处理的异常、适用重试耗尽或配置的执行时限到达时，Execution 进入 FAILED，保存失败原因和可返回的提示。执行时限以及等待输入是否另设超时、采用何种时限，留待运行参数设计。

RUNNING 和 WAITING_INPUT 均可响应用户手动结束。取消被接受后，Execution 停止触发后续节点，M2 根据依赖提供的能力尝试终止在途调用，并按生命周期规则确定 CANCELLED。依赖侧调用的实际终止能力待接入验证。

成功、失败、取消或超时并发到达时，同一 Execution 只确认一次终态，并同时确定对应结果。终态确定后的迟到调用结果不再触发后续 Node，也不再修改当前执行和会话。进入终态后，通过 M1 提供结果或结束原因，更新 Session 中的当前执行关联，释放运行资源，允许用户直接提出下一问。

### §4.2.3.6 在最后一段前补入

F-002 本次新提取的查询条件和口径选择先用于当前 Execution。执行正常完成时，将本次确定的条件更新到 Session，供后续 Execution 按场景规则继承，正常返回“暂无数据”也适用。执行失败或取消后，Session 中可供后续继承的条件沿用最近一次正常完成时确定的内容。用户输入与提示可作为临时会话消息保存和展示。

### §4.2.4 补入正文

本节细化 P-004、P-005、P-006。Session 组织会话及其中的 Execution，Execution 根据本次规则和状态决定节点触发及执行进度，Runner 执行节点，Node 完成具体处理。

1. M1 选定场景及版本，将本次输入和 Session 关联信息交给 M2。新调用通过校验后，在所属 Session 下创建 Execution，并固定本次场景版本和规则。
2. Execution 根据规则和当前状态确定可执行 Node，组织对应输入，交给 Runner 执行。Node 按需调用 M3、M4、M5 的能力，返回处理结果或供后续规则使用的判断结果。
3. Runner 将节点输出或调用错误交回 Execution。Execution 更新本次节点运行状态和中间结果，依据规则决定继续执行、进入等待或结束。
4. 需要用户补充时，Execution 保存续接信息并进入 WAITING_INPUT，通过 M1 发出提示。有效的关联回复到达后，通过 Session 找到原 Execution，按其续接规则继续执行。
5. Execution 进入终态后，确定最终结果或结束原因，更新所属 Session 的当前执行关联。一次性 Session 随当次 Execution 结束，多轮 Session 保留后续对话所需的上下文，后续独立问题在同一有效 Session 下创建新的 Execution。

同一 Node 的各次运行共享该节点的定义，各自的输入、输出和运行状态归所属 Execution 管理。并行、循环及汇聚的具体触发规则见 §4.2.5 的后续设计。

### §4.2.6 补入正文

以下结合 F-001、F-002、F-003 说明对象层级和流程定位，节点顺序为逻辑示意，具体拆分及配置留待节点设计。

| 对象或流程 | F-001 辅助办理人力业务流程 | F-002 查询与分析人力数据 | F-003 检查征信报告内容 |
| --- | --- | --- | --- |
| Session 范围 | 一次性调用的临时会话，包含当次 Execution | 多轮对话共享会话，先后包含多个 Execution，保存可继承的查询条件和口径 | 一次报告检查的临时会话，包含当次 Execution |
| Execution 范围 | 一次文本输入到页面 JSON 或业务提示 | 一次查询分析，包含必要的澄清及选择，终态后新的独立问题创建新 Execution | 一次上传报告到完整检查结果 |
| 逻辑处理顺序 | 意图识别与参数提取、分支选择、所需的人员或余额查询、条件判断、结果生成 | 问题理解、指标口径检索、条件判断与必要澄清、接口取数、分析输出 | 文档解析、制度检索、对照检查、结果汇总 |
| Execution 临时上下文 | 提取参数、人员及余额查询结果、分支判断和待返回结果 | 本次条件、待选口径、待补内容、续接位置、查询数据和分析结果 | 解析内容或临时引用、制度依据、逐项检查结果和汇总结果 |
| 用户后续输入 | 用户按提示重新提交完整输入，开始新的 Session 和 Execution | 澄清回复续接原 Execution，执行结束后的下一问在同一有效 Session 下新建 Execution | 用户修改报告后重新上传，开始新的 Session 和 Execution |

F-001 中，Execution 根据意图和查询结果确定后续节点。人员多匹配或信息不足时，按业务规则返回提示并结束本次处理。Runner 执行各次已满足触发条件的 Node。

F-002 中，查询“本月福州分行离职率”形成 Execution A。需要选择口径时，A 进入 WAITING_INPUT，用户选择后续接 A。A 正常完成后，用户追问“去年同期呢”，在同一 Session 下创建 Execution B，并沿用仍适用的机构和口径，按新时间范围执行查询。

F-003 中，Execution 依据解析内容和制度检索结果推进检查，并收集各项检查结论形成最终汇总。部分条目缺少判断依据时，可以将“无法判断”作为该条目的结果继续汇总。

按 §4.2.3 的状态定义，正常返回“余额不足”提示或包含“无法判断”条目的检查结果，可以对应 SUCCEEDED。依赖调用重试耗尽等导致执行无法继续的错误对应 FAILED。

### 审阅边界与待讨论事项

本次涉及 §4.2.1、§4.2.2、§4.2.3 的上述具体位置、§4.2.4 和 §4.2.6。既有状态转换表、缓存时限和 SSE 生命周期关系继续作为本稿依据。

Session 的具体创建和查找组件、新 Execution 的场景版本选择策略、Node 的结构化返回契约，以及并行控制和终态结果清理仍待讨论。

第 3 章 P-005 原写为“Runner 管理 Execution 的运行、等待输入、完成、失败及取消”。用户已同意同步改为“Execution 根据本次规则管理运行、等待输入、完成、失败及取消，关联场景、节点和结果。Runner 负责执行节点。SSE 连接生命周期与 Execution 独立”，该关联修改已落稿。

## §4.2.7 对象接口契约设计建议

状态为 AI 建议、待确认。依据为当前分支说明书 §4.2.1—§4.2.4、§4.2.6。以下操作名和字段名用于需求级接口讨论，具体语言类型与实现方式待设计。

### 对象字段与操作

| 对象 | 主要字段建议 | 操作建议 |
| --- | --- | --- |
| Session | sessionId、ownerRef、mode、conversationContext、currentExecutionId、expiresAt | startExecution 接收选定的场景版本及本次输入，校验会话有效且当前执行已结束，创建并关联新 Execution。currentExecution 获取当前关联。finishExecution 接收已确定的执行终态及待继承条件，核对关联后释放当前执行，按完成状态更新可继承条件。 |
| Execution | executionId、sessionId、sceneVersionRef、rulesRef、input、state、context、nodeRuns、pendingInput、result、error | start 按规则启动。acceptNodeResult 接收某次节点运行结果。resume 接收关联回复。cancel 接受显式取消。onTimeout 处理适用超时。snapshot 提供状态及可返回内容的只读视图。 |
| Node | nodeId、type、config，归属场景版本 | execute 接收节点输入及本次调用上下文，返回节点结果。具体实现按需调用 M3、M4、M5。 |
| Runner | 运行依赖与配置由 M2 装配 | run 接收 Execution 已确定的一次节点调用，调用对应 Node 并交回运行结果。每次调用携带所需的关联标识、时限和取消信号。 |

Session 的创建、读取、缓存写入及所属身份校验由 M2 接入处理组织，具体模块位置待确认。startExecution 对 Session 对象的建议职责为关联新执行并维护单个未结束执行约束，Session 在缓存中的逻辑包含关系通过执行标识表达。

### Execution 与 Runner 之间的输入输出

Execution 的状态操作返回本次推进结果，包含新的执行状态、零个或多个已满足条件的节点调用，以及需要向 M1 返回的追问、最终结果或结束原因。返回零个节点时可能正在等待其他在途节点或用户回复，结束与否根据执行状态判断。输出多个节点用于表达已经具备并行执行条件的调用，汇聚规则继续由 Execution 判断。

一次节点调用建议包含 executionId、nodeId、invocationId、节点定义引用、input，以及可信调用身份、traceId、截止时间和取消信号组成的调用上下文。调用上下文按节点实际需要提供，节点参数与可信身份分别组织。

一次节点运行结果建议包含相同的关联标识，以及以下一种结果内容。

| 结果内容 | 用途 |
| --- | --- |
| 正常输出 | 携带节点输出和可供规则判断的事实，例如候选口径、人员查询结果或分段检查意见。 |
| 需要用户输入 | 携带缺失字段、提示或选项。Execution 根据场景规则决定进入等待或生成业务提示并结束。 |
| 调用失败 | 携带错误分类、错误码及必要的依赖错误信息。Execution 结合规则与已有尝试情况决定后续处理。 |

invocationId 区分同一 Node 的各次运行，重试或循环产生新的调用标识，其记录保存在 Execution 的 nodeRuns 中。执行规则通过该记录关联节点、循环位置、分支和尝试次数。Node 返回输出与判断事实，Execution 确定后续节点。

### 等待、重复结果与取消

pendingInput 建议保存本次追问标识、待补内容和续接位置。resume 接收追问标识、回复标识和内容，服务端结合 Session 归属校验确认关联。Execution 仅在当前等待有效时处理，重复回复只触发一次续接。F-002 的澄清继续使用原 Execution，F-001、F-003 按既定一次性规则结束或重新发起。

Execution 接收节点结果时校验调用归属及接收状态。重复返回和终态后的迟到返回维持当前状态。取消、超时和结果到达需按同一 Execution 一致地处理，终态确认与对应结果同时确定，具体并发实现待设计。

取消信号随节点调用传递，Execution 接受取消后停止后续触发，由 M2 按依赖能力通知在途调用。Runner 持有完成当前调用所需的临时信息，跨节点和跨请求的执行进度保存在 Execution 中。运行时调用句柄的具体承载位置待实现设计。

Session 更新当前执行关联和继承条件时须核对 executionId，避免旧执行的重复完成通知影响新执行。并发新建执行须共同遵守单个未结束 Execution 的约束。接口方案需包含该一致性要求，具体缓存更新实现待设计。

### 三功能应用与待确认项

F-001 中，人员查询 Node 返回人员匹配结果，Execution 按规则决定继续查余额或返回业务提示。F-002 中，口径识别 Node 返回待选内容，Execution 保存 pendingInput 并等待，resume 后确定后续调用。F-003 中，检查 Node 返回各项结论，Execution 根据汇聚规则形成最终结果。

建议先确认 Session 通过 startExecution 和 finishExecution 维护会话级关联、Execution 提供上述少量状态操作、Runner 按一次节点调用执行三个接口方向。操作名称、字段类型、并行控制的详细契约及超时取值仍待逐项讨论。

## 三场景接口伪代码

2026-11-17 按用户要求补充。状态为讨论示意、待确认。以下采用事件驱动写法表达节点调用结束后将结果交回 Execution 的过程。操作名、规则表达方式和节点拆分属于建议。场景版本提供规则定义，Execution 固定引用并应用规则。伪代码中的 dispatch、waitForInput、succeed、fail 表示 Execution 内部操作。

### 共用接口与调用过程

```text
Session.startExecution(sceneVersion, input)
    校验归属、有效期及当前执行已结束
    创建 Execution，关联本 Session、场景版本和本次输入
    将可继承条件复制到 Execution.context
    关联 currentExecutionId，并返回 Execution

Execution.start()
    进入 RUNNING，按本次规则处理 START

Execution.acceptNodeResult(result)
    校验 executionId、invocationId 及本次调用尚未处理
    终态或重复结果到达时返回当前状态
    将本次结果记录到 nodeRuns 和 context
    调用失败时，按本次规则决定有限重试或 fail
    正常结果交给本次场景规则，确定下一次推进结果

Execution.resume(promptId, replyId, content)
    校验当前处于 WAITING_INPUT、追问匹配、回复尚未处理
    记录回复，按 pendingInput 中的续接位置进入 RUNNING
    按本次规则处理 USER_REPLY

Runner.run(invocation)
    node = 取得 invocation.nodeRef 对应的节点
    result = node.execute(invocation.input, invocation.callContext)
    返回附带 executionId、invocationId 的结果
    调用抛错时返回调用失败结果

M2 推进(execution, 操作)
    一致地执行操作并保存 Execution 状态及已确定的节点调用
    若进入终态，联动执行 Session.finishExecution
    将本次新增的追问、结果或结束原因交给 M1
    对每个已确定的节点调用
        Runner 执行一次调用
        完成后再次进入 M2 推进(execution, acceptNodeResult(result))
```

M2 推进是对接操作、状态保存和调用结果的流程描述。后续节点、等待及终态由 Execution 的规则决定。调用完成后重新进入推进过程可用任务或回调实现，具体执行机制待设计。

Session.finishExecution 核对 currentExecutionId 与已结束 Execution 一致，仅在正常完成时更新本次确定的可继承条件，然后释放当前执行关联。一次性 Session 随之结束，多轮 Session 保留有效上下文。上述关联更新与 Execution 终态需保持一致，重复完成通知只收尾一次。

### F-001 规则示意

```text
on START
    dispatch("识别意图与提取参数", input.文本)

on "识别意图与提取参数" 完成
    若无法识别意图
        succeed("无法识别用户意图")
    若信息不足或业务输入校验未通过
        succeed(对应业务提示)
    若意图为补卡
        dispatch("生成页面JSON", 已校验的补卡参数)
    若意图为本人请假
        dispatch("查询余额", 已核验身份中的本人标识)
    若意图为他人请假
        dispatch("查询人员", 提取的人员信息)

on "查询人员" 完成
    若人员匹配数不等于 1
        succeed("信息不足，无法办理")
    dispatch("查询余额", 匹配人员的标识)

on "查询余额" 完成
    若余额不足
        succeed("剩余请假余额不足")
    dispatch("生成页面JSON", 本次请假参数及所需人员信息)

on "生成页面JSON" 完成
    succeed(符合场景输出契约的 JSON)
```

每次 dispatch、waitForInput、succeed 或 fail 都结束当前规则分支并交回推进结果。JSON 字段及各类输入异常提示遵循 §3.2.1，包括为他人请假输出姓名、半天粒度、休息时间提醒和补卡日期限制。

入口通过一次性 Session.startExecution 创建本次 Execution 并调用 start。用户按提示重新提交完整输入时，创建新的 Session 和 Execution。

### F-002 规则与续接示意

```text
on START
    dispatch("理解问题与检索口径", input.问题 + context.继承条件)

on "理解问题与检索口径" 完成
    若找不到适用口径
        succeed("暂时无法确定口径")
    若缺少条件或有多个口径待选
        waitForInput(提示或选项, 续接位置 = "确认查询条件")
    保存本次已确定的查询条件
    dispatch("查询业务数据", context.本次查询条件)

on USER_REPLY，续接位置为 "确认查询条件"
    dispatch("校验并补全查询条件", 本次回复 + context.待补条件)

on "校验并补全查询条件" 完成
    若回复仍不足
        waitForInput(新的提示或选项, 续接位置 = "确认查询条件")
    保存本次已确定的查询条件
    dispatch("查询业务数据", context.本次查询条件)

on "查询业务数据" 完成
    若正常返回无数据
        succeed("当前条件下暂无数据", 可继承条件 = context.本次查询条件)
    dispatch("分析并组织展示", 查询结果 + 本次口径)

on "分析并组织展示" 完成
    succeed(分析及展示结果, 可继承条件 = context.本次查询条件)
```

```text
session = M2 创建多轮 Session
executionA = session.startExecution(选定场景版本, "本月福州分行离职率")
M2 推进(executionA, start())

用户选择口径后
    M2 校验 Session 归属及回复关联
    executionA = session.currentExecution()
    M2 推进(executionA, resume(本次追问标识, 回复标识, 用户选择))

executionA 正常完成，用户再问 "去年同期呢"
    executionB = session.startExecution(M1 本次选定的场景版本, 新问题)
    M2 推进(executionB, start())
```

新输入按业务规则修正仍适用的继承条件。M1 为新 Execution 选择版本的具体策略仍待确认。条件更新在本次 Execution 正常完成时发生，失败或取消后继续采用此前已确认的继承规则。

### F-003 规则示意

```text
on START
    dispatch("解析Word", input.报告临时引用)

on "解析Word" 完成
    dispatch("检索适用制度", 解析后的报告内容)

on "检索适用制度" 完成
    若找不到或无法确定适用制度
        succeed("制度依据不明确")
    dispatch("逐项检查", 报告内容 + 制度依据)

on "逐项检查" 完成
    保存各项结论及依据
    dispatch("汇总检查结果", 各项结论及修改意见)

on "汇总检查结果" 完成
    succeed(总体意见 + 逐项检查 + 修改意见)
```

一次性 Session 包含本次报告检查 Execution。检查项可输出满足要求、不满足要求或无法判断。Word 解析失败通过错误结果返回约定提示，知识库或模型调用失败按场景规则有限重试。用户修改报告后重新上传，创建新的 Session 和 Execution。

### 共用取消与异常约定

Execution.cancel 和 onTimeout 通过同一 M2 推进入口处理，与节点结果回写串行或采用等价的一致性控制。取消后停止触发后续节点，按依赖能力通知在途调用。Runner 执行前及调用期间按依赖能力检查取消信号，终态后的迟到结果保持原状态。重试由 Execution 规则决定并产生新 invocationId。以上流程只在有效临时状态范围内运行，持久化恢复能力仍遵循现有首期范围。

## Java 对象协作教学示例

2026-11-17 用户明确要求 Java 代码以理解接口。示例见 [M2ObjectExample.java](M2ObjectExample.java)，包括 Session、Execution、Node、Runner 和三场景的调用次序。状态为教学示例，接口细节待确认。

示例用单线程顺序调用、固定节点返回值和 String 输入输出展示对象关系。Execution.next 根据当前执行位置和已取得的结果确定 NodeCall、NeedInput 或 Completed，Runner 只执行 NodeCall。节点结果保存在 Execution 中。F-002 等待后通过 Session 取得原 Execution 并续接，正常完成后的下一问创建新 Execution，继承已选口径。

F-001 展示本人请假路径，F-003 展示顺序检查路径。节点中模拟数据仅用于阅读，规则暂直接写在 Execution 方法中。场景版本加载、实际业务输入输出校验、鉴权、缓存、并发、重试、取消和超时的完整实现留待后续设计。按节点名称保存输出适用于本示例的单次顺序调用，循环或重试的正式契约使用前述 invocationId 区分各次运行。

验证范围为 Java 17 编译检查，已通过。编译产物写入系统临时目录。示例调用流程尚未运行，说明书保持当前已确认内容。

## 类、接口与 Node 类型清单

2026-11-17 用户要求列出各类及接口，并明确 Node 类型。本节中现有方法按 M2ObjectExample.java 核对，正式接口和 Node 类型枚举为待确认建议。

### 示例中各类型的接口

| 类型 | 构造或方法 | 职责 |
| --- | --- | --- |
| Session | Session(boolean multiTurn) | 建立一次性或多轮 Session。 |
| Session | Execution startExecution(Scene scene, String input) | 创建并关联一次 Execution。 |
| Session | Execution currentExecution() | 取得当前关联的 Execution。 |
| Session | void finishExecution(Execution execution) | 结束关联，按正常完成状态更新继承条件。 |
| Execution | Execution(String sessionId, Scene scene, String input, String inheritedMetric) | 构造本次执行。 |
| Execution | Decision next() | 按本次规则确定节点调用、等待或完成。 |
| Execution | void acceptNodeResult(String nodeName, String output) | 保存节点输出，推进顺序执行位置。 |
| Execution | void resume(String reply) | 校验示例中的口径选择并续接。 |
| Execution | void fail()、boolean isTerminal() | 标记失败、判断终态。 |
| Execution | private Decision leaveRules()/queryRules()/reportRules() | 示例内部的三个场景规则。 |
| Execution | private Completed complete(String result) | 设置成功终态并返回结果。 |
| Node | String execute(String input) | 执行节点处理。 |
| Runner | String run(NodeCall call) | 执行一次指定节点调用。 |
| Decision | 封闭返回类型，无行为方法 | 约束本次推进结果的类型。 |
| NodeCall | NodeCall(String name, Node node, String input)，name()/node()/input() | 携带一次节点调用。 |
| NeedInput | NeedInput(String prompt)，prompt() | 携带等待输入提示。 |
| Completed | Completed(String result)，result() | 携带最终结果。 |
| Scene | F001、F002、F003 | 示例场景枚举。 |
| State | RUNNING、WAITING_INPUT、SUCCEEDED、FAILED、CANCELLED | 执行状态枚举。 |
| M2ObjectExample | static void drive(Session, Execution, Runner)、public static void illustrateThreeScenes() | 示例外层的推进过程和三个场景调用入口。 |

上述表列出手写方法与 record 字段访问方法。record 的构造、equals/hashCode/toString 以及 enum 的标准方法由 Java 提供。

### 正式接口拟议调整

Session 使用通用场景版本引用和会话上下文，替换示例中的 Scene.F002 判断及专用 confirmedMetric。Execution 引用场景版本规则，增加结构化运行结果、追问及回复标识、取消与超时操作，按 invocationId 区分节点各次运行。Runner 以节点调用及关联结果作为输入输出，维持无状态执行职责。

Node 建议提供 id()、type()、execute(NodeInput input, NodeContext context) 三个方法。id 标识场景版本内的节点，type 标识预定义类型。NodeInput 按节点配置的契约组织数据，NodeContext 按需提供可信身份、追踪信息、时限、取消信号和必要的只读运行信息。节点返回 NodeResult，包含输出、控制判断、待补内容或调用错误。Execution 接收这些结果后，按规则确定后续可执行节点及状态转换。

Node 的配置随场景版本装配到节点实例中。同一节点的各次运行数据在 Execution 中按调用标识保存。NodeInput、NodeContext、NodeResult 为接口数据结构，具体字段及 Java 类型继续待确认。

### Node 类型建议

来源说明书 §3.1.2 已列十类逻辑节点，§4.2.1 另有循环 Node 和 ReAct Node 的示例。建议把循环显式列为 LOOP，ReAct 作为 AGENT 的一种策略。如此形成以下十一种候选类型，该枚举及分类调整待用户确认。

| NodeType 建议值 | 逻辑类型 | 节点处理与输出 | Execution 的职责 |
| --- | --- | --- | --- |
| MODEL | 模型 | 通过 M3 处理提示词、模型调用及输出解析，返回结构化结果 | 使用结果推进规则 |
| TOOL | 工具 | 通过 M4 调用场景允许的工具，返回业务结果或错误 | 决定后续步骤及适用重试 |
| KNOWLEDGE_RETRIEVAL | 知识检索 | 通过 M5 返回检索内容及引用依据 | 将依据用于后续节点输入 |
| DOCUMENT_PARSE | 文档解析 | 通过 M5 返回文档文本和结构 | 保存临时解析结果并推进 |
| CODE | 代码处理 | 调用已注册的处理逻辑，返回映射、计算或校验结果 | 使用结果推进规则 |
| CONDITION | 条件分支 | 返回条件计算结果或分支键 | 按场景连接关系触发对应分支 |
| PARALLEL | 并行分发 | 输出本次分发所需的数据及候选分支信息 | 按规则产生多个调用并保存各分支状态 |
| JOIN | 结果汇聚 | 对传入的分支结果合并或转换 | 判断汇聚条件何时满足，再触发汇聚节点 |
| LOOP | 循环 | 计算继续或退出条件，可按场景需要使用 M3 的判断结果 | 保存轮次及中间状态，触发循环体或后续节点 |
| AGENT | Agent 组合 | 按策略产生本轮行动建议或完成判断，ReAct 为候选策略之一 | 校验允许范围、轮次和终止条件，推进模型或工具调用并保存运行信息 |
| RETURN | 结果返回 | 整理符合场景输出契约的结果 | 确认执行终态，交给 M1 返回 |

控制类节点提供本次处理结果，节点触发及状态转换继续归 Execution 规则管理。Agent 组合的内部编排展开方式和子调用追踪规则仍待细化。

用户追问先按现有 WAITING_INPUT 和待补内容契约表达，暂以现有结果结构讨论。若后续需要编排专用交互节点，再讨论是否扩展类型。

### 示例节点与候选类型的映射

F-001 的提取参数对应 MODEL，人员及余额查询对应 TOOL，条件判断对应 CONDITION，页面数据组织可对应 CODE，最终输出对应 RETURN。

F-002 示例中的“理解问题并检索口径”覆盖两个能力，正式编排建议拆为 MODEL 与 KNOWLEDGE_RETRIEVAL。接口取数对应 TOOL，分析对应 MODEL，展示数据整理可对应 CODE，输出对应 RETURN。

F-003 的解析报告对应 DOCUMENT_PARSE，检索制度对应 KNOWLEDGE_RETRIEVAL，逐项语义检查对应 MODEL，结果合并可按具体处理采用 CODE 或 MODEL，输出对应 RETURN。是否使用 PARALLEL、JOIN 或 LOOP，依实际报告处理规则确认。

## 四对象接口与完整调用链审阅稿

2026-11-17 用户要求先展示 Session、Execution、Node、Runner 的接口定义，再展示它们串联的完整 Java 调用链，以便判断设计。对应文件为 [M2CallChain.java](M2CallChain.java)。

文件包含四个核心接口、所用传参和返回类型、DefaultRunner 实现以及 M2Entry 的 start、reply、cancel 和内部推进方法。M2Entry 连接对象调用，Execution.next 决定执行节点、等待输入或返回终态。Node 执行结果经 DefaultRunner 包装后交回 Execution.acceptNodeResult。正常完成或失败结束后，调用 Session.finishExecution。等待时向 M1 返回追问，后续通过 Session.currentExecution 找到原执行并调用 resume。

本稿统一 next 的返回契约，终态返回 Finish，等待返回 WaitForInput，运行期间根据规则返回 RunNode 或进入等待、结束。此前教学示例 next 只允许 RUNNING，该行为在本稿中调整，以便取消和等待状态也能通过统一结果表达。Session.finishExecution 须按已讨论的归属与终态校验及幂等要求实现。

接口实现方式仍待用户判断。Session、Execution 的内部规则实现以接口契约表达，源文件实现了 Runner 与调用串联。示例聚焦同步顺序调用及用户等待，实际并行调度、状态并发更新、持久化和取消在途调用等完整实现仍待设计。

所有 Java 传参类型均在同一文件定义，已通过 Java 17 编译检查。调用流程尚未运行，说明书尚未写入本轮接口方案。

## Runner 是否独立保留

2026-11-17 用户询问 Runner 是否仍有保留必要。结论状态为 AI 建议、待确认。

按 M2CallChain.java 当前职责，DefaultRunner 仅调用 Node.execute、转换调用异常、附加 Execution 与调用关联标识。节点选择和状态转换由 Execution 决定，推进循环及 Session 收尾由 M2Entry 执行。独立 Runner 对当前调用者隐藏的复杂性有限。

建议合并当前 Runner 的单次调用职责到 M2 接入执行流程，保留 Session、Execution、Node 三个核心对象接口，以及串接调用的 M2 入口。入口向 Execution 取得推进决定，直接执行 Node，将输出或错误包装成 NodeRunResult 交回 Execution。错误转换和调用标识继续保留，重试、分支、等待和终态仍由 Execution 的规则管理。

Runner 无状态这一属性本身不构成删除理由。本建议依据是现有 Runner 的职责与 M2 入口重叠、接口只承担少量转发。后续若出现独立的执行资源控制或多种执行实现，再根据实际契约判断是否抽取运行模块。

该建议调整此前已确认的四对象设计，需用户确认后更新说明书及相应 Java 审阅稿。当前保留原稿作为比较依据。

## §4.2.7 最新落稿审阅稿

2026-11-17 用户先要求查看本稿，随后回复“确认，请落稿”，并要求更新项目交接材料及推送工作分支。本稿及末尾关联修订已获确认并写入本分支说明书，包括合并 Runner 职责、十一类 NodeType 和节点调用集合。

### 拟写入 §4.2.7 的正文

本节细化 P-004、P-005、P-006、P-007。Session 管理会话及所属 Execution，Execution 保存本次规则、状态和运行信息，Node 完成具体处理。M2 入口串接对象调用，按 Execution 的推进决定调用 Node，将输出或错误交回 Execution，并完成会话收尾。原 Runner 的单次节点调用、错误转换及结果包装职责合并到 M2 入口。

以下接口名和字段名表达需求级契约，具体 Java 类型和传输协议留待实现设计。业务规则、节点配置和输入输出契约由场景版本提供。

##### 4.2.7.1 Session 接口

Session 保存 sessionId、会话归属、一次性或多轮模式、会话上下文、currentExecutionId 和有效期信息。一个 Session 可以先后包含多个 Execution，同时最多关联一个未结束的 Execution。

| 接口 | 输入与输出 | 契约 |
| --- | --- | --- |
| id() | 返回 sessionId | 标识当前会话。 |
| startExecution(sceneVersion, input) | 选定场景版本和本次输入，返回新 Execution | 在归属及有效期校验通过后，确认当前执行已结束，创建并关联新执行。固定本次场景版本，提供可继承上下文。当前执行未结束时返回忙时提示。 |
| currentExecution() | 返回当前未结束 Execution | 用于关联回复和取消。会话过期或当前执行已结束时，返回相应状态提示。 |
| finishExecution(execution) | 已确定终态的 Execution | 核对会话归属与当前执行关联，按终态更新会话上下文并释放当前执行关联。重复收尾保持已有结果，旧执行的通知不能影响新执行。 |

F-002 正常完成时更新可继承条件，失败或取消后沿用最近一次正常完成时确定的条件。一次性 Session 随当次 Execution 结束，多轮 Session 按 §4.2.3.6 的有效期规则保存上下文。

##### 4.2.7.2 Execution 接口

Execution 保存 executionId、sessionId、场景版本及规则引用、本次输入、状态、临时上下文、节点运行记录、待补信息、最终结果或错误。节点运行记录以 invocationId 区分同一个 Node 的各次运行。

| 接口 | 输入与输出 | 契约 |
| --- | --- | --- |
| id() / sessionId() | 返回执行或所属会话标识 | 用于执行归属与调用关联。 |
| next() | 返回 ExecutionDecision | 根据当前状态、运行结果和规则确定下一步。返回本次新确定的节点调用、等待信息或终态结果。 |
| acceptNodeResult(result) | NodeRunResult | 校验执行与调用标识，接收有效结果并更新节点运行信息。由本次规则决定后续处理及适用重试。重复或迟到结果维持已确定状态。 |
| resume(reply) | UserReply | 校验当前等待、追问关联及回复是否重复，保存有效回复并按续接位置继续。信息仍不足时保持 WAITING_INPUT。 |
| cancel() | 显式取消请求 | 接受取消后停止触发后续节点，按生命周期规则确认 CANCELLED。M2 入口按依赖能力通知在途调用。 |
| onTimeout(timeoutEvent) | 适用的超时事件 | 根据本次时限规则判断是否超时，成立时确定失败原因和终态。具体时限见后续运行参数设计。 |
| snapshot() | 返回 ExecutionSnapshot | 读取当前状态、追问或可获取的结果及结束原因。 |

ExecutionDecision 包含执行状态、本次可执行节点调用集合、待输入提示或终态结果。顺序执行通常返回一个调用，并行时可返回多个调用。RUNNING 且调用集合为空时，可能正在等待在途节点结果。WAITING_INPUT 返回当前追问，终态返回已确定的结果或结束原因。

节点调用被本次推进确定后，其调用标识和分发状态须与 Execution 状态一致记录，重复推进须避免重复提交同一次调用。需要等待在途节点时，在结果或其他有效事件到达后继续推进。

##### 4.2.7.3 Node 接口与调用数据

Node 定义包含场景版本内的节点标识、预定义类型、节点配置及输入输出契约。各次运行的状态保存在所属 Execution 中。

| 接口 | 输入与输出 | 契约 |
| --- | --- | --- |
| id() | 返回 nodeId | 唯一标识场景版本内的节点。 |
| type() | 返回 NodeType | 标识节点的预定义能力类型。 |
| execute(input, context) | NodeInput、NodeContext，返回 NodeResult | 按节点配置处理输入，返回输出、判断事实、待补信息或错误。 |

| 数据结构 | 主要内容 | 使用要求 |
| --- | --- | --- |
| NodeCall | 节点引用、输入、调用上下文 | 由 Execution 根据规则确定。 |
| NodeContext | executionId、invocationId、追踪标识、可信身份，以及适用时限和取消信号 | 由可信接入和运行环境建立，按节点需要提供。 |
| NodeResult | 正常输出、控制判断、待补条件或调用错误 | 由 Execution 按场景规则决定继续、等待、重试或结束。 |
| NodeRunResult | executionId、invocationId、NodeResult | M2 入口接收节点输出或异常后，关联到本次调用并交回 Execution。 |
| UserReply | 追问标识、回复标识、回复内容 | 服务端校验所属 Session 与当前等待关联。 |
| ExecutionSnapshot | executionId、状态、追问或结果、结束原因及已确定的可继承上下文 | 对外返回按调用方权限及场景输出契约组织，内部继承信息按会话更新需要使用。 |

模型、工具和知识等依赖能力分别通过 M3、M4、M5 提供。节点返回的判断事实供 Execution 应用规则，后续节点触发与 Execution 状态转换由 Execution 统一决定。

##### 4.2.7.4 Node 类型

| NodeType | 类型 | 处理职责 |
| --- | --- | --- |
| MODEL | 模型 | 通过 M3 调用模型并取得解析结果。 |
| TOOL | 工具 | 通过 M4 调用场景允许的工具。 |
| KNOWLEDGE_RETRIEVAL | 知识检索 | 通过 M5 检索知识内容与引用依据。 |
| DOCUMENT_PARSE | 文档解析 | 通过 M5 提取文档文本及结构。 |
| CODE | 代码处理 | 执行已注册的数据映射、计算或校验逻辑。 |
| CONDITION | 条件分支 | 计算条件并返回判断结果或分支键。 |
| PARALLEL | 并行分发 | 组织本次分发数据和候选分支信息。 |
| JOIN | 结果汇聚 | 对已满足汇聚条件的分支结果进行合并或转换。 |
| LOOP | 循环 | 计算本轮继续或退出条件，可按场景需要使用模型判断。 |
| AGENT | Agent 组合 | 按配置策略产生本轮行动建议或完成判断，ReAct 为其中一种策略。 |
| RETURN | 结果返回 | 整理符合场景输出契约的最终结果。 |

Execution 根据条件结果触发分支，维护并行分支状态并判断汇聚条件，维护循环轮次及退出规则，对 Agent 行动应用工具允许范围、调用限制与终止规则。具体并行失败处理、循环退出条件及 Agent 内部编排继续细化。

用户澄清通过待补信息与 WAITING_INPUT 表达。场景规则决定是否等待用户，F-001、F-003 沿用一次性处理范围，F-002 支持关联回复续接。

##### 4.2.7.5 M2 入口与完整调用链

M2 入口提供 start、reply、cancel 操作，分别用于新调用、当前追问回复和显式取消。取得 Session 及校验会话归属后，按以下过程串接对象方法。

1. 新调用通过 Session.startExecution 创建 Execution。当前追问的回复通过 Session.currentExecution 取得原执行并调用 resume。取消请求作用于当前 Execution 的 cancel。
2. 入口调用 Execution.next，取得推进决定。
3. 存在可执行节点时，入口调用对应 Node.execute，将输出或调用错误包装成 NodeRunResult，交给 Execution.acceptNodeResult。Execution 保存运行信息并按规则决定后续处理。
4. 等待在途节点时，在调用结果到达后继续推进。需要用户输入时，通过 M1 返回追问，Session 继续关联当前 Execution。后续有效回复续接同一执行。
5. Execution 确定终态后，入口调用 Session.finishExecution，更新会话关联及适用的继承条件，并通过 M1 返回结果或结束原因。

同一 Session 的并发新调用须遵守单个未结束 Execution 的约束。同一次节点结果和同一回复只触发一次有效处理。取消、超时和完成并发到达时，同一 Execution 只确定一次终态及对应结果，终态后的迟到结果保持已确定状态。

本节描述对象调用契约。具体接口路径、数据类型、缓存一致性实现、运行参数、终态结果保留与清理方式留待后续设计。SSE 与执行状态及会话有效期遵循 §4.2.3。

### 与本稿一并审阅的关联修订

为使正文一致，落稿时需同时调整以下位置，具体文字如下。

| 位置 | 拟调整内容 |
| --- | --- |
| §3.1.2 P-005 | 将“Runner 负责执行节点”改为“M2 入口执行节点并将结果交回 Execution”。 |
| §3.1.2 节点类型列表 | 将“十类”改为“十一类”，在“结果汇聚”后增加“循环”，完整列表与 §4.2.7.4 一致。 |
| §4.2.1 | 将 Runner 职责句改为“M2 入口按 Execution 确定的节点及输入执行调用，将输出或错误交回 Execution”。 |
| §4.2.2 | 删除 Runner 行，紧接核心对象关系段补充“M2 入口串接 Session、Execution 和 Node 的方法调用，执行节点并交回结果，具体接口与调用链见 §4.2.7”。 |
| §4.2.3 引导段 | 将末句改为“M2 入口按 Execution 确定的可执行节点执行调用并交回结果”。 |
| §4.2.3.2 | 将“Runner 执行节点并交回结果”改为“M2 入口执行节点并交回结果”。 |
| §4.2.3.3 | 将“由 Runner 执行”改为“由 M2 入口执行”。 |
| §4.2.4 引导段 | 将“Runner 执行节点”改为“M2 入口执行节点并回传结果”。 |
| §4.2.4 第 2、3 步 | 分别将“交给 Runner 执行”改为“交给 M2 入口执行”，将“Runner 将节点输出或调用错误交回 Execution”改为“M2 入口将节点输出或调用错误交回 Execution”。 |
| §4.2.6 F-001 段 | 将末句改为“M2 入口执行各次已满足触发条件的 Node”。 |

本轮获确认的细化包括十一类 NodeType，以及将顺序 Java 示例的单节点推进结果扩展为节点调用集合并表达等待在途结果。上述内容已随本稿落入说明书。
