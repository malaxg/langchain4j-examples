package _4_parallel_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import domain.CvReview;

/**
 * Agent 接口（实体接口）：经理(Manager)视角的简历评审 Agent —— 并行工作流中的评审员之一。
 *
 * <p>在并行工作流中，三个评审 Agent 会【同时】评估同一份简历。
 * 本评审员从“招聘经理”角度，根据职位描述评估候选人胜任岗位的可能性，
 * 决定是否邀请面试，返回 CvReview（评分+反馈）。
 *
 * <p>注意：SystemMessage 末尾要求“只返回合法 JSON”，
 * 以保证并行的结构化输出可被独立反序列化。
 */
public interface ManagerCvReviewer {

    /**
     * 从招聘经理角度评估简历。
     *
     * @Agent(name = "managerReviewer") 指定 Agent 标识名（供框架识别），description 描述职责。
     * @SystemMessage 设定经理身份，{{jobDescription}} 注入职位描述。
     * @UserMessage 提供本次要评审的简历。
     *
     * @param cv 候选人简历
     * @param jobDescription 职位描述
     * @return CvReview 评审对象（评分+反馈）
     */
    @Agent(name = "managerReviewer", description = "根据职位描述评审简历,给出反馈和评分")
    @SystemMessage("""
            你是这个职位的招聘经理：
            {{jobDescription}}
            你需要评审求职者的简历，并决定众多求职者中邀请谁参加现场面试。
            你要给每份简历一个评分和反馈（包括好的方面和不好的方面）。
            可以忽略诸如缺少地址、占位符之类的问题。

            重要提示：只返回合法 JSON，换行用 \\n，不要使用任何 markdown 格式或代码块。
            """)
    @UserMessage("""
            评审这份简历：{{candidateCv}}
            """)
    CvReview reviewCv(@V("candidateCv") String cv, @V("jobDescription") String jobDescription);
}
