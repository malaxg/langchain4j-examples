package _5_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent 接口（实体接口）：面试组织(InterviewOrganizer) Agent —— 条件工作流中的成功分支 Agent。
 *
 * <p>当评审通过（条件满足）时触发本分支：为候选人组织现场面试，
 * 包括给相关人员发送日程邀请、给候选人发送祝贺邮件并附上面试细节、
 * 以及把申请状态更新为“invited on-site”（已邀请到现场）。
 *
 * <p>核心概念：该 Agent 被挂上 .contentRetriever(...)（RAG 检索器）与 .tools(...)（工具），
 * 说明在 Agent 化工作流中，不同 Agent 可附加不同能力（工具、RAG、模型等）。
 */
public interface InterviewOrganizer {

    /**
     * 为候选人组织一场现场面试。
     *
     * @Agent 声明 Agent 及职责描述。
     * @SystemMessage 设定组织面试的规则：给所有相关人员发日历邀请、约在一周后上午 3 小时、
     *   给候选人发祝贺邮件并附面试信息、最后更新申请状态为“已邀请到现场”。
     * @UserMessage 提供本次要组织的候选人信息。
     *
     * @param candidateContact 候选人联系方式（外部访客政策适用于此候选人）
     * @param jobDescription 相关职位描述
     * @return 组织结果文本
     */
    @Agent("为求职者组织现场面试")
    @SystemMessage("""
            你负责组织现场会议：向所有相关人员发送日历邀请，
            从当前日期起一周后的上午，安排一场 3 小时的面试。
            涉及的职位空缺如下：{{jobDescription}}
            你还要给候选人发送一封祝贺邮件，包含面试细节
            以及他来现场前需要了解的一切事项。
            最后，把申请状态更新为“invited on-site”（已邀请到现场）。
            """)
    @UserMessage("""
            为这位候选人组织一场现场面试会议（适用外部访客政策）：{{candidateContact}}
            """)
    String organize(@V("candidateContact") String candidateContact, @V("jobDescription") String jobDescription);
}
