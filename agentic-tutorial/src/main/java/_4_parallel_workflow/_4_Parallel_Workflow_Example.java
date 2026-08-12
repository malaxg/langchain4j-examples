package _4_parallel_workflow;

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
 * 示例主类（4）：演示【并行工作流(Parallel workflow)】。
 *
 * <p>与 3x 顺序/循环的区别：这里构建 3 个【并行】运行的评审 Agent，
 * 它们会【同时】评估同一份简历，互不等待、并发执行，之后再聚合结果。
 * 三个 Agent：
 * - HrCvReviewer（从 HR 角度评估，输入：简历、HR 要求，输出 CvReview）
 * - ManagerCvReviewer（评估胜任岗位的可能性，输入：简历、职位描述，输出 CvReview）
 * - TeamMemberCvReviewer（评估能否融入团队，输入：简历，输出 CvReview）
 *
 * <p>核心编排概念（异步/并发）：
 * - parallelBuilder()：并行工作流构建器；
 * - .executor(executor)：指定线程池（可选）；不指定时框架会用内部缓存线程池，执行完后自动关闭；
 * - .output(agenticScope -> ...)：输出适配器回调，在所有并行子 Agent 完成后执行，
 *   从作用域读取每个评审员的结果，聚合成一个平均分+合并反馈的 CvReview。
 */
public class _4_Parallel_Workflow_Example {

    // 设置日志级别
    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);
    }

    /**
     * 本示例演示如何实现 3 个并行运行的 CvReviewer Agent，
     * 它们会同时评估这份简历。三个 Agent 分别是：
     * - ManagerCvReviewer（判断候选人胜任该岗位的可能性）
     *      输入：简历 CV 和职位描述
     * - TeamMemberCvReviewer（判断候选人融入团队的可能性）
     *      输入：简历 CV
     * - HrCvReviewer（从 HR 角度检查候选人是否合规/达标）
     *      输入：简历 CV、HR 要求
     */

    // 1. 定义驱动所有 Agent 的底层模型
    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 2. 在本包中定义三个子 Agent 接口：
        //      - HrCvReviewer.java
        //      - ManagerCvReviewer.java
        //      - TeamMemberCvReviewer.java

        // 3. 用 AgenticServices 构建所有 Agent
        HrCvReviewer hrCvReviewer = AgenticServices.agentBuilder(HrCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("hrReview") // 该评审结果会写入作用域，供稍后的聚合(输出适配器)读取
                .build();

        ManagerCvReviewer managerCvReviewer = AgenticServices.agentBuilder(ManagerCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("managerReview") // 评审结果键名：managerReview
                .build();

        TeamMemberCvReviewer teamMemberCvReviewer = AgenticServices.agentBuilder(TeamMemberCvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("teamMemberReview") // 评审结果键名：teamMemberReview
                .build();

        // 4. 构建并行工作流
        var executor = Executors.newFixedThreadPool(3); // 固定 3 线程的线程池，稍后需要关闭（保存引用）

        UntypedAgent cvReviewGenerator = AgenticServices // 除非自定义类型化接口，否则用 UntypedAgent（见 _2_Sequential_Agent_Example）
                .parallelBuilder()                              // 并行工作流构建器
                .subAgents(hrCvReviewer, managerCvReviewer, teamMemberCvReviewer) // 子 Agent，可任意多个
                .executor(executor)                             // 可选：指定线程池；默认内部会使用自动关闭的缓存线程池
                .outputKey("fullCvReview")                      // 最终聚合输出的键名
                .output(agenticScope -> {                       // 输出适配器回调：在所有并行子 Agent 完成后执行
                    // 从作用域读取每个评审员的结果
                    CvReview hrReview = (CvReview) agenticScope.readState("hrReview");
                    CvReview managerReview = (CvReview) agenticScope.readState("managerReview");
                    CvReview teamMemberReview = (CvReview) agenticScope.readState("teamMemberReview");
                    // 返回一个聚合的评审结果：评分取平均，反馈合并（也可在这里做任何想要的聚合）
                    String feedback = String.join("\n",
                            "HR 评审: " + hrReview.feedback,
                            "经理评审: " + managerReview.feedback,
                            "团队成员评审: " + teamMemberReview.feedback
                    );
                    double avgScore = (hrReview.score + managerReview.score + teamMemberReview.score) / 3.0; // 三个评分求平均

                    return new CvReview(avgScore, feedback); // 返回聚合后的 CvReview
                        })
                .build();

        // 5. 从 resources/documents/ 加载原始参数
        String candidateCv = StringLoader.loadFromResource("/documents/tailored_cv.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        String hrRequirements = StringLoader.loadFromResource("/documents/hr_requirements.txt");
        String phoneInterviewNotes = StringLoader.loadFromResource("/documents/phone_interview_notes.txt");

        // 6. 无类型 Agent 需用 Map 传参（键名与各 Agent 接口的 @V 变量名一致）
        Map<String, Object> arguments = Map.of(
                "candidateCv", candidateCv,
                "jobDescription", jobDescription
                ,"hrRequirements", hrRequirements
                ,"phoneInterviewNotes", phoneInterviewNotes
        );

        // 7. 调用并行组合 Agent 生成聚合评审
        var review = cvReviewGenerator.invoke(arguments);

        // 8. 打印聚合评审结果
        System.out.println("=== 聚合评审后的简历 REVIEWED CV ===");
        System.out.println(review);

        // 9. 关闭线程池（释放资源）
        executor.shutdown();
   }
}
