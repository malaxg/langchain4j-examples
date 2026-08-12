package _5_conditional_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import domain.CvReview;

/**
 * Agent 接口（实体接口）：信息补充请求(InfoRequester) Agent —— 条件工作流中的分支 Agent。
 *
 * <p>当 HR 评审反馈表明缺少某些信息时（条件工作流的某个分支被触发），
 * 本 Agent 会给候选人发一封友善的邮件，请求补充公司评审申请所需的信息，
 * 并说明其申请仍在考虑中。
 *
 * <p>核心概念：这是“多分支条件工作流”中的三个 Agent 之一，
 * 由条件分支决定是否调用（见 _5b）。返回类型为 String（发送结果）。
 */
public interface InfoRequester {

    /**
     * 给候选人发送请求补充信息的邮件。
     *
     * @Agent 声明 Agent 及职责描述。
     * @SystemMessage 设定邮件语气与原则。
     * @UserMessage 提供缺失信息的描述（HR 评审反馈）、候选人联系方式、职位描述。
     *
     * @param candidateContact 候选人联系方式
     * @param jobDescription 职位描述
     * @param hrReview HR 评审反馈（含缺失信息描述）
     * @return 发送结果文本
     */
    @Agent("给候选人发送邮件以获取额外信息")
    @SystemMessage("""
            你给候选人发送一封友善的邮件，请求他们补充公司评审申请所需
            的额外信息。请说明他们的申请仍在被考虑之中。
            """)
    @UserMessage("""
            包含缺失信息说明的 HR 评审：{{cvReview}}

            候选人联系方式：{{candidateContact}}

            职位描述：{{jobDescription}}
            """)
    String send(@V("candidateContact") String candidateContact, @V("jobDescription") String jobDescription, @V("cvReview") CvReview hrReview);
}
