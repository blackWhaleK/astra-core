import java.util.Map;

/**
 * Java 17 接口与调用链审阅稿。
 * 阶段记录：独立 Runner 与单节点推进已由说明书 §4.2.7 的新契约调整。
 * 四个核心接口、传参类型、Runner 实现和 M2 接入调用链均在本文件。
 * Session、Execution 的内部规则实现通过接口表达。
 * 本稿聚焦同步顺序调用与等待续接，接口和 NodeType 划分待确认。
 */
public class M2CallChain {
    public interface Session {
        String id();

        // 校验当前执行已结束，创建新执行并固定场景版本。
        Execution startExecution(SceneVersion scene, Map<String, Object> input);

        Execution currentExecution();

        // 核对执行归属和终态，按完成状态更新继承条件并释放当前关联。
        void finishExecution(Execution execution);
    }

    public interface Execution {
        String id();
        String sessionId();

        // 根据本次场景规则、节点结果及当前状态产生推进决定。
        // 已结束时返回 Finish，等待时返回 WaitForInput。
        Decision next();

        // 核对调用关联和重复结果，保存节点输出并应用执行规则。
        void acceptNodeResult(NodeRunResult result);

        // 核对追问与回复，继续同一 Execution。
        void resume(UserReply reply);

        void cancel();

        ExecutionSnapshot snapshot();
    }

    public interface Node {
        String id();
        NodeType type();
        NodeResult execute(Map<String, Object> input, NodeContext context);
    }

    public interface Runner {
        NodeRunResult run(NodeCall call);
    }

    public enum State {
        RUNNING, WAITING_INPUT, SUCCEEDED, FAILED, CANCELLED
    }

    // 前轮提出的十一类候选类型，等待用户确认。
    public enum NodeType {
        MODEL, TOOL, KNOWLEDGE_RETRIEVAL, DOCUMENT_PARSE, CODE,
        CONDITION, PARALLEL, JOIN, LOOP, AGENT, RETURN
    }

    public enum NodeOutcome {
        SUCCESS, NEED_INPUT, ERROR
    }

    public record SceneVersion(String sceneId, String version) { }

    public record UserReply(
        String promptId, String replyId, Map<String, Object> values
    ) { }

    public record UserPrompt(String promptId, String message) { }

    // 由接入处理建立并验证可信身份，再按节点需要传递。
    public record TrustedIdentity(String subjectId) { }

    public record NodeContext(
        String executionId, String invocationId, String traceId,
        TrustedIdentity identity
    ) { }

    public record NodeCall(
        Node node, Map<String, Object> input, NodeContext context
    ) { }

    // NEED_INPUT 携带所需条件，Execution 按场景规则生成追问或业务提示。
    public record NodeResult(
        NodeOutcome outcome, Map<String, Object> output,
        String message
    ) { }

    public record NodeRunResult(
        String executionId, String invocationId, NodeResult result
    ) { }

    public record ExecutionSnapshot(
        String executionId, State state, Map<String, Object> result,
        Map<String, Object> inheritableContext
    ) { }

    public sealed interface Decision permits RunNode, WaitForInput, Finish { }
    public record RunNode(NodeCall call) implements Decision { }
    public record WaitForInput(UserPrompt prompt) implements Decision { }
    public record Finish(ExecutionSnapshot snapshot) implements Decision { }

    // Runner 无执行进度字段，单次调用的数据全部由 NodeCall 提供。
    public static class DefaultRunner implements Runner {
        @Override
        public NodeRunResult run(NodeCall call) {
            NodeResult result;
            try {
                result = call.node().execute(call.input(), call.context());
            } catch (RuntimeException error) {
                result = new NodeResult(
                    NodeOutcome.ERROR, Map.of(), "节点调用异常"
                );
            }
            return new NodeRunResult(
                call.context().executionId(),
                call.context().invocationId(),
                result
            );
        }
    }

    public record Response(
        String sessionId, String executionId, State state,
        UserPrompt prompt, Map<String, Object> result
    ) { }

    // M2 的调用入口，持有可复用 Runner，Execution 状态保存在执行对象中。
    // Session 的取得及归属校验发生在进入以下方法之前。
    public static class M2Entry {
        private final Runner runner;

        public M2Entry(Runner runner) {
            this.runner = runner;
        }

        public Response start(
            Session session, SceneVersion scene, Map<String, Object> input
        ) {
            Execution execution = session.startExecution(scene, input);
            return advance(session, execution);
        }

        public Response reply(Session session, UserReply reply) {
            Execution execution = session.currentExecution();
            execution.resume(reply);
            return advance(session, execution);
        }

        public Response cancel(Session session) {
            Execution execution = session.currentExecution();
            execution.cancel();
            return advance(session, execution);
        }

        private Response advance(Session session, Execution execution) {
            while (true) {
                Decision decision = execution.next();

                if (decision instanceof RunNode runNode) {
                    NodeRunResult result = runner.run(runNode.call());
                    execution.acceptNodeResult(result);
                } else if (decision instanceof WaitForInput waiting) {
                    return new Response(
                        session.id(), execution.id(), State.WAITING_INPUT,
                        waiting.prompt(), Map.of()
                    );
                } else if (decision instanceof Finish finished) {
                    session.finishExecution(execution);
                    return new Response(
                        session.id(), execution.id(), finished.snapshot().state(),
                        null, finished.snapshot().result()
                    );
                }
            }
        }
    }

    // 展示 M1 持有同一个有效 Session 时的调用顺序。
    // 示例约定首问触发一次口径选择，口径A为该示例中的有效选项。
    public static void showQueryCalls(Session session, SceneVersion scene) {
        M2Entry entry = new M2Entry(new DefaultRunner());

        Response first = entry.start(
            session, scene, Map.of("question", "本月福州分行离职率")
        );

        if (first.state() != State.WAITING_INPUT) {
            return;
        }

        Response completed = entry.reply(session, new UserReply(
            first.prompt().promptId(), "reply-001", Map.of("metric", "口径A")
        ));

        if (completed.state() == State.SUCCEEDED) {
            entry.start(session, scene, Map.of("question", "去年同期呢"));
        }
    }
}
