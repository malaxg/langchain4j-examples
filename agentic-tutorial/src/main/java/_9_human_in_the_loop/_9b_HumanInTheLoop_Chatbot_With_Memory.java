package _9_human_in_the_loop;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.workflow.HumanInTheLoop;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import util.ChatModelProvider;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Scanner;

public class _9b_HumanInTheLoop_Chatbot_With_Memory {

    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);  // 控制从模型调用中可见的信息量
    }

    /**
     * 本示例演示"来回循环 + 人机协作"的用法：
     * AI 提议会议时间 → 人类（在控制台）回答是否有空 → 再提议 → …… 
     * 直到达到某个结束目标（退出条件），之后工作流的其余部分才继续执行。
     * 循环会一直进行，直到人类确认有空（由 DecisionsReachedService 这个 AiService 判定）。
     * 如果一直没找到合适时段，循环会在 5 轮之后结束。
     */

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) {

        // 1. 定义子 Agent：会议提议者
        MeetingProposer proposer = AgenticServices
                .agentBuilder(MeetingProposer.class)
                .chatModel(CHAT_MODEL)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(15)) // 让 Agent 记住它已经提议过哪些时段
                .outputKey("proposal")
                .build();

        // 2. 用 AiService 判断双方是否已达成一致（任务很简单，可选用小型本地模型）
        DecisionsReachedService decisionService = AiServices.create(DecisionsReachedService.class, CHAT_MODEL);

        // 2. 定义"人机协作" Agent
        HumanInTheLoop humanInTheLoop = AgenticServices
                .humanInTheLoopBuilder()
                .description("向用户索取输入的智能体")
                .outputKey("candidateAnswer") // 与提议者的某个输入变量名保持一致
                .responseProvider(scope -> {
                    // 打印给人类看的提议，并等待输入
                    System.out.println(scope.readState("request"));
                    System.out.print("> ");
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                        return reader.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException("读取输入失败", e);
                    }
                })
                .async(true) // 无需在等待用户输入时阻塞整个程序
                .build();

        // 3. 构造循环
        // 这里我们希望每个循环只检查一次退出条件（而不是每次 Agent 调用后都检查），
        // 所以把两个 Agent 打包成一个顺序组合，作为一个整体交给循环。
        UntypedAgent agentSequence = AgenticServices
                .sequenceBuilder()
                .subAgents(proposer, humanInTheLoop)
                .output(agenticScope -> Map.of(
                        "proposal", agenticScope.readState("proposal"),
                        "candidateAnswer", agenticScope.readState("candidateAnswer")
                ))
                .outputKey("proposalAndAnswer")
                // 这个输出包含了"最后一次会议提议 + 候选人的回答"，足以让后续 Agent
                // 安排会议（或放弃尝试）
                .build();

        UntypedAgent schedulingLoop = AgenticServices
                .loopBuilder()
                .subAgents(agentSequence)
                .exitCondition(scope -> {
                    // 退出条件：人类确认了有空（由 AiService 判定）
                    System.out.println("--- 正在检查退出条件 ---");
                    String response = (String) scope.readState("candidateAnswer");
                    String proposal = (String) scope.readState("proposal");
                    return response != null && decisionService.isDecisionReached(proposal, response);
                })
                .outputKey("proposalAndAnswer")
                .maxIterations(5)
                .build();

        // 4. 运行"安排会议"循环
        Map<String, Object> input = Map.of("meetingTopic", "现场参观", // meetingTopic 是会议主题
                "candidateAnswer", "hi", // 这个变量需要事先存在于 AgenticScope，因为 MeetingProposer 把它作为输入
                "memoryId", "user-1234"); // 若不提供 memoryId，提议者将无法记住它已经提议过什么

        var lastProposalAndAnswer = schedulingLoop.invoke(input);

        System.out.println("=== 结果：最后一次提议与回答 ===");
        System.out.println(lastProposalAndAnswer);
    }
}
