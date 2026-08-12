package _7_supervisor_orchestration;

import _4_parallel_workflow.HrCvReviewer;
import _4_parallel_workflow.ManagerCvReviewer;
import _4_parallel_workflow.TeamMemberCvReviewer;
import _5_conditional_workflow.EmailAssistant;
import _5_conditional_workflow.InterviewOrganizer;
import _5_conditional_workflow.OrganizingTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.model.chat.ChatModel;
import util.ChatModelProvider;
import util.StringLoader;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.IOException;

/**
 * 至此我们构建的都是"确定性工作流"：
 * - 顺序、并行、条件、循环，以及它们的组合。
 * 你还可以构建"监督者式（Supervisor）智能体系统"：由一个 Agent 动态决定
 * 接下来该调用哪些子 Agent、以什么顺序调用。
 * 在本示例中，监督者负责调度整个招聘流程：运行 HR/经理/团队评审，
 * 然后要么安排面试，要么发送拒绝邮件。
 * 这相当于"组合工作流"示例的第 2 部分，但这次是"自组织"完成的。
 * 注意：监督者这类超级 Agent 也可以像其它超级 Agent 一样被用在组合工作流中。
 * 重要：本示例用 GPT-4o-mini 运行约需 50 秒，PRETTY 日志会持续显示当前进展。
 * 关于如何加速执行，见本文件结尾的注释。
 */
public class _7a_Supervisor_Orchestration {

    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 200);  // 控制从模型调用中可见的信息量
    }

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 1. 定义所有子 Agent
        HrCvReviewer hrReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("hrReview")
                .build();
        // 重要：如果多个 Agent 使用相同的方法名（这里所有评审者都叫 'reviewCv'），
        // 最好给每个 Agent 起一个明确的名字，例如：
        // @Agent(name = "managerReviewer", description = "根据职位描述评审一份简历，给出反馈和打分")

        ManagerCvReviewer managerReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("managerReview")
                .build();

        TeamMemberCvReviewer teamReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("teamMemberReview")
                .build();

        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(CHAT_MODEL)
                .tools(new OrganizingTools())
                .build();

        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(CHAT_MODEL)
                .tools(new OrganizingTools())
                .build();

        // 2. 构建监督者 Agent
        SupervisorAgent hiringSupervisor = AgenticServices.supervisorBuilder()
                .chatModel(CHAT_MODEL)
                .subAgents(hrReviewer, managerReviewer, teamReviewer, interviewOrganizer, emailAssistant)
                // 上下文生成策略：保留聊天记忆并做摘要（让监督者了解子 Agent 都做了什么）
                .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY_AND_SUMMARIZATION)
                // 响应策略：我们希望得到"发生了什么"的摘要，而不是直接回传某个子 Agent 的结果
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                // 给监督者的可选"行为上下文"提示，指导它如何行事（这是发给 LLM 的指令）
                .supervisorContext("务必使用我们提供的全部评审小组。务必用英语回答。调用 Agent 时使用纯 JSON（不要反引号，换行用反斜杠+n 表示）。")
                .build();
        // 关键点：监督者一次只调用 1 个 Agent，调用后会重新审视自己的计划，再决定下一个调用谁。
        // 监督者无法让多个 Agent 并行执行。
        // 如果某个 Agent 被标记为异步，监督者会强制改为同步（不并发）并给出警告。

        // 3. 加载候选人简历与职位描述
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");

        // 启动计时器
        long start = System.nanoTime();
        // 4. 用一句自然语言请求调用监督者
        String result = (String) hiringSupervisor.invoke(
                "请评估下面的候选人：\n" +
                        "候选人简历：\n" + candidateCv + "\n\n" +
                        "候选人联系方式：\n" + candidateContact + "\n\n" +
                        "职位描述：\n" + jobDescription + "\n\n" +
                        "HR 要求：\n" + hrRequirements + "\n\n" +
                        "电话面试记录：\n" + phoneInterviewNotes
        );
        long end = System.nanoTime();
        double elapsedSeconds = (end - start) / 1_000_000_000.0;
        // 在日志里你会看到最后一次调用了名为 'done' 的 Agent，这就是监督者结束一次调用序列的方式

        System.out.println("=== 监督者运行完成，用时 " + elapsedSeconds + " 秒 ===");
        System.out.println(result);
    }

    // 进阶用法：
    // 参见 _7b_Supervisor_Orchestration_Advanced.java，其中包含：
    // - 类型化监督者（typed supervisor）
    // - 上下文工程（context engineering）
    // - 输出策略（output strategies）
    // - 调用链观察（call chain observation）

    // 关于延迟：
    // 整个流程通常要 60 秒以上。
    // 一种解决方案是使用像 CEREBRAS 这样的快速推理提供商，
    // 它能在 10 秒内跑完整个流程，但会更容易出错。
    // 想用 CEREBRAS 体验本示例：去获取 key（点击 get started 领取免费 API key）
    // https://inference-docs.cerebras.ai/quickstart
    // 并保存到环境变量 "CEREBRAS_API_KEY"
    // 然后把第 38 行改为：
    // private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel("CEREBRAS");

}
