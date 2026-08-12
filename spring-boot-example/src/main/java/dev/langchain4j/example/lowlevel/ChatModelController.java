package dev.langchain4j.example.lowlevel;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST 控制器：演示 LangChain4j 的低层 API 用法。
 * <p>
 * 与高层 {@code @AiService} 不同，低层 API 直接操作 {@link ChatModel} 对象，
 * 需要你自己拼接消息、处理对话历史和工具调用等逻辑。它更灵活，但代码量更多，
 * 适合需要精细控制请求的场景；大多数情况下推荐使用高层 API。
 */
@RestController
public class ChatModelController {

    // 注入低层的聊天模型对象：由 Spring Boot 自动配置根据 application.yml 中的 spring.ai.* 配置创建
    private final ChatModel chatModel;

    /**
     * 构造方法注入依赖：Spring 自动把 ChatModel 实例传入。
     *
     * @param chatModel 低层聊天模型实例（如 OpenAI、Ollama 等提供商）
     */
    public ChatModelController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 直接调用低层聊天模型完成一次对话。
     * <p>
     * 这里没有系统提示词、记忆或工具，仅仅是把用户消息原样发给模型并返回回复，
     * 用来演示低层 API 最基本的调用方式。
     *
     * @param message 用户消息，来自 URL 查询参数 {@code message}，默认值为“你好”
     * @return 模型生成的回复文本
     */
    @GetMapping("/model")
    public String model(@RequestParam(value = "message", defaultValue = "你好") String message) {
        // 把用户消息直接发送给模型并同步等待完整回复
        return chatModel.chat(message);
    }
}
