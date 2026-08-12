package _6_composed_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * Agent 接口：简历改进循环（组合工作流中的循环节点）。
 * 封装"定制简历 → 评审"的迭代循环：反复改进简历并让评审 Agent 打分，
 * 直到评分 >= 0.8（合格线）才退出循环。
 */
public interface CvImprovementLoop {
    @Agent("通过迭代式定制与评审不断改进简历，直到获得合格分数")
    String improveCv(@V("cv") String cv, @V("jobDescription") String jobDescription);
}
