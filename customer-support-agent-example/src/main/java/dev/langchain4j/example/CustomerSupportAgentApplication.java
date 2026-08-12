package dev.langchain4j.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 客服 Agent 示例的 Spring Boot 启动类。
 *
 * <p>本类是应用的入口，负责启动整个 Spring Boot 应用上下文。
 * 启动后，框架会自动装配配置类中定义的所有 Bean（模型、记忆、工具、RAG 检索器等），
 * 并注册 REST 控制器，从而提供一个完整的"客服 Agent"服务。</p>
 */
@SpringBootApplication
public class CustomerSupportAgentApplication {

    /**
     * 应用入口方法，用于启动 Spring Boot 应用。
     *
     * @param args 命令行参数（一般不需要）
     */
    public static void main(String[] args) {
        SpringApplication.run(CustomerSupportAgentApplication.class, args);
    }
}
