# 任务 Handoff：rebuild_spec

- 负责人：blackWhale
- 分支：docs/blackWhale/rebuild_spec
- 来源 main 完整提交：351f321090ac6bc84560b264f5fbdce93f731a68
- 最近比较的 main：351f321090ac6bc84560b264f5fbdce93f731a68
- 任务目标与完成标准：范围 rebuild_spec_with_clear_semantics；清晰重建说明书的具体收口标准待确认；先讨论业务取舍，经用户确认再改候选说明书。不生成代码，不直接更新 main。
- 当前状态：讨论中
- 本任务允许读取材料：AGENTS.md、README.md、docs/baseline/specification.md、docs/handoff/current.md、collaboration/templates/、collaboration/tasks/rebuild_spec/；实际适用 Skill 可按规则追加。禁止默认读取历史归档、其他任务纪要与过程稿。

## 已完成与当前候选改动

已联网 fetch origin，从来源 main 创建任务分支；基于三份模板建立本任务材料。候选说明书无修改，项目 Handoff 无修改。RS-Q-001—003 仅为讨论标识，未成为正式需求或决策。
当前聊天此前仅进行克隆与 SSH 问题解释，无旧业务专题上下文；按本轮明确指令初始化。

## 检查与确认

来源主线已核对，起始工作区干净。材料中的相对链接：无；未新增或重排业务编号。已审阅三份材料完整差异，三类 diff --check 均通过。初始化材料已提交并成功推送同名远程分支。
用户只指定任务、范围与流程；具体业务取舍尚未确认。无业务实现或运行验证。

## 阻塞和未决事项

待用户判断 proposal.md 中 RS-Q-001—003。Git 配置作者为 kameco（现有本机配置），任务负责人不自动等于 Git 作者，不替用户改写作者身份。

## 下一步

先取得重建范围与讨论顺序的确认，再澄清具体业务语义，记录本任务纪要；只有用户明确同意落稿才修改候选说明书。初始化材料完成检查后提交并尝试推送同名工作分支；如上传失败保留本地提交并披露。
