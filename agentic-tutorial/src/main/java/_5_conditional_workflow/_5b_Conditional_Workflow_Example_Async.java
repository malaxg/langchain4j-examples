package _5_conditional_workflow;

import _4_parallel_workflow.ManagerCvReviewer;
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
 * 示例主类（5b）：演示【异步(async) + 多分支的条件工作流】。
 *
 * <p>与 5a 的区别：
 * - 这里演示“多个条件同时成立”的情况；
 * - Agent 通过 .async(true) 声明为异步，这样被触发的多个分支 Agent
 *   可以【并行】执行，从而加快整体运行。
 *
 * <p>三个分支：
 * - 条件1：如果 HR 评审良好（score>=0.8），把简历交给管理评审员(ManagerCvReviewer)进一步评审；
 * - 条件2：如果 HR 评审不佳（score<0.8），发送拒绝邮件(EmailAssistant)；
 * - 条件3：如果 HR 反馈提示缺失信息（feedback 含 "missing information:"），
 *   联系候选人索取更多信息(InfoRequester)。
 *
 * <p>核心编排概念（异步/状态机）：
 * - .async(true)：把 Agent 声明为异步，使其可在条件工作流中并行执行；
 * - conditionalBuilder().subAgents(条件, Agent)...：多分支条件路由；
 * - .output(...)：输出适配器回调，读取各分支产生的变量拼成最终输出；
 * - scope.readState(key, 默认值)：读取状态时提供缺省值，避免某个分支未执行时抛异常。
 */
public class _5b_Conditional_Workflow_Example_Async {

    // 设置日志级别
    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 150);
    }

    /**
     * 本示例演示“多个条件同时成立”以及“异步 agent”，
     * 异步允许被触发的多个连续 agent 并行执行以加快速度。
     * 在本例中：
     * - 条件 1：如果 HR 评审良好，简历交给经理评审；
     * - 条件 2：如果 HR 评审提示缺失信息，则联系候选人补信息。
     */

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 1. 创建所有异步 Agent
        ManagerCvReviewer managerCvReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .async(true) // 声明为异步 Agent
                .outputKey("managerReview")
                .build();
        EmailAssistant emailAssistant = AgenticServices.agentBuilder(EmailAssistant.class)
                .chatModel(CHAT_MODEL)
                .async(true) // 异步
                .tools(new OrganizingTools())
                .outputKey("sentEmailId")
                .build();
        InfoRequester infoRequester = AgenticServices.agentBuilder(InfoRequester.class)
                .chatModel(CHAT_MODEL)
                .async(true) // 异步
                .tools(new OrganizingTools())
                .outputKey("sentEmailId")
                .build();

        // 2. 构建异步条件工作流
        UntypedAgent candidateResponder = AgenticServices
                .conditionalBuilder() // 条件工作流构建器
                .subAgents(scope -> { // 条件1：HR 评审通过则交经理进一步评审
                    CvReview hrReview = (CvReview) scope.readState("cvReview");
                    return hrReview.score >= 0.8;
                }, managerCvReviewer)
                .subAgents(scope -> { // 条件2：HR 评审不佳则发送拒绝邮件
                    CvReview hrReview = (CvReview) scope.readState("cvReview");
                    return hrReview.score < 0.8;
                }, emailAssistant)
                .subAgents(scope -> { // 条件3：若缺少信息则联系候选人补信息
                    CvReview hrReview = (CvReview) scope.readState("cvReview");
                    return hrReview.feedback.toLowerCase().contains("missing information:"); // 判断反馈中是否出现缺失信息标记
                }, infoRequester)
                .output(agenticScope -> // 输出适配器回调：拼装最终输出
                        (agenticScope.readState("managerReview", new CvReview(0, "无需经理评审"))).toString() + // 若该分支未执行则用默认值
                                "\n" + agenticScope.readState("sentEmailId", 0) // 若未发邮件则默认 0
                ) // 最终输出 = 经理评审（如存在）等信息
                .build();

        // 3. 输入参数
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        CvReview hrReview = new CvReview(
                0.85,
                """
                        Solid candidate, salary expectations in scope and able to start within desired timeframe.
                        Missing information: details about work authorization status in Belgium.
                        """ // 注意：此文本中的 "Missing information:" 是下方条件3用 contains() 匹配的关键字，因此刻意保留英文
        );

        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "jobDescription", jobDescription,
                "cvReview", hrReview
        );

        // 4. 运行异步条件工作流
        candidateResponder.invoke(arguments);

        System.out.println("=== 异步条件工作流执行完毕 ===");
    }
}
