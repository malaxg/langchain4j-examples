package _4_parallel_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import domain.CvReview;

/**
 * Agent 接口（实体接口）：HR 视角的简历评审 Agent —— 并行工作流中的评审员之一。
 *
 * <p>在并行工作流中，三个评审 Agent 会【同时】评估同一份简历。
 * 本评审员从 HR（人力资源）的角度，根据 HR 招聘要求与电话面试记录，
 * 判断候选人是否符合 HR 层面的资格，返回 CvReview（评分+反馈）。
 *
 * <p>注意与 2x 中评审器 Prompt 的区别：
 * SystemMessage 末尾要求“只返回合法 JSON”，这是为了让三个并行评审员的
 * 结构化输出可以被独立反序列化（并行输出时必须显式约束格式）。
 */
public interface HrCvReviewer {

    /**
     * 从 HR 角度评审简历。
     *
     * @Agent(name = "hrReviewer") 指定 Agent 的标识名（供框架识别），
     *       description 用中文描述其职责。
     * @SystemMessage 设定 HR 身份与评审准则，{{hrRequirements}} 注入 HR 招聘要求。
     * @UserMessage 提供本次要评审的简历与电话面试记录。
     *
     * @param cv 候选人简历
     * @param phoneInterviewNotes 电话面试记录
     * @param hrRequirements HR 招聘要求
     * @return CvReview 评审对象（评分+反馈）
     */
    @Agent(name = "hrReviewer", description = "评审简历,检查候选人是否符合 HR 招聘要求,给出反馈和评分")
    @SystemMessage("""
            你任职于 HR 部门，负责评审简历以寻找满足以下要求的候选人：
            {{hrRequirements}}
            你要为每份简历给出评分和反馈（包括好的和不好的方面）。
            可以忽略诸如缺少地址、占位符之类的问题。

            重要提示：只返回合法 JSON，换行用 \\n，不要使用任何 markdown 格式或代码块。
            """)
    @UserMessage("""
            评审这份简历：{{candidateCv}}，并参考随附的电话面试记录：{{phoneInterviewNotes}}
            """)
    CvReview reviewCv(@V("candidateCv") String cv, @V("phoneInterviewNotes") String phoneInterviewNotes, @V("hrRequirements") String hrRequirements);
}
