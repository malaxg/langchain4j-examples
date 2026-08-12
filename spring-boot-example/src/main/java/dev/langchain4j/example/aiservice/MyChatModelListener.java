package dev.langchain4j.example.aiservice;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自定义的聊天模型监听器：监听每次模型调用的生命周期事件。
 * <p>
 * 实现 LangChain4j 的 {@link ChatModelListener} 接口，框架会在模型调用的三个关键节点
 * 回调对应的方法：请求发出前（onRequest）、收到响应后（onResponse）、调用出错时（onError）。
 * <p>
 * 本示例只把请求/响应/错误记录到日志中；在生产环境中，你可以在这些回调里做
 * 统一的审计、指标上报（如耗时、token 消耗统计）、告警、请求追踪等横切逻辑。
 */
public class MyChatModelListener implements ChatModelListener {

    // 使用 SLF4J 日志框架记录监听事件
    private static final Logger log = LoggerFactory.getLogger(MyChatModelListener.class);

    /**
     * 回调时机：请求发送给 LLM 之前。
     * <p>
     * 可以在这里记录完整的请求内容（提示词、参数等），用于审计和排查问题。
     *
     * @param requestContext 请求上下文，包含本次发给模型的完整 ChatRequest
     */
    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        log.info("请求发出: {}", requestContext.chatRequest());
    }

    /**
     * 回调时机：模型成功返回响应之后。
     * <p>
     * 可以在这里记录响应内容、token 消耗量、耗时等信息，用于成本核算和性能监控。
     *
     * @param responseContext 响应上下文，包含模型返回的完整 ChatResponse
     */
    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        log.info("响应返回: {}", responseContext.chatResponse());
    }

    /**
     * 回调时机：模型调用抛出异常时。
     * <p>
     * 可以在这里记录错误信息，实现错误告警；注意异常不会在这里被吞掉，
     * 它仍会继续向上抛出，由调用方决定如何处理。
     *
     * @param errorContext 错误上下文，包含调用模型时抛出的异常
     */
    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.info("调用出错: {}", errorContext.error().getMessage());
    }
}
