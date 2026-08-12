package _9_human_in_the_loop;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.model.chat.ChatModel;
import domain.CvReview;
import util.ChatModelProvider;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Scanner;

/**
 * 示例 9a：简单的人工验证（Human-in-the-loop）。
 * 演示"人机协作"的最简用法：AI 先根据 CvReview 提议一个招聘决定，
 * 然后由【人类】在控制台输入最终决定（邀请 / 拒绝 / 暂缓），作为工作流的最终输出。
 * 这样关键的、需要承担责任的决策由人来把关，而不是完全交给模型。
 */
public class _9a_HumanInTheLoop_Simple_Validator {

    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);
    }

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) {
        // 1. 创建参与流程的 Agent
        HiringDecisionProposer decisionProposer = AgenticServices.agentBuilder(HiringDecisionProposer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("modelDecision")
                .build();

        // 2. 定义"人类介入"节点：由人来验证模型提出的招聘决定
        HumanInTheLoop humanValidator = AgenticServices.humanInTheLoopBuilder()
                .description("验证模型提出的招聘决定")   // description 是发给 LLM 的描述
                .outputKey("finalDecision") // 该键由人类来确认/填写
                .responseProvider(scope -> {
                    // 在控制台打印给人类看的信息（人机交互提示文本）
                    System.out.println("AI 招聘助手建议: " + scope.readState("request"));
                    System.out.println("请确认最终决定。");
                    System.out.println("选项：邀请到现场（I）、拒绝（R）、暂缓（H）");
                    System.out.print("> "); // 真实系统中需要做输入校验和容错处理
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                        return reader.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("读取输入失败", e);
                    }
                })
                .build();

        // 3. 把 Agent 串成工作流：先提议，再交给人类验证
        UntypedAgent hiringDecisionWorkflow = AgenticServices.sequenceBuilder()
                .subAgents(decisionProposer, humanValidator)
                .outputKey("finalDecision")
                .build();

        // 4. 准备输入参数（这里是评审反馈数据）
        Map<String, Object> input = Map.of(
                "cvReview", new CvReview(0.85,
                        """
                                技术能力很强，但缺少所要求的 React 经验。
                                不过看起来学得又快又独立。文化契合度不错。
                                工作许可可能存在一点问题，但看起来可解决。
                                薪资预期略超预算。
                                决定推进到现场面试。
                                """)
        );

        // 5. 运行工作流（会在控制台等待人类输入）
        String finalDecision = (String) hiringDecisionWorkflow.invoke(input);

        System.out.println("\n=== 人类给出的最终决定 ===");
        System.out.println("(邀请到现场（I）、拒绝（R）、暂缓（H）)\n");
        System.out.println(finalDecision);

        // 注意：人机协作/人工验证通常需要较长时间等待用户响应。
        // 这种情况下推荐使用异步 Agent，这样它们不会阻塞工作流的其余部分，
        // 其余部分可以在用户回答到来之前先执行。
    }
}
