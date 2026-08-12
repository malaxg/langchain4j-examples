package _7_supervisor_orchestration;

import _4_parallel_workflow.HrCvReviewer;
import _4_parallel_workflow.ManagerCvReviewer;
import _4_parallel_workflow.TeamMemberCvReviewer;
import _5_conditional_workflow.EmailAssistant;
import _5_conditional_workflow.InterviewOrganizer;
import _5_conditional_workflow.OrganizingTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import util.ChatModelProvider;
import util.StringLoader;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 进阶监督者示例：显式使用 AgenticScope，以观察不断演化的上下文。
 */
public class _7b_Supervisor_Orchestration_Advanced {

    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 200);
    }

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    /**
     * 本示例构建了一个与 _7a_Supervisor_Orchestration 类似的监督者，
     * 但额外探索了 Supervisor 的一些高级特性：
     * - 类型化监督者（typed supervisor）：使用接口 + @Agent 定义入口
     * - 上下文工程（context engineering）
     * - 输出策略（output strategies）
     * - 调用链观察（call chain observation）
     * - 上下文演化观察（context evolution inspection）
     */
    public static void main(String[] args) throws IOException {

        // 1. 定义子 Agent（这里未显式设置 outputKey，使用接口默认定义）
        HrCvReviewer hrReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .build();
        ManagerCvReviewer managerReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .build();
        TeamMemberCvReviewer teamReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .build();
        InterviewOrganizer interviewOrganizer = AgenticServices.agentBuilder(InterviewOrganizer.class)
                .chatModel(CHAT_MODEL)
                .tools(new OrganizingTools())
                .outputKey("response")
                .build();
        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(CHAT_MODEL)
                .tools(new OrganizingTools())
                .outputKey("response")
                .build();

        // 2. 构建监督者

        HiringSupervisor hiringSupervisor = AgenticServices
                .supervisorBuilder(HiringSupervisor.class)
                .chatModel(CHAT_MODEL)
                .subAgents(hrReviewer, managerReviewer, teamReviewer, interviewOrganizer, emailAssistant)
                .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY_AND_SUMMARIZATION)
                // 根据监督者需要了解的子 Agent 情况，可选择 CHAT_MEMORY、SUMMARIZATION 或 CHAT_MEMORY_AND_SUMMARIZATION
                .responseStrategy(SupervisorResponseStrategy.SCORED) // 该策略用评分模型判断：最后一条响应还是摘要，哪个更能满足用户请求
                // 这里如果提供一个输出函数，则会覆盖上面的响应策略
                .supervisorContext("政策：先检查 HR，必要时升级，低匹配度直接拒绝。")
                .build();

        // 3. 加载输入数据
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");

        String request = "评估这位候选人，然后要么安排面试，要么发送拒绝邮件。\n"
                + "候选人简历：\n" + candidateCv + "\n"
                + "候选人联系方式：\n" + candidateContact + "\n"
                + "职位描述：\n" + jobDescription + "\n"
                + "HR 要求：\n" + hrRequirements + "\n"
                + "电话面试记录：\n" + phoneInterviewNotes;

        // 4. 调用监督者（第二个参数作为监督者上下文）
        long start = System.nanoTime();
        ResultWithAgenticScope<String> decision = hiringSupervisor.invoke(request, "经理的技术评审最重要。");
        long end = System.nanoTime();

        System.out.println("=== 招聘监督者完成，用时 " + ((end - start) / 1_000_000_000.0) + " 秒 ===");
        System.out.println(decision.result());

        // 打印收集到的上下文
        System.out.println("\n=== 以对话形式呈现的上下文 ===");
        System.out.println(decision.agenticScope().contextAsConversation()); // will work in next release

    }
}
