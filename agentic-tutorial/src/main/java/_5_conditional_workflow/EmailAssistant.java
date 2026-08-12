package _5_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent 接口（实体接口）：邮件助手(EmailAssistant) Agent —— 条件工作流中的“拒绝”分支 Agent。
 *
 * <p>当评审未通过（条件分支被触发）时，本 Agent 给候选人发送一封友善的拒绝邮件，
 * 并把申请状态更新为“rejected”（已拒绝），最后返回发送的邮件 ID。
 *
 * <p>返回类型为 int：该返回会被代码逻辑使用（作为 sentEmailId 存入作用域），
 * 因此返回值本身是“被代码解析的数据”，保持不变。
 */
public interface EmailAssistant {

    /**
     * 向未通过评审的候选人发送拒绝邮件。
     *
     * @Agent 声明 Agent：负责给未通过第一轮评审的候选人发拒绝邮件，
     *   返回发送的邮件 ID；若未能发送则返回 0。
     * @SystemMessage 设定拒绝邮件的原则与动作（发邮件 + 更新状态为“rejected”）。
     * @UserMessage 提供被拒绝的候选人信息与职位描述。
     *
     * @param candidateContact 候选人联系方式
     * @param jobDescription 职位描述
     * @return 发送的邮件 ID（int，被代码逻辑解析使用）
     */
    @Agent("给未通过的候选人发送拒绝邮件,返回发送的邮件 ID,若无法发送则返回 0")
    @SystemMessage("""
            你给未通过第一轮评审的求职者发送一封友善的邮件。
            同时把申请状态更新为“rejected”（已拒绝）。
            最后返回发送的邮件 ID。
            """)
    @UserMessage("""
            被拒绝的候选人：{{candidateContact}}

            所应聘职位：{{jobDescription}}
            """)
    int send(@V("candidateContact") String candidateContact, @V("jobDescription") String jobDescription);
}
