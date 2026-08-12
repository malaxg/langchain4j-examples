package _8_non_ai_agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;
import domain.CvReview;

/**
 * 非 AI 智能体：把多份简历评审聚合成一份综合评审。
 * 这说明：普通的 Java 方法也可以当作"一等公民" Agent 用在智能体工作流中，
 * 与 AI 智能体互换使用。
 * 它非常适合确定性操作（计算、数据转换、聚合）——这些场景我们希望完全不用 LLM 参与。
 */
public class ScoreAggregator {

    /**
     * 聚合方法：对 HR / 经理 / 团队成员的评审进行合并。
     * @param hr   HR 评审（来自 AgenticScope 的 "hrReview"）
     * @param mgr  经理评审（来自 "managerReview"）
     * @param team 团队成员评审（来自 "teamMemberReview"）
     * @return 合成后的综合评审（平均分 + 拼接反馈）
     */
    @Agent(description = "把 HR/经理/团队的评审聚合成一份综合评审", outputKey = "combinedCvReview")
    public CvReview aggregate(@V("hrReview") CvReview hr,
                             @V("managerReview") CvReview mgr,
                             @V("teamMemberReview") CvReview team) {

        System.out.println("ScoreAggregator 被调用，hrReview: " + hr +
                ", managerReview: " + mgr +
                ", teamMemberReview: " + team);

        // 确定性计算三份评审的平均分（不经过 LLM，保证结果可预测、正确、便宜）
        double avgScore = (hr.score + mgr.score + team.score) / 3.0;
        
        // 把三份反馈拼接成一份综合反馈
        String combinedFeedback = String.join("\n\n",
                "HR 评审: " + hr.feedback,
                "经理评审: " + mgr.feedback,
                "团队成员评审: " + team.feedback
        );
        
        return new CvReview(avgScore, combinedFeedback);
    }
}

