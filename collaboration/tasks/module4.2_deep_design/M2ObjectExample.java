import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 对象协作教学示例，适用 Java 17。
 * 阶段记录：独立 Runner 与顺序推进已由说明书 §4.2.7 的新契约调整。
 * 节点使用固定返回值，输入输出用 String 简化。
 * 展示顺序执行、Session 归属、查询条件继承和等待输入续接。
 * 缓存、鉴权、并发、调用重试、取消和超时的完整契约留待设计。
 */
public class M2ObjectExample {
    enum Scene { F001, F002, F003 }
    enum State { RUNNING, WAITING_INPUT, SUCCEEDED, FAILED, CANCELLED }

    @FunctionalInterface
    interface Node {
        String execute(String input);
    }

    // 三种推进结果只是方法返回值，用于表达执行、等待或完成。
    sealed interface Decision permits NodeCall, NeedInput, Completed { }
    record NodeCall(String name, Node node, String input) implements Decision { }
    record NeedInput(String prompt) implements Decision { }
    record Completed(String result) implements Decision { }

    static class Runner {
        String run(NodeCall call) {
            System.out.println("执行 Node：" + call.name());
            return call.node().execute(call.input());
        }
    }

    static class Session {
        final String id = UUID.randomUUID().toString();
        private final boolean multiTurn;
        private boolean closed;
        private Execution current;
        private String confirmedMetric;

        Session(boolean multiTurn) {
            this.multiTurn = multiTurn;
        }

        Execution startExecution(Scene scene, String input) {
            if (closed) {
                throw new IllegalStateException("会话已结束");
            }
            if (current != null) {
                throw new IllegalStateException("请先结束当前执行");
            }
            current = new Execution(id, scene, input, confirmedMetric);
            return current;
        }

        Execution currentExecution() {
            if (current == null) {
                throw new IllegalStateException("当前没有执行");
            }
            return current;
        }

        void finishExecution(Execution execution) {
            if (current != execution) {
                return;
            }
            if (!execution.isTerminal()) {
                throw new IllegalStateException("执行尚未结束");
            }
            if (execution.state == State.SUCCEEDED && execution.scene == Scene.F002) {
                confirmedMetric = execution.selectedMetric;
            }
            current = null;
            closed = !multiTurn;
        }
    }

    static class Execution {
        final String id = UUID.randomUUID().toString();
        final String sessionId;
        private final Scene scene;
        private final String input;
        private State state = State.RUNNING;
        private int step;
        private String lastOutput;
        private String selectedMetric;
        private final Map<String, String> nodeOutputs = new HashMap<>();

        Execution(String sessionId, Scene scene, String input, String inheritedMetric) {
            this.sessionId = sessionId;
            this.scene = scene;
            this.input = input;
            this.selectedMetric = inheritedMetric;
        }

        // 本示例把三个场景的规则写在方法里，便于阅读。
        // 正式设计中规则来自启动时固定的场景版本。
        Decision next() {
            if (state != State.RUNNING) {
                throw new IllegalStateException("当前状态不能推进");
            }
            return switch (scene) {
                case F001 -> leaveRules();
                case F002 -> queryRules();
                case F003 -> reportRules();
            };
        }

        // F-001 选取本人请假的顺序流程，使用模拟提取结果和余额。
        private Decision leaveRules() {
            return switch (step) {
                case 0 -> new NodeCall("提取请假参数", text -> "请假1天", input);
                case 1 -> new NodeCall("查询本人余额", text -> "余额满足", lastOutput);
                case 2 -> {
                    if (!"余额满足".equals(lastOutput)) {
                        yield complete("剩余请假余额不足");
                    }
                    yield new NodeCall("生成页面数据", text -> "模拟请假页面数据", nodeOutputs.get("提取请假参数"));
                }
                case 3 -> complete(lastOutput);
                default -> throw new IllegalStateException("无效执行位置");
            };
        }

