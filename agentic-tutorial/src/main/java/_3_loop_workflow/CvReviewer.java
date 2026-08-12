package _3_loop_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import domain.CvReview;

/**
 * Agent 接口（实体接口）：简历评审(Reviewer) Agent —— 循环工作流中的“评分器”。
 *
 * <p>它扮演“招聘经理”角色，根据职位描述评审某份简历，
 * 返回一个 CvReview 对象（评分 + 反馈）。
 * 在循环工作流中，它的评分会被用作【退出条件】的判断依据：
 * 得分足够高时结束循环，否则继续让定制 Agent 改进简历后再评审。
 *
 * <p>核心概念：
 * - @SystemMessage：固化“你是招聘经理”的身份与评审准则，{{jobDescription}} 注入职位描述；
 * - @UserMessage：本次要评审的简历，{{cv}} 注入简历文本；
 * - 返回 CvReview：强制 LLM 输出结构化评分+反馈，供代码读取并判断循环是否该结束。
 */
public interface CvReviewer {

    /**
     * 评审一份简历并返回结构化评审结果。
     *
     * @param cv 待评审的简历
     * @param jobDescription 职位描述（作为评审依据）
     * @return CvReview 对象（score + feedback）
     */
    @Agent("根据具体指令评审一份简历,给出反馈和评分。综合考虑简历与职位的匹配程度")
    @SystemMessage("""
            你是这个职位的招聘经理：
            {{jobDescription}}
            你需要评审求职者的简历，并决定众多求职者中邀请谁参加现场面试。
            你要给每份简历一个评分和反馈（包括好的方面和不好的方面）。
            可以忽略诸如缺少地址、占位符之类的问题。
            """)
    @UserMessage("""
            评审这份简历：{{cv}}
            """)
    CvReview reviewCv(@V("cv") String cv, @V("jobDescription") String jobDescription);
}
