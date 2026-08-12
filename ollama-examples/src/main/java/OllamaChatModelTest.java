import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import utils.AbstractOllamaInfrastructure;

import java.util.Map;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * OllamaChatModelTest：演示使用 Ollama 的"聊天模型"（ChatModel）的各种方式。
 * <p>
 * 角色/作用：通过继承 AbstractOllamaInfrastructure 复用 Ollama 启动逻辑，
 * 展示从最简单的直接对话，到让模型按 JSON Schema 结构化输出的多种用法。
 * 这些是"本地模型接入"最常见的配置场景，适合初学者逐一对照学习。
 * </p>
 * 配置要点（面向初学者）：
 * <ul>
 *   <li>.baseUrl(ollamaBaseUrl(ollama))：Ollama 服务地址（本机 http://localhost:11434 或测试容器端点）</li>
 *   <li>.modelName(MODEL_NAME)：使用的模型（llama3.1）</li>
 *   <li>.temperature(0.0)：温度设为 0，让输出更确定、可复现</li>
 *   <li>.responseFormat(...)：要求模型以 JSON 格式输出，并可通过 JsonSchema 约束字段</li>
 * </ul>
 */
class OllamaChatModelTest extends AbstractOllamaInfrastructure {

    /**
     * 如果你本机已经运行了 Ollama，
     * 请设置环境变量 OLLAMA_BASE_URL（例如 http://localhost:11434）。
     * 如果未设置该环境变量，
     * Testcontainers 会自动下载并启动 Ollama Docker 容器（可能需数分钟）。
     */

    /**
     * 测试用例一：最简单的对话。
     * 让模型给出"为什么 Java 很强大"的三个简短要点，并断言回答非空。
     */
    @Test
    void simple_example() {

        // 构建 Ollama 聊天模型：指定服务地址与模型
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama)) // Ollama 服务地址
                .modelName(MODEL_NAME)          // 模型：llama3.1
                .logRequests(true)              // 打印请求日志
                .build();

        // 直接发送一条消息并获取完整回答
        String answer = chatModel.chat("请用 3 条简短要点解释为什么 Java 很强大");
        System.out.println(answer);

        // 断言回答不是空白
        assertThat(answer).isNotBlank();
    }

    /**
     * 测试用例二：配合 AI 服务（AiServices）做 JSON Schema 结构化抽取。
     * 定义一个 PersonExtractor 接口，让模型从文本中抽取人物姓名和年龄，
     * 并自动映射成 Java 记录（record）对象。
     */
    @Test
    void json_schema_with_AI_Service_example() {

        // 定义一个记录（record）表示"人"
        record Person(String name, int age) {
        }

        // 声明一个 AI 服务接口：从文本中抽取 Person
        interface PersonExtractor {

            Person extractPersonFrom(String text);
        }

        // 构建支持 JSON Schema 输出能力的聊天模型
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .temperature(0.0)                                        // 温度 0，输出稳定
                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)     // 声明支持 JSON Schema 输出
                .logRequests(true)
                .build();

        // 通过 AiServices 把接口自动实现为一个 AI 服务
        PersonExtractor personExtractor = AiServices.create(PersonExtractor.class, chatModel);

        // 调用接口方法：从句子中抽取人物
        Person person = personExtractor.extractPersonFrom("John Doe 今年 42 岁");
        System.out.println(person);

        // 断言抽取结果正确
        assertThat(person).isEqualTo(new Person("John Doe", 42));
    }

    /**
     * 测试用例三：使用底层的 ChatRequest API（手动构造）输出 JSON。
     * 通过 ResponseFormat + JsonSchema 显式指定输出字段，再调用 chat(ChatRequest) 获取 JSON。
     */
    @Test
    void json_schema_with_low_level_chat_api_example() {

        // 构建基础聊天模型
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .logRequests(true)
                .build();

        // 定义响应格式：输出 JSON，并指定字段结构（name 字符串、age 整数）
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormatType.JSON)   // 类型：JSON
                .jsonSchema(JsonSchema.builder()
                        .rootElement(JsonObjectSchema.builder()
                                .addStringProperty("name")  // 字段 name：字符串
                                .addIntegerProperty("age")   // 字段 age：整数
                                .build())
                        .build())
                .build();

        // 把响应格式封装进请求参数
        ChatRequestParameters parameters = ChatRequestParameters.builder()
                .responseFormat(responseFormat)
                .build();

        // 构造聊天请求：包含消息和参数
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.from("John Doe 今年 42 岁"))
                .parameters(parameters)
                .build();

        // 发送请求并得到响应
        ChatResponse chatResponse = chatModel.chat(chatRequest);
        System.out.println(chatResponse);

        // 解析 JSON 并断言字段值正确
        assertThat(toMap(chatResponse.aiMessage().text())).isEqualTo(Map.of("name", "John Doe", "age", 42));
    }

    /**
     * 测试用例四：直接在模型构建器上配置响应格式。
     * 与用例三效果类似，但把 ResponseFormat 直接内联到 builder 上，代码更简洁。
     */
    @Test
    void json_schema_with_low_level_model_builder_example() {

        // 构建聊天模型，直接在 builder 上配置 JSON Schema 输出格式
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .responseFormat(ResponseFormat.builder()   // 指定 JSON 输出及字段结构
                        .type(ResponseFormatType.JSON)
                        .jsonSchema(JsonSchema.builder()
                                .rootElement(JsonObjectSchema.builder()
                                        .addStringProperty("name")
                                        .addIntegerProperty("age")
                                        .build())
                                .build())
                        .build())
                .logRequests(true)
                .build();

        // 请求模型从句子中抽取 JSON
        String json = chatModel.chat("提取：John Doe 今年 42 岁");
        System.out.println(json);

        // 解析并断言
        assertThat(toMap(json)).isEqualTo(Map.of("name", "John Doe", "age", 42));
    }

    /**
     * 测试用例五：最简单的 JSON 模式。
     * 用内置的 ResponseFormat.JSON 快捷方式，让模型返回 JSON，但不约束具体字段结构。
     */
    @Test
    void json_mode_with_low_level_model_builder_example() {

        // 构建聊天模型，使用快捷的 JSON 模式
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .temperature(0.0)
                .responseFormat(ResponseFormat.JSON) // 快捷方式：输出 JSON
                .logRequests(true)
                .build();

        // 请求模型生成包含 name 和 age 两个字段的 JSON 对象
        String json = chatModel.chat("给我一个 JSON 对象，包含 2 个字段：John Doe 的 name 和 42 岁的 age");
        System.out.println(json);

        // 解析并断言
        assertThat(toMap(json)).isEqualTo(Map.of("name", "John Doe", "age", 42));
    }

    /**
     * 工具方法：把 JSON 字符串解析成 Map 对象，便于断言。
     *
     * @param json JSON 字符串
     * @return 解析后的 Map
     */
    private static Map<String, Object> toMap(String json) {
        try {
            return new ObjectMapper().readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
