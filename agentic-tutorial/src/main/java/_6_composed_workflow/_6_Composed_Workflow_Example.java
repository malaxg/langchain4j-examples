package _6_composed_workflow;

import _1_basic_agent.CvGenerator;
import _3_loop_workflow.CvReviewer;
import _3_loop_workflow.ScoredCvTailor;
import _4_parallel_workflow.HrCvReviewer;
import _4_parallel_workflow.ManagerCvReviewer;
import _4_parallel_workflow.TeamMemberCvReviewer;
import _5_conditional_workflow.EmailAssistant;
import _5_conditional_workflow.InterviewOrganizer;
import _5_conditional_workflow.OrganizingTools;
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
import java.util.concurrent.Executors;

/**
 * 示例：组合工作流（Composed Workflow）。
 * 本示例把前面教程里做过的各类小 Agent（顺序、并行、循环、条件等）任意组合成两个大的"超级 Agent"，
 * 来编排一个完整的招聘流程：
 *   1) 候选人流程（CANDIDATE WORKFLOW）：人生经历 → 生成简历 → 评审 → 循环改进 → 发邮件。
 *   2) 招聘团队流程（HIRING TEAM WORKFLOW）：三路并行评审 → 汇总 → 条件决策（安排面试 / 发拒信）。
 * 核心要点：所有 Agent 底层都是同一个 Agent 对象，因此天然可组合、可嵌套，
 * 无论嵌套多少层，所有参数都存放在同一个共享的 AgenticScope 中。
 */
