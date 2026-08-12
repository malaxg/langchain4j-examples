package dev.langchain4j.example.aiservice;

import dev.langchain4j.example.lowlevel.ChatModelController;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * AI 服务相关的 Spring 配置类。
 * <p>
 * 这里手工声明了两个自定义 Bean：对话记忆（ChatMemory）和模型监听器（ChatModelListener）。
 * 这两个 Bean 会被 LangChain4j 的自动配置自动注入到所有 AI 服务 / 聊天模型中，
 * 属于“零侵入”的扩展点，非常适合在生产环境做统一的监控、日志、审计等横切逻辑。
 */
@Configuration
public class AssistantConfiguration {

    /**
     * 创建一个对话记忆 Bean。
     * <p>
     * 记忆的作用：让 AI 在多轮对话中记住上下文。这里使用 {@link MessageWindowChatMemory}
     * 固定只保留最近 10 条消息，防止上下文无限增长、浪费 token。
     * <p>
     * {@code @Scope(SCOPE_PROTOTYPE)} 表示每次注入时都创建一个新的实例（原型作用域），
     * 这样每个会话/客户端都能拥有独立的记忆，互不干扰；
     * 而默认的单例（Singleton）作用域会让所有请求共享同一份记忆。
     * <p>
     * 该 Bean 会自动被 {@link Assistant} 和 {@link StreamingAssistant} 这两个 AI 服务注入使用。
     *
     * @return 一个最多保留 10 条消息的滑动窗口对话记忆
     */
    @Bean
    @Scope(SCOPE_PROTOTYPE)
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }

    /**
     * 创建一个模型监听器 Bean。
     * <p>
     * 该监听器会被注入到应用上下文中找到的每一个 {@link ChatModel} 和 {@link StreamingChatModel}
     * 中，因此它既能监听到 {@link ChatModelController} 中使用的低层 ChatModel，
     * 也能监听到 {@link Assistant} 和 {@link StreamingAssistant} 这两个高层 AI 服务。
     * <p>
     * 监听器会在请求前（onRequest）、响应后（onResponse）、出错时（onError）三个时机被回调，
     * 是生产环境中实现日志、监控、成本统计等功能的推荐方式。
     *
     * @return 自定义的模型监听器实现
     */
    @Bean
    ChatModelListener chatModelListener() {
        return new MyChatModelListener();
    }
}
