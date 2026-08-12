package _6_composed_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * Agent 接口：招聘团队综合工作流（组合工作流之二）。
 * 内部由"并行评审"与"条件决策"两个子工作流组合而成。
 * 流程：HR/经理/团队成员 3 个评审并行打分 → 汇总平均分 → 达标则安排面试，否则发送拒信。
 */
public interface HiringTeamWorkflow {
    @Agent("根据简历、电话面试记录和职位描述，本智能体将决定邀请或拒绝该候选人")
    void processApplication(@V("candidateCv") String candidateCv,
                          @V("jobDescription") String jobDescription, 
                          @V("hrRequirements") String hrRequirements, 
                          @V("phoneInterviewNotes") String phoneInterviewNotes, 
                          @V("candidateContact") String candidateContact);
}
