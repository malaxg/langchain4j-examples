package _3_loop_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import domain.CvReview;

/**
 * Agent 接口（实体接口）：带评分反馈的简历定制(Tailor) Agent —— 循环工作流中的“改进器”。
 *
 * <p>与 _2_sequential_workflow 里的 CvTailor 类似，区别在于：
 * 它的指令里包含上一轮评审得到的 CvReview（评分 + 反馈），
 * 因此每次循环都根据最新的评审反馈迭代改进简历，让它越来越符合职位要求。
 *
 * <p>循环工作流编排逻辑：
 * ScoredCvTailor（改进简历） ↰
 *      ↓                             ↱（评分不满意，继续循环）
 * CvReviewer（评审打分） → 退出条件(exitCondition)判断
 */
public interface ScoredCvTailor {

    /**
     * 根据评审反馈定制/改进简历。
     *
     * @Agent 声明 Agent 及其职责描述。
     * @SystemMessage 固定行为准则，{{cv}} 注入当前版本的简历。
     * @UserMessage 提供本轮评审反馈（CvReview），{{cvReview}} 注入上轮评分+反馈。
     *
     * @param cv 当前版本的简历（会在每次循环中被更新）
     * @param cvReview 上一轮评审结果（评分+反馈，指导如何改进）
     * @return 改进后的简历文本
     */
    @Agent("根据具体指令定制一份简历")
    @SystemMessage("""
            这里是一份需要根据具体职位描述、反馈或其他指令来定制(量身定制)的简历。
            你可以为了满足要求而让简历看起来更好，但不要虚构事实。
            如果让简历更符合指令，你可以删除无关内容。
            目标是让求职者获得面试机会，且简历经得起面试验证。
            当前简历：{{cv}}
            """)
    @UserMessage("""
            以下是定制这份简历的指令和反馈：
            （再次强调：不要虚构原简历中不存在的事实。
            如果求职者不匹配，请突出他现有的、与要求最接近的特征，
            但不要编造事实）
            评审结果：{{cvReview}}
            """)
    String tailorCv(@V("cv") String cv, @V("cvReview") CvReview cvReview);
}
