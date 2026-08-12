package _3_loop_workflow;

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
 * 示例主类（3a）：演示【循环工作流(Loop workflow)】。
 *
 * <p>与 2x（顺序执行一次）的区别：这里把两个 Agent 放进一个“循环”，
 * 反复执行“改进简历 → 评审打分”，直到评分达到阈值才停止。
 * 两个 Agent：
 * - ScoredCvTailor（接收 CV 和评审反馈 CvReview，改进简历）；
 * - CvReviewer（接收定制后的简历和职位描述，返回 CvReview 评审对象）。
 * 循环在评分超过阈值（如 0.7/0.8）时结束（退出条件）。
 *
 * <p>核心编排概念（状态机 States/Loop）：
 * - loopBuilder()：循环工作流构建器；
 * - .exitCondition(agenticScope -> ...)：定义退出条件回调；
 *   每次 Agent 调用后都会检查（不只是整个循环结束后）；
 *   返回 true 即结束循环。这里用评审分数作为判断依据；
 * - .maxIterations(3)：最大迭代次数，防止退出条件一直不满足时陷入死循环；
 * - agenticScope.readState("cvReview")：从作用域读取评审变量。
 */
public class _3a_Loop_Agent_Example {

    // 设置日志级别
    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);
    }

    // 1. 定义驱动所有 Agent 的底层模型
    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 2. 在本包中定义两个子 Agent 接口：
        //      - CvReviewer.java（评审打分）
        //      - ScoredCvTailor.java（带反馈的定制）

        // 3. 用 AgenticServices 构建所有 Agent
        CvReviewer cvReviewer = AgenticServices.agentBuilder(CvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("cvReview") // 每次迭代都会用新反馈/新评分更新该变量，供下一轮定制使用
                .build();
        ScoredCvTailor scoredCvTailor = AgenticServices.agentBuilder(ScoredCvTailor.class)
                .chatModel(CHAT_MODEL)
                .outputKey("cv") // 每次迭代都会更新该变量，持续改进简历
                .build();

        // 4. 构建循环工作流
        UntypedAgent reviewedCvGenerator = AgenticServices // 除非自定义类型化接口，否则用 UntypedAgent（见 _2_Sequential_Agent_Example）
                .loopBuilder()                                     // 循环工作流构建器
                .subAgents(cvReviewer, scoredCvTailor)             // 加入子 Agent，顺序即一次循环内的执行顺序
                .outputKey("cv")                                   // 最终要观察的输出变量（改进后的简历）
                .exitCondition(agenticScope -> {                   // 定义退出条件回调
                            CvReview review = (CvReview) agenticScope.readState("cvReview"); // 读取评审结果
                            System.out.println("检查退出条件,当前评分=" + review.score);        // 打印中间评分供观察
                            return review.score > 0.8;             // 评分>0.8 即满意，退出循环
                        }) // 注意：退出条件在【每次 Agent 调用后】都会检查，而不是等整个循环跑完才检查
                .maxIterations(3) // 最大迭代次数：防止退出条件一直不满足时无限循环
                .build();

        // 5. 从 resources/documents/ 的文本文件加载初始参数
        // - master_cv.txt（初始主简历）
        // - job_description_backend.txt（职位描述）
        String masterCv = StringLoader.loadFromResource("/documents/master_cv.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");

        // 5. 无类型 Agent 需用 Map 传参
        Map<String, Object> arguments = Map.of(
                "cv", masterCv,         // 从主简历开始，它会被循环持续改进
                "jobDescription", jobDescription
        );

        // 5. 调用组合 Agent 生成定制后的简历
        String tailoredCv = (String) reviewedCvGenerator.invoke(arguments);

        // 6. 打印生成的简历
        System.out.println("=== 循环评审后的简历 REVIEWED CV UNTYPED ===");
        System.out.println((String) tailoredCv);

        // 这份简历很可能在第一轮“定制+评审”后即达标；
        // 若想看它失败，可改用长笛教师的职位描述（如 3b），
        // 那里我们还检查简历的中间状态，并取回最终的评审和评分
    }
}
