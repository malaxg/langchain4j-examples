package shared;

/**
 * 这是一个 "AI Service"（AI 服务）。它是一个具有 AI 能力的 Java 服务接口。
 * 它可以像其他服务一样集成到你的代码中，可以作为 Spring 的 Bean 使用，也可以被 mock 用于测试。
 * 目标是让你以最小的成本把 AI 功能无缝集成到现有代码中。
 * 它的概念与 Spring Data JPA 或 Retrofit 类似：
 * 你只需要定义一个接口，并（可选地）用注解来定制它。
 * LangChain4j 会通过代理（proxy）和反射（reflection）为这个接口生成实现。
 * 这种方式把所有的复杂性和样板代码都封装了起来，
 * 你不需要再手动管理模型、消息、记忆、RAG 组件、工具、输出解析器等。
 * 不过不用担心，它非常灵活和可配置，你可以根据具体需求来定制它。
 * <br>
 * 更多信息请看：https://docs.langchain4j.dev/tutorials/ai-services
 */
public interface Assistant {

    /**
     * 让 AI 助手回答一个问题。
     *
     * @param query 用户提出的问题
     * @return 模型的回答文本
     */
    String answer(String query);
}
