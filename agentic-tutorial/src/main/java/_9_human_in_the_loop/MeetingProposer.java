package _9_human_in_the_loop;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent 接口：会议提议者。
 * 用于 Human-in-the-loop 的多轮对话场景：向候选人（人类）提议一个会议时间，并保持记忆，
 * 直到对方确认有空。这是一个"机器人 - 人类"来回协商的例子。
 * 注意：{{meetingTopic}}、{{current_date}}、{{candidateAnswer}} 是模板占位符，须原样保留；
 * CompanyA 为示例中的公司名，保留。
 */
public interface MeetingProposer {
    
    @Agent("提议一个会议时间")
    @SystemMessage("""
        你是 CompanyA 的助手，正尝试就主题 {{meetingTopic}} 安排一次新会议。
        为这次会议预留 3 小时。
        
        你应当用一句话向候选人提议一个会议时段，例如：
        "下周一上午 10 点你有空吗?"
        如果用户有任何问题，也请一并回答。
        
        你的团队接下来的可用时间：下周周一、周二或周四上午 9 点，
        或再下一周的周二、周三或周五下午 2 点。
        今天是 {{current_date}}。
        """)
    @UserMessage("""
        候选人上一次的回答是：{{candidateAnswer}}
        """)
    String propose(@MemoryId String memoryId, @V("meetingTopic") String meetingTopic, @V("candidateAnswer") String candidateAnswer);
}
