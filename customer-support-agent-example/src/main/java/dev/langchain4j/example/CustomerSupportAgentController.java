package dev.langchain4j.example;

import dev.langchain4j.service.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服 Agent 的 REST 控制器，对外提供 HTTP 接口。
 *
 * <p>用户（或前端）通过 GET 请求把"会话ID"和"消息内容"发送给该接口，
 * 控制器再把它们转发给 {@link CustomerSupportAgent}，最后把 Agent 的回复返回给用户。</p>
 */
@RestController
public class CustomerSupportAgentController {

    // 注入 AI 客服 Agent（由 LangChain4j 自动生成的实现类）
    private final CustomerSupportAgent customerSupportAgent;

    /**
     * 构造器注入客服 Agent 实例。
     *
     * @param customerSupportAgent AI 客服 Agent（Spring 自动注入）
     */
    public CustomerSupportAgentController(CustomerSupportAgent customerSupportAgent) {
        this.customerSupportAgent = customerSupportAgent;
    }

    /**
     * 处理客服消息的 HTTP 接口。
     *
     * <p>接口路径保留英文原样，供前端调用，例如：
     * {@code GET /customerSupportAgent?sessionId=xxx&userMessage=hello}</p>
     *
     * @param sessionId   会话ID，用于区分不同用户，隔离各自的对话记忆
     * @param userMessage 用户发给客服的消息
     * @return 客服的回复文本（只取结果内容，不包含工具调用等元数据）
     */
    @GetMapping("/customerSupportAgent")
    public String customerSupportAgent(@RequestParam String sessionId, @RequestParam String userMessage) {
        // 调用 Agent，得到结构化的结果对象
        Result<String> result = customerSupportAgent.answer(sessionId, userMessage);
        // 只返回回复内容
        return result.content();
    }
}
