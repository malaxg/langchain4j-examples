package _4_parallel_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import domain.CvReview;

/**
 * Agent 接口（实体接口）：团队成员(Team Member)视角的简历评审 Agent —— 并行工作流中的评审员之一。
 *
 * <p>在并行工作流中，三个评审 Agent 会【同时】评估同一份简历。
 * 本评审员从“团队成员”角度，评估候选人能否融入团队文化，
 * 返回 CvReview（评分+反馈）。
 *
 * <p>注意：SystemMessage 末尾要求“只返回合法 JSON”，
 * 以保证并行的结构化输出可被独立反序列化。
 */
public interface TeamMemberCvReviewer {

    /**
     * 评估候选人是否适合团队。
     *
     * @Agent(name = "teamMemberReviewer") 指定 Agent 标识名，description 描述职责。
     * @SystemMessage 设定团队成员身份与关注点，描述团队文化。
     * @UserMessage 提供本次要评审的简历。
     *
     * @param cv 候选人简历
     * @return CvReview 评审对象（评分+反馈）
     */
    @Agent(name = "teamMemberReviewer", description = "评审简历,评估候选人是否适合团队,给出反馈和评分")
    @SystemMessage("""
            你与一群积极上进、自驱力强的同事共事，工作自由度很高。
            你的团队看重协作、责任感和务实精神。
            你需要评审求职者的简历，决定这个人有多大可能融入你的团队。
            你要给每份简历一个评分和反馈（包括好的方面和不好的方面）。
            可以忽略诸如缺少地址、占位符之类的问题。

            重要提示：只返回合法 JSON，换行用 \\n，不要使用任何 markdown 格式或代码块。
            """)
    @UserMessage("""
            评审这份简历：{{candidateCv}}
            """)
    CvReview reviewCv(@V("candidateCv") String cv);
}
