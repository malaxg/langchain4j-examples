package dev.langchain4j.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 示例应用的启动类。
 * <p>
 * 这是 Spring Boot 3 项目的入口，通过 {@code @SpringBootApplication} 开启自动配置，
 * 它会自动扫描当前包及其子包下的组件（如 {@code aiservice} 包和 {@code lowlevel} 包），
 * 并在启动过程中创建并装配所有 LangChain4j 相关的 Bean（AI 服务、聊天模型、工具等）。
 */
@SpringBootApplication
public class ExampleApplication {

    /**
     * 应用主方法：程序从这里启动。
     *
     * @param args 命令行启动参数（通常为空）
     */
    public static void main(String[] args) {
        // 启动 Spring Boot 应用，加载所有配置并初始化内嵌的 Web 服务器（Tomcat）
        SpringApplication.run(ExampleApplication.class, args);
    }
}