        // F-002 展示等待口径选择，以及下一次 Execution 继承口径。
        private Decision queryRules() {
            return switch (step) {
                case 0 -> new NodeCall("理解问题并检索口径", text -> "存在口径A和口径B", input);
                case 1 -> {
                    if (selectedMetric == null) {
                        state = State.WAITING_INPUT;
                        yield new NeedInput("请选择口径A或口径B");
                    }
                    yield new NodeCall("查询业务数据", text -> "模拟查询数据", input + selectedMetric);
                }
                case 2 -> new NodeCall("组织分析结果", text -> "模拟分析结果", lastOutput);
                case 3 -> complete(lastOutput);
                default -> throw new IllegalStateException("无效执行位置");
            };
        }

        // F-003 展示一次上传到检查结果的顺序流程。
        private Decision reportRules() {
            return switch (step) {
                case 0 -> new NodeCall("解析Word", file -> "模拟报告正文", input);
                case 1 -> new NodeCall("检索制度", text -> "模拟适用制度", lastOutput);
                case 2 -> new NodeCall("逐项检查", text -> "模拟逐项检查意见",
                    nodeOutputs.get("解析Word") + nodeOutputs.get("检索制度"));
                case 3 -> new NodeCall("汇总结果", text -> "模拟总体意见及修改建议", lastOutput);
                case 4 -> complete(lastOutput);
                default -> throw new IllegalStateException("无效执行位置");
            };
        }

        void acceptNodeResult(String nodeName, String output) {
            if (state != State.RUNNING) {
                throw new IllegalStateException("当前状态不能接收节点结果");
            }
            lastOutput = output;
            nodeOutputs.put(nodeName, output);
            step++;
        }

        void resume(String reply) {
            if (state != State.WAITING_INPUT) {
                throw new IllegalStateException("当前没有等待用户输入");
            }
            if (!"口径A".equals(reply) && !"口径B".equals(reply)) {
                throw new IllegalArgumentException("请选择口径A或口径B");
            }
            selectedMetric = reply;
            state = State.RUNNING;
            // step 仍为 1，从原来的等待位置继续。
        }

        private Completed complete(String result) {
            state = State.SUCCEEDED;
            return new Completed(result);
        }

        void fail() {
            state = State.FAILED;
        }

        boolean isTerminal() {
            return state == State.SUCCEEDED || state == State.FAILED || state == State.CANCELLED;
        }
    }

    // M2 接入流程。循环连接对象调用，具体节点选择在 Execution.next() 中。
    static void drive(Session session, Execution execution, Runner runner) {
        while (execution.state == State.RUNNING) {
            Decision decision = execution.next();

            if (decision instanceof NodeCall call) {
                try {
                    String output = runner.run(call);
                    execution.acceptNodeResult(call.name(), output);
                } catch (RuntimeException error) {
                    execution.fail();
                    session.finishExecution(execution);
                    System.out.println("节点调用失败");
                    return;
                }
            } else if (decision instanceof NeedInput needInput) {
                System.out.println(needInput.prompt());
                return;
            } else if (decision instanceof Completed completed) {
                session.finishExecution(execution);
                System.out.println(completed.result());
                return;
            }
        }
    }

    // 用于阅读调用次序。本次仅进行编译检查。
    public static void illustrateThreeScenes() {
        Runner runner = new Runner();

        Session leaveSession = new Session(false);
        Execution leave = leaveSession.startExecution(Scene.F001, "我要请假1天");
        drive(leaveSession, leave, runner);

        Session querySession = new Session(true);
        Execution firstQuery = querySession.startExecution(Scene.F002, "查询本月离职率");
        drive(querySession, firstQuery, runner); // 在选择口径处返回。

        Execution waitingQuery = querySession.currentExecution();
        waitingQuery.resume("口径A");
        drive(querySession, waitingQuery, runner); // 继续同一个 Execution。

        Execution nextQuery = querySession.startExecution(Scene.F002, "去年同期呢");
        drive(querySession, nextQuery, runner); // 同一 Session 下的新 Execution。

        Session reportSession = new Session(false);
        Execution report = reportSession.startExecution(Scene.F003, "报告临时引用");
        drive(reportSession, report, runner);
    }
}
