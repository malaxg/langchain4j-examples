package _8_non_ai_agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;
import domain.CvReview;

/**
 * 非 AI 智能体：根据综合评分更新应聘状态（伪数据库更新）。
 * 同样是"普通 Java 方法当 Agent 用"的例子——状态更新是纯确定性逻辑，不应交给 LLM。
 */
public class StatusUpdate {

    /**
     * 更新状态方法：根据平均分决定邀请还是拒绝。
     * @param aggregateCvReview 综合评审（来自 AgenticScope 的 "combinedCvReview"）
     */
    @Agent(description = "根据评分更新应聘状态")
    public void update(@V("combinedCvReview") CvReview aggregateCvReview) {
        double score = aggregateCvReview.score;
        System.out.println("StatusUpdate 被调用，评分: " + score);

        if (score >= 8.0) {
            // 模拟数据库更新（演示用）
            System.out.println("应聘状态已更新为：已邀请 ");
        } else {
            // 模拟数据库更新（演示用）
            System.out.println("应聘状态已更新为：已拒绝");
        }
    }
}

