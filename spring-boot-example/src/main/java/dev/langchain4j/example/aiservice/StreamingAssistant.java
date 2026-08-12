package dev.langchain4j.example.aiservice;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

/**
 * 流式 AI 服务接口：LangChain4j 高层 API 的流式版本。
 * <p>
 * 与非流式的 {@link Assistant} 接口类似，同样由 {@code @AiService} 注解自动生成代理实现，
 * 自动支持系统提示词、对话记忆和 {@link AssistantTools} 工具调用，
 * 唯一区别是返回类型换成了响应式流 {@link Flux}，结果会分片逐个推送，而不是一次性返回。
 */
@AiService
public interface StreamingAssistant {

    /**
     * 与 AI 助手进行流式对话，逐片返回模型的回复片段。
     * <p>
     * {@code @SystemMessage} 指定发送给 LLM 的系统提示词，用来设定 AI 的角色和行为。
     *
     * @param userMessage 用户发来的消息（UserMessage），由框架自动构造
     * @return 包含模型回复片段的响应式流（Flux<String>），每片是一个增量文本
     */
    @SystemMessage("你是一个礼貌的助手")
    Flux<String> chat(String userMessage);
}
