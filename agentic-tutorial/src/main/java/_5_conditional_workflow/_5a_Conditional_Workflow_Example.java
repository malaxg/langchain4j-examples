package _5_conditional_workflow;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import domain.CvReview;
import util.ChatModelProvider;
import util.StringLoader;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.IOException;
import java.util.Map;

/**
 * 示例主类（5a）：演示【条件工作流(Conditional workflow)】。
 *
 * <p>与 4x（并行）的区别：本示例根据“评审分数 + 候选人档案”做【条件判断】，
 * 决定调用哪个分支 Agent：
 * - 分数达标 → 调用 InterviewOrganizer，为候选人安排现场面试；
 * - 分数不达标 → 调用 EmailAssistant，发一封友善的“不继续推进”邮件。
 *
 * <p>核心编排概念：
 * - conditionalBuilder()：条件工作流构建器；
 * - .subAgents(条件Lambda, 分支Agent)：为每个分支指定“触发条件 + 要执行的 Agent”；
 *   conditionalBuilder 会依次检查每个条件，满足则执行对应 Agent；
 *   （多个条件都被满足时，会按顺序依次执行；若要并行，用 async 的 _5b）
 * - 条件 Lambda 内部通过 agenticScope.readState("cvReview") 读取评审结果做判断；
 * - 为 Agent 挂 .tools(...)（工具）和 .contentRetriever(...)（RAG）以扩展能力。
 */
public class _5a_Conditional_Workflow_Example {

    // 设置日志级别
    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 200);
    }

    /**
     * 本示例演示条件 Agent 工作流。
     * 根据评分和候选人档案，我们或者：
     * - 调用一个 Agent，为候选人的现场面试做好一切准备；
     * - 调用一个 Agent，发送一封友善的“我们不再推进”邮件。
     */

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 2. 在本包中定义两个子 Agent 接口：
        //      - EmailAssistant.java（拒绝邮件）
        //      - InterviewOrganizer.java（安排面试）

        // 3. 用 AgenticServices 创建所有 Agent
        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(CHAT_MODEL)
                .tools(new OrganizingTools()) // 该 Agent 可以使用其中定义的所有工具
                .build();
        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(CHAT_MODEL)
                .tools(new OrganizingTools())                 // 可用工具
                .contentRetriever(RagProvider.loadHouseRulesRetriever()) // 通过这种方式给 Agent 加 RAG 能力
                .build();

        // 4. 构建条件工作流
        UntypedAgent candidateResponder = AgenticServices // 除非自定义类型化接口，否则用 UntypedAgent（见 _2_Sequential_Agent_Example）
                .conditionalBuilder()                                // 条件工作流构建器
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("cvReview")).score >= 0.8, interviewOrganizer) // 分支1：评分>=0.8 → 安排面试
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("cvReview")).score < 0.8, emailAssistant)        // 分支2：评分<0.8 → 发拒绝邮件
                .build();
        // 补充说明：当定义了多个条件时，它们会按顺序依次执行。
        // 如果希望这里并行执行，请使用异步 agent，参见 _5b_Conditional_Workflow_Example_Async

        // 5. 从 resources/documents/ 加载参数
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        CvReview cvReviewFail = new CvReview(0.6, "简历不错,但缺少与后端岗位相关的部分技术细节。");
        CvReview cvReviewPass = new CvReview(0.9, "简历非常优秀,完全符合后端岗位的所有要求。");

        // 5. 无类型 Agent 需用 Map 传所有输入参数
        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "jobDescription", jobDescription,
                "cvReview", cvReviewPass // 可改成 cvReviewFail 来观察另一个分支
        );

        // 5. 调用条件 Agent，根据评审结果做出符合预期的候选人回应
        candidateResponder.invoke(arguments);
        // 本示例没有对 AgenticScope 做有意义的修改，
        // 也没有有意义的输出可打印，因为最终的“动作”是由工具完成的。
        // 我们通过控制台观察工具执行了哪些动作（发送邮件、更新申请状态）。

        // 以调试模式观察日志时，工具调用的结果 'success' 仍会回传给模型，
        // 模型也仍会回答类似“已向 John Doe 发送邮件并告知他……”的内容。

        // 补充说明：如果工具是你的最后一步动作、不想再随后调用模型，
        // 通常会给该工具加 @Tool(returnBehavior = ReturnBehavior.IMMEDIATE)`
        // https://docs.langchain4j.dev/tutorials/tools#returning-immediately-the-result-of-a-tool-execution-request
        // !!! 但在 agentic 工作流中，不推荐对工具使用 IMMEDIATE RETURN BEHAVIOR，
        //     因为立即返回行为会把工具结果存入 AgenticScope，可能导致问题。

        // 补充说明：这只是“用代码检查条件来做路由”的示例。
        // 路由行为也可以让 LLM 来决定接下来调用哪个工具/Agent，方法包括：
        // - Supervisor agent（监管者）：作用于 Agent 之上，参见 _7_supervisor_orchestration
        // - 把 AiService 当作工具，如下：
        // RouterService routerService = AiServices.builder(RouterAgent.class)
        //        .chatModel(model)
        //        .tools(medicalExpert, legalExpert, technicalExpert)
        //        .build();
        //
        // 哪种方案最优取决于你的使用场景：
        //
        // - 条件(conditional) Agent：把调用条件硬编码
        // - 而 AiService 或 Supervisor：由 LLM 决定调用哪个专家
        //
        // - 用 agentic 方案（conditional、supervisor）：所有中间状态和调用链都存于 AgenticScope
        // - 而用 AiService 则较难追踪调用链或中间状态
    }
}
