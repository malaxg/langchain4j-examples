package _8_non_ai_agents;

import _4_parallel_workflow.HrCvReviewer;
import _4_parallel_workflow.ManagerCvReviewer;
import _4_parallel_workflow.TeamMemberCvReviewer;
import _5_conditional_workflow.EmailAssistant;
import _5_conditional_workflow.InterviewOrganizer;
import _5_conditional_workflow.OrganizingTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import domain.CvReview;
import util.ChatModelProvider;
import util.StringLoader;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;

public class _8_Non_AI_Agents {

    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 100);  // 控制从模型调用中可见的信息量
    }

    /**
     * 这里演示如何在智能体工作流中使用"非 AI 智能体"（即普通的 Java 方法/运算符）。
     * 非 AI 智能体本质上就是普通方法，但可以像其它类型的 Agent 一样使用。
     * 它们非常适合确定性操作（计算、数据转换、聚合）——这类场景我们希望完全没有 LLM 参与。
     * 工作流中点能被外包给非 AI 智能体的步骤越多，就越快、越准、越便宜。
     * 当某个步骤需要强制"确定性"时，非 AI 智能体优先于工具（tool）。
     * 本例中我们就希望评审的平均分由程序确定性计算（而非 LLM 算），
     * 并根据聚合后的分数以确定性方式更新数据库中的应聘状态。
     */

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 1. 本包中的 ScoreAggregator 等非 AI 智能体直接声明即可

        // 2. 构建并行评审步骤所需的 AI 子智能体
        HrCvReviewer hrReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("hrReview")
                .build();

        ManagerCvReviewer managerReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("managerReview")
                .build();

        TeamMemberCvReviewer teamReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("teamMemberReview")
                .build();

        // 3. 构建"并行评审"的组合 Agent
        var executor = Executors.newFixedThreadPool(3);  // 保留引用，稍后需要关闭

        UntypedAgent parallelReviewWorkflow = AgenticServices
                .parallelBuilder()
                .subAgents(hrReviewer, managerReviewer, teamReviewer)
                .executor(executor)
                .build();

        // 4. 构建完整工作流（包含非 AI 智能体）
        UntypedAgent collectFeedback = AgenticServices
                .sequenceBuilder()
                .subAgents(
                        parallelReviewWorkflow,
                        new ScoreAggregator(), // 非 AI 智能体无需 AgenticServices 构建器；outputKey 已在类内定义为 'combinedCvReview'
                        new StatusUpdate(), // 以 'combinedCvReview' 作为输入，无需输出
                        AgenticServices.agentAction(agenticScope -> { // 另一种添加非 AI 智能体的方式：直接操作 AgenticScope
                            CvReview review = (CvReview) agenticScope.readState("combinedCvReview");
                            agenticScope.writeState("scoreAsPercentage", review.score * 100); // 不同系统间的 Agent 通信往往需要做输出换算
                        })
                )
                .outputKey("scoreAsPercentage") // outputKey 来自 ScoreAggregator.java 上的非 AI 智能体注解
                .build();

        // 5. 加载输入数据
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String candidateContact = StringLoader.loadFromResource("/documents/candidate_contact.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");

        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "candidateContact", candidateContact,
                "hrRequirements", hrRequirements,
                "phoneInterviewNotes", phoneInterviewNotes,
                "jobDescription", jobDescription
        );

        // 6. 调用整个工作流
        double scoreAsPercentage = (double) collectFeedback.invoke(arguments);
        executor.shutdown();

        System.out.println("=== 评分百分比（百分制） ===");
        System.out.println(scoreAsPercentage);
        // 从日志可见，应聘状态也已被同步更新

    }
}