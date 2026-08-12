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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 示例主类（3b）：演示【循环工作流的失败场景 + 采集中间状态 + 返回最终评分】。
 *
 * <p>与 3a 的区别：这次构建同一个循环 Agent，但故意用一份【不匹配】的职位描述
 * （长笛教师）去定制简历，从而看到循环因评分始终不达标、最后触发 maxIterations。
 * 同时：
 * - 除了最终简历，还额外返回最新一次的评分和反馈（检查是否值得投递这份简历）；
 * - 用一个技巧采集每一轮评审的中间状态：exitCondition 在每次 Agent 调用后都会触发，
 *   因此可以趁机把当轮评审存入一个 List（reviewHistory），便于事后查看完整演化历史。
 */
public class _3b_Loop_Agent_Example_States_And_Fail {

    // 设置日志级别
    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);
    }

    /**
     * 这里我们构建与 3a 相同的循环 Agent，但这次用一份不匹配的职位描述来定制简历，
     * 从而观察它失败（评分始终不达标）。
     * 我们还会在最终简历之外，返回最新的评分和反馈，
     * 以便检查是否拿到了满意分数、值不值得投递这份简历。
     * 另外演示一个技巧：在每次检查退出条件时，
     * 把当轮的中间评审状态（它每轮都会被覆盖）存进一个 List，方便后续查看。
     */

    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 1. 创建所有子 Agent（与之前相同）
        CvReviewer cvReviewer = AgenticServices.agentBuilder(CvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputKey("cvReview") // 每次迭代都会更新，产生下一轮定制所需的新反馈
                .build();
        ScoredCvTailor scoredCvTailor = AgenticServices.agentBuilder(ScoredCvTailor.class)
                .chatModel(CHAT_MODEL)
                .outputKey("cv") // 每次迭代都会更新，持续改进简历
                .build();

        // 2. 构建循环，并在每次检查退出条件时收集评审历史
        // 判断循环是“达标退出”还是“达到最大迭代次数”很重要
        // （例如候选人可能根本不想为这份工作费劲投递）。
        // 可以通过修改输出变量，把最后一次的评分和反馈也包含进来，
        // 循环结束后自行检查；也可以用一个可变 List 把中间值存起来事后查看。
        // 下面这段代码同时做了这两件事。
        List<CvReview> reviewHistory = new ArrayList<>(); // 用于保存每一轮的评审（CvReview）

        UntypedAgent reviewedCvGenerator = AgenticServices // 除非自定义类型化接口，否则用 UntypedAgent
                .loopBuilder().subAgents(cvReviewer, scoredCvTailor) // 子 Agent，顺序即循环内执行顺序
                .outputKey("cvAndReview") // 最终观察的输出键
                .output(agenticScope -> { // 自定义输出适配器：打包最终 CV 和最后一轮评审
                    Map<String, Object> cvAndReview = Map.of(
                            "cv", agenticScope.readState("cv"),             // 循环结束后最终的简历
                            "finalReview", agenticScope.readState("cvReview") // 最后一轮评审
                    );
                    return cvAndReview;
                })
                .exitCondition(scope -> { // 退出条件：每次 Agent 调用后触发
                    CvReview review = (CvReview) scope.readState("cvReview");
                    reviewHistory.add(review); // 把当轮评分+反馈存入历史列表（用于采集中间状态）
                    System.out.println("退出检查,评分=" + review.score);
                    return review.score >= 0.8; // 评分>=0.8 才结束循环
                })
                .maxIterations(3) // 最大迭代次数：防止退出条件永不满足时死循环
                .build();

        // 3. 从 resources/documents/ 加载初始参数
        // - master_cv.txt（初始主简历）
        // - job_description_backend.txt（职位描述）
        String masterCv = StringLoader.loadFromResource("/documents/master_cv.txt");
        String fluteJobDescription = "我们正在寻找一位充满热情的长笛教师加入我们的音乐学院。";

        // 4. 无类型 Agent 需用 Map 传参
        Map<String, Object> arguments = Map.of(
                "cv", masterCv,               // 从主简历开始，会被持续改进
                "jobDescription", fluteJobDescription // 注意：这是与候选人背景不匹配的职位描述
        );

        // 5. 调用组合 Agent 生成定制后的简历
        Map<String, Object> cvAndReview = (Map<String, Object>) reviewedCvGenerator.invoke(arguments);

        // 可以从日志中观察到每一轮的过程，例如：
        // 第 1 轮输出："content": "{\n  \"score\": 0.0,\n  \"feedback\": \"这份简历不适合我们音乐学院的资深长笛教师岗位...
        // 第 2 轮输出："content": "{\n  \"score\": 0.3,\n  \"feedback\": \"John 的简历展现出较强的软技能，如沟通、耐心和适应能力，这些对教学岗位很重要。然而缺乏正式的音乐训练或...
        // 第 3 轮输出："content": "{\n  \"score\": 0.4,\n  \"feedback\": \"John Doe 展现出较强的软技能和辅导经验,...

        System.out.println("=== 为长笛教师定制的评审简历 ===");
        System.out.println(cvAndReview.get("cv")); // 循环结束后的最终简历

        // 现在可以在输出 Map 里拿到 finalReview，自行检查
        // 最终分数和反馈是否符合要求
        CvReview review = (CvReview) cvAndReview.get("finalReview");
        System.out.println("=== 为长笛教师的最终评审 ===");
        System.out.println("简历" + (review.score >= 0.8 ? " 通过" : " 未通过") + ",评分=" + review.score);
        System.out.println("最终反馈: " + review.feedback);

        // reviewHistory 里保存了完整的评审历史
        System.out.println("=== 为长笛教师定制的完整评审历史 ===");
        System.out.println(reviewHistory);

    }
}
