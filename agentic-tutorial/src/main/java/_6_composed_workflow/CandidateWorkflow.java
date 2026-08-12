package _6_composed_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;


/**
 * Agent 接口：候选人综合工作流（组合工作流之一）。
 * 这是一个"编排型"Agent：内部由顺序（sequence）组合了多个子 Agent。
 * 线上流程：根据人生经历生成主简历 → 根据职位描述定制 → 反复评审改进，直到分数达标。
 * 因为 LangChain4j 中的所有 Agent 本质上都是同一个 Agent 对象，所以可以把小 Agent
 * 组合成这个大 Agent，实现"组合工作流（composed workflow）"。
 */
public interface CandidateWorkflow {
    @Agent("根据人生经历和职位描述，生成主简历，并通过反馈循环针对职位描述进行定制，直到达到合格分数")
    String processCandidate(@V("lifeStory") String userInfo, @V("jobDescription") String jobDescription);
}
