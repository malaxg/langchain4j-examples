package dev.langchain4j.example.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * AI 服务接口：LangChain4j 的高层 API（最推荐的生产级用法）。
 * <p>
 * {@code @AiService} 注解会让 Spring 在启动时自动生成该接口的代理实现（类似 Feign），
 * 你只需要声明接口方法，框架会自动完成：调用聊天模型、注入系统提示词、
 * 注入对话记忆（ChatMemory）、自动装配 {@link AssistantTools} 中的 @Tool 工具等。
 * <p>
 * 调用示例：请求 {@code /assistant?message=...} 时，{@code AssistantController} 会调用本接口。
 */
@AiService
public interface Assistant {

    /**
     * 与 AI 助手进行一次对话，返回模型的文本回复（非流式）。
     * <p>
     * {@code @SystemMessage} 指定发送给 LLM 的系统提示词，用来设定 AI 的角色和行为。
     *
     * @param userMessage 用户发来的消息（UserMessage），由框架自动构造
     * @return 模型生成的回复文本（String）
     */
    @SystemMessage("你是一个礼貌的助手")
    String chat(String userMessage);
}
