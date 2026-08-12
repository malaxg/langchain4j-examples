package dev.langchain4j.example.aiservice;

import dev.langchain4j.service.spring.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * REST 控制器：通过 HTTP 接口暴露 AI 能力（高层 API 用法示例）。
 * <p>
 * 这是使用 {@link AiService}（LangChain4j 高层 API）的示例，共提供两个端点：
 * 一个返回普通文本（非流式），一个以 Server-Sent Events（SSE）流式返回结果。
 */
@RestController
public class AssistantController {

    // 注入两个 AI 服务接口：Spring 会自动生成并注入它们的代理实现
    private final Assistant assistant;              // 非流式助手
    private final StreamingAssistant streamingAssistant; // 流式助手

    /**
     * 构造方法注入依赖：Spring 自动把代理实例传入。
     *
     * @param assistant           非流式的 AI 助手
     * @param streamingAssistant  流式的 AI 助手
     */
    public AssistantController(Assistant assistant, StreamingAssistant streamingAssistant) {
        this.assistant = assistant;
        this.streamingAssistant = streamingAssistant;
    }

    /**
     * 非流式聊天接口：一次性返回完整的模型回复。
     * <p>
     * 当模型需要调用 {@code currentTime()} 工具时，框架会自动触发工具调用，
     * 拿到结果后再次请求模型，最终返回合并后的完整文本。
     *
     * @param message 用户消息，来自 URL 查询参数 {@code message}，默认值为“现在几点？”
     * @return 模型生成的完整回复文本
     */
    @GetMapping("/assistant")
    public String assistant(@RequestParam(value = "message", defaultValue = "现在几点？") String message) {
        // 调用高层 AI 服务接口，底层所有逻辑（提示词、记忆、工具调用）由框架自动处理
        return assistant.chat(message);
    }

    /**
     * 流式聊天接口：以 Server-Sent Events（SSE）格式流式返回回复片段。
     * <p>
     * 返回类型 {@link Flux} 是 Reactor 的响应式流，每次 emit 一个文本片段，
     * 客户端可以一边生成一边显示，体验更接近 ChatGPT 的打字机效果。
     *
     * @param message 用户消息，来自 URL 查询参数 {@code message}，默认值为“现在几点？”
     * @return 包含模型回复片段的响应式流（Flux<String>）
     */
    @GetMapping(value = "/streamingAssistant", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamingAssistant(
            @RequestParam(value = "message", defaultValue = "现在几点？") String message) {
        // 调用流式 AI 服务接口，返回响应式数据流
        return streamingAssistant.chat(message);
    }
}
