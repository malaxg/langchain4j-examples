package dev.langchain4j.example.aiservice;

import dev.langchain4j.agent.tool.Tool;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * AI 工具类：定义可供 LLM 调用的函数（Function Calling / 工具调用）。
 * <p>
 * 关键概念：LLM 本身不能访问现实世界的数据。当模型判断需要“当前时间”这类信息时，
 * 它会在回复中声明要调用某个工具（而不是直接给答案），由本类的方法真正去执行，
 * 再把结果返回给模型，模型据此生成最终回答。这就是“Agent 调用工具”的核心机制。
 * <p>
 * 工具实例来自该类的 Spring Bean（{@code @Component}），LangChain4j 会自动发现
 * 容器中所有标注了 {@code @Tool} 的方法并暴露给 AI 服务使用。
 */
@Component
public class AssistantTools {

    /**
     * 获取当前时间（示例工具）。
     * <p>
     * {@code @Tool} 注解把该方法注册为 AI 可调用的工具，方法名会被作为工具名暴露给模型。
     * {@code @Observed} 是 Micrometer 的观测注解，用于在 Spring Boot 3 中自动记录
     * 该方法的调用指标（耗时、次数等），方便监控工具执行情况。
     *
     * @return 当前时间的字符串表示，如 "10:15:30"
     */
    @Tool
    @Observed
    public String currentTime() {
        // 获取当前本地时间并转为字符串，作为工具执行结果返回给模型
        return LocalTime.now().toString();
    }
}
