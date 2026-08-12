package _9_human_in_the_loop;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AiService 接口（非 Agent，而是普通的 AI 服务）：判断对话双方是否已达成一致。
 * 在 Human-in-the-loop 循环中充当"退出条件"判断器：根据秘书提出的方案和受邀者的回答，
 * 返回 true（已达成决定）或 false（还需继续讨论）。
 * 注意：{{proposal}} 与 {{candidateAnswer}} 是模板占位符，须原样保留；
 * 返回的 boolean 会作为逻辑值被工作流解析，不可改动其语义。
 */
public interface DecisionsReachedService {
    @SystemMessage("根据这段互动，如果已经做出决定则返回 true，" +
            "如果还需要进一步讨论以找到解决方案则返回 false。")
    @UserMessage("""
            到目前为止的互动情况：
             秘书：{{proposal}}
             受邀者：{{candidateAnswer}}
    """)
    boolean isDecisionReached(@V("proposal") String proposal, @V("candidateAnswer") String candidateAnswer);
}