public class _6_Composed_Workflow_Example {

    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);  // 控制从模型调用中可见的信息量
    }

    /**
     * 每个 Agent——无论是单个任务的 Agent，还是顺序工作流等——在本质上都仍然是一个 Agent 对象。
     * 这使得 Agent 完全可以组合：
     * - 可以把小 Agent 打包成超级 Agent
     * - 可以用子 Agent 分解大任务
     * - 可以在任意层级混合使用顺序、并行、循环、监督者等工作流
     * 在本示例中，我们将前面构建好的组合 Agent（顺序、并行等）进一步组合成两个更大的
     * 组合 Agent，用来编排整个应聘流程。
     */

    // 1. 定义驱动所有 Agent 的大模型
    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        ////////////////// CANDIDATE COMPOSED WORKFLOW //////////////////////
        // 流程：人生经历 > 简历 > 评审 > 评审循环（直到达标）> 把简历发给公司

        // 1. 创建候选人流程所需的全部 Agent
        // 简历生成器：负责根据人生经历生成初版简历（输出到 AgenticScope 的 "cv" 键）
        CvGenerator cvGenerator = AgenticServices
                .agentBuilder(CvGenerator.class)
                .chatModel(CHAT_MODEL)
                .outputKey("cv")
                .build();

        // 带评分的简历定制器：根据职位描述进一步定制简历
        ScoredCvTailor scoredCvTailor = AgenticServices
                .agentBuilder(ScoredCvTailor.class)
                .chatModel(CHAT_MODEL)
                .outputKey("cv")
                .build();

        // 简历评审器：对简历打分并给出反馈（输出到 "cvReview" 键）
        CvReviewer cvReviewer = AgenticServices
                .agentBuilder(CvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("cvReview")
                .build();

        // 2. 创建简历改进的循环工作流（loop）：定制 → 评审 → 不达标则继续
        UntypedAgent cvImprovementLoop = AgenticServices
                .loopBuilder()
                .subAgents(scoredCvTailor, cvReviewer)   // 循环体内：先定制再评审
                .outputKey("cv")
                // 退出条件：评分 >= 0.8 即视为合格，退出循环
                .exitCondition(agenticScope -> {
                    CvReview review = (CvReview) agenticScope.readState("cvReview");
                    System.out.println("简历评审分数: " + review.score);
                    if (review.score >= 0.8)
                        System.out.println("简历已达标，退出循环。\n");
                    return review.score >= 0.8;
                })
                .maxIterations(3)   // 最多迭代 3 轮，防止无限循环
                .build();

        // 3. 创建完整的候选人流程：生成简历 > 首次评审 > 进入改进循环
        CandidateWorkflow candidateWorkflow = AgenticServices
                .sequenceBuilder(CandidateWorkflow.class)
                .subAgents(cvGenerator, cvReviewer, cvImprovementLoop)
                // 注意：这里把"组合 Agent" cvImprovementLoop 作为子节点放进了顺序构建器里
                // 之所以还要一个 cvReviewer，是为了在进入循环前先生成第一次评审结果
                .outputKey("cv")
                .build();

        // 4. 加载输入数据（人生经历、职位描述）
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");

        // 5. 执行候选人流程
        String candidateResult = candidateWorkflow.processCandidate(lifeStory, jobDescription);
        // 注意：入口参数和中间结果都存放在同一个 AgenticScope 中，
        // 无论组合嵌套多少层，所有 Agent 都能访问到这份共享上下文

        System.out.println("=== 候选人流程执行完成 ===");
        System.out.println("最终简历: " + candidateResult);

        System.out.println("\n\n\n\n");

        ////////////////// HIRING TEAM COMPOSED WORKFLOW //////////////////////
        // 我们收到了一封带候选人简历和联系方式的邮件，并已完成了电话 HR 面试。
        // 现在让 3 个评审并行打分，再把结果送入条件流程来决定"邀请"还是"拒绝"。

        // 1. 创建招聘团队流程所需的全部 Agent（三位评审 + 两个执行 Agent）
        HrCvReviewer hrCvReviewer = AgenticServices
                .agentBuilder(HrCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("hrReview")
                .build();

        ManagerCvReviewer managerCvReviewer = AgenticServices
                .agentBuilder(ManagerCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("managerReview")
                .build();

        TeamMemberCvReviewer teamMemberCvReviewer = AgenticServices
                .agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("teamMemberReview")
                .build();

        // 邮件助手：负责发送拒绝邮件（带工具）
        EmailAssistant emailAssistant = AgenticServices
                .agentBuilder(EmailAssistant.class)
                .chatModel(CHAT_MODEL)
                .tools(new OrganizingTools())
                .build();

        // 面试组织者：负责安排面试（带工具）
        InterviewOrganizer interviewOrganizer = AgenticServices
                .agentBuilder(InterviewOrganizer.class)
                .chatModel(CHAT_MODEL)
                .tools(new OrganizingTools())
                .build();

        // 2. 创建并行评审工作流：3 个评审同时运行，然后汇总成一份组合评审结果
        UntypedAgent parallelReviewWorkflow = AgenticServices
                .parallelBuilder()
                .subAgents(hrCvReviewer, managerCvReviewer, teamMemberCvReviewer)
                .executor(Executors.newFixedThreadPool(3))   // 用 3 线程线程池真正并行执行
                .outputKey("combinedCvReview")
                // 并行全部结束后，用 join 逻辑把 3 份评审合并：反馈拼接 + 计算平均分
                .output(agenticScope -> {
                    CvReview hrReview = (CvReview) agenticScope.readState("hrReview");
                    CvReview managerReview = (CvReview) agenticScope.readState("managerReview");
                    CvReview teamMemberReview = (CvReview) agenticScope.readState("teamMemberReview");
                    String feedback = String.join("\n",
                            "HR 评审: " + hrReview.feedback,
                            "经理评审: " + managerReview.feedback,
                            "团队成员评审: " + teamMemberReview.feedback
                    );
                    double avgScore = (hrReview.score + managerReview.score + teamMemberReview.score) / 3.0;
                    System.out.println("综合平均简历评审分数: " + avgScore + "\n");
                    return new CvReview(avgScore, feedback);
                })
                .build();

        // 3. 创建最终决策的条件工作流（conditional）：
        //    平均分 >= 0.8 走面试组织者，< 0.8 走邮件助手（发拒信）
        UntypedAgent decisionWorkflow = AgenticServices
                .conditionalBuilder()
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("combinedCvReview")).score >= 0.8, interviewOrganizer)
                .subAgents(agenticScope -> ((CvReview) agenticScope.readState("combinedCvReview")).score < 0.8, emailAssistant)
                .build();

        // 4. 创建完整的招聘团队流程：并行评审 → 决策
        HiringTeamWorkflow hiringTeamWorkflow = AgenticServices
                .sequenceBuilder(HiringTeamWorkflow.class)
                .subAgents(parallelReviewWorkflow, decisionWorkflow)
                .build();

        // 5. 加载输入数据
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");

        // 把所有数据放进一个 Map，便于统一访问
        Map<String, Object> inputData = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "hrRequirements", hrRequirements,
                "phoneInterviewNotes", phoneInterviewNotes,
                "jobDescription", jobDescription
        );

        // 6. 执行招聘团队流程
        hiringTeamWorkflow.processApplication(candidateCv, jobDescription, hrRequirements, phoneInterviewNotes, candidateContact);

        System.out.println("=== 招聘团队流程执行完成 ===");
        System.out.println("并行评审已完成，并已作出最终决策");

        // 注意：随着工作流越来越复杂，请确保输入、中间结果和输出参数的名称保持唯一，
        // 以免在共享的 AgenticScope 中无意覆盖数据
    }
}