package dev.langchain4j.example;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 客服 Agent 的 AI 接口（LangChain4j 的核心）。
 *
 * <p>{@code @AiService} 注解会让 LangChain4j 在运行时自动为这个接口生成实现对象，
 * 对象内部会串联"大语言模型 + 记忆(ChatMemory) + 工具(Tools) + RAG 检索(ContentRetriever)"。
 * 也就是说，你只需要声明方法签名，LangChain4j 就会替你完成整个 Agent 的装配。</p>
 *
 * <p>面向初学者：这个接口就是 Agent 的"大脑入口"。调用 {@link #answer(String, String)}
 * 方法传入"会话ID(memoryId)"和"用户消息(userMessage)"，就能得到客服的回复。</p>
 */
@AiService
public interface CustomerSupportAgent {

    /**
     * 处理用户消息并返回客服回复。
     *
     * <p>通过注解为这次调用配置"系统提示词(SystemMessage)"，其中 {@code {{current_date}}}
     * 占位符会被自动替换为当前日期。</p>
     *
     * @param memoryId    会话（对话）ID，用于区分不同用户，实现多会话记忆隔离
     * @param userMessage 用户发来的消息内容
     * @return 客服的回复结果，其中既包含回复内容，也包含工具调用记录、Token 用量、
     *         RAG 检索到的来源等信息
     */
    @SystemMessage("""
            你的名字叫 Roger，你是一家名为 'Miles of Smiles' 的租车公司的客服代理。
            你友好、礼貌且简洁。

            你必须遵守的规则：

            1. 在获取预订详情或取消预订之前，
            你必须确认自己已经知道客户的姓名和预订编号。

            2. 当被要求取消预订时，首先确认该预订存在，然后征求客户的明确确认。
            取消预订后，一定要说 "We hope to welcome you back again soon"。

            3. 你只能回答与 Miles of Smiles 业务相关的问题。
            当被问到与公司业务无关的问题时，
            请道歉并说明你无法提供帮助。

            今天是 {{current_date}}。
            """)
    Result<String> answer(@MemoryId String memoryId, @UserMessage String userMessage);
}
