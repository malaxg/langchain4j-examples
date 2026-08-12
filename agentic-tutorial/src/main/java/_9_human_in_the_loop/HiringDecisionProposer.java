package _9_human_in_the_loop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import domain.CvReview;

/**
 * Agent 接口：招聘决策提议者。
 * 作用：把多方参与招聘过程后给出的反馈（CvReview）进行简洁总结，
 * 输出给"人类"来做最终决定（是否继续推进）。这是"Human-in-the-loop（人机协作）"里
 * 由 AI 先提出建议、再由人来把关的一环。
 * 其中 {{cvReview}} 是模板占位符，会被 @V("cvReview") 注入的真实值替换（占位符须原样保留）。
 */
public interface HiringDecisionProposer {
    
    @Agent("总结招聘决策，供最终人工验证")
    @SystemMessage("""
        对于一个给定的评审，请用至多 3 行总结招聘的理由，
        这样人类就可以据此做出是否继续推进的最终决定。
        """)
    @UserMessage("""
        招聘过程中各方给出的全部反馈如下：{{cvReview}}
        """)
    String propose(@V("cvReview") CvReview cvReview);
}
