import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatResponseMetadata;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * OpenAiObservabilityExamples：演示 OpenAI 模型的可观测性（Observability）能力。
 * 通过注册 ChatModelListener 监听器，可以在请求发出前（onRequest）、收到响应后（onResponse）、
 * 以及出错时（onError）三个时机，观察请求/响应的详细信息（参数、token 用量、元数据等），
 * 非常适合做日志、监控和调试。
 */
public class OpenAiObservabilityExamples {

    // 观察 OpenAI 聊天模型的运行过程
    static class Observe_OpenAiChatModel {

        public static void main(String[] args) {

            // 定义一个监听器，在模型调用的各个阶段打印信息
            ChatModelListener listener = new ChatModelListener() {

                // 请求发出前触发：可以看到发给模型的完整请求内容
                @Override
                public void onRequest(ChatModelRequestContext requestContext) {
                    ChatRequest chatRequest = requestContext.chatRequest();

                    // 打印请求中的消息列表
                    List<ChatMessage> messages = chatRequest.messages();
                    System.out.println(messages);

                    // 打印通用请求参数
                    ChatRequestParameters parameters = chatRequest.parameters();
                    System.out.println(parameters.modelName());           // 模型名
                    System.out.println(parameters.temperature());         // 温度
                    System.out.println(parameters.topP());                // 核采样概率
                    System.out.println(parameters.topK());                // top-K 采样
                    System.out.println(parameters.frequencyPenalty());    // 频率惩罚
                    System.out.println(parameters.presencePenalty());     // 存在惩罚
                    System.out.println(parameters.maxOutputTokens());     // 最大输出 token
                    System.out.println(parameters.stopSequences());       // 停止序列
                    System.out.println(parameters.toolSpecifications());  // 工具规范
                    System.out.println(parameters.toolChoice());          // 工具选择策略
                    System.out.println(parameters.responseFormat());      // 响应格式

                    // 如果参数是 OpenAI 专属类型，还可以打印更多 OpenAI 专属参数
                    if (parameters instanceof OpenAiChatRequestParameters openAiParameters) {
                        System.out.println(openAiParameters.maxCompletionTokens()); // 最大补全 token
                        System.out.println(openAiParameters.logitBias());           // logit 偏置
                        System.out.println(openAiParameters.parallelToolCalls());   // 是否允许并行调用工具
                        System.out.println(openAiParameters.seed());                // 随机种子
                        System.out.println(openAiParameters.user());                // 终端用户标识
                        System.out.println(openAiParameters.store());               // 是否存储请求
                        System.out.println(openAiParameters.metadata());            // 元数据
                        System.out.println(openAiParameters.serviceTier());         // 服务等级
                        System.out.println(openAiParameters.reasoningEffort());     // 推理努力程度
                    }

                    // 打印模型供应商
                    System.out.println(requestContext.modelProvider());

                    // 属性（attributes）可以在请求→响应→错误三个阶段之间共享数据
                    Map<Object, Object> attributes = requestContext.attributes();
                    attributes.put("my-attribute", "my-value"); // 自定义属性，后续阶段可读取
                }

                // 收到完整响应后触发：可以看到模型返回的内容和元数据
                @Override
                public void onResponse(ChatModelResponseContext responseContext) {
                    ChatResponse chatResponse = responseContext.chatResponse();

                    // 打印模型生成的 AiMessage
                    AiMessage aiMessage = chatResponse.aiMessage();
                    System.out.println(aiMessage);

                    // 打印响应元数据
                    ChatResponseMetadata metadata = chatResponse.metadata();
                    System.out.println(metadata.id());          // 请求 id
                    System.out.println(metadata.modelName());   // 实际使用的模型名
                    System.out.println(metadata.finishReason()); // 结束原因（正常结束/达到长度上限/触发工具等）

                    // OpenAI 专属的响应元数据
                    if (metadata instanceof OpenAiChatResponseMetadata openAiMetadata) {
                        System.out.println(openAiMetadata.created());             // 创建时间戳
                        System.out.println(openAiMetadata.serviceTier());         // 服务等级
                        System.out.println(openAiMetadata.systemFingerprint());   // 系统指纹
                    }

                    // 打印 token 用量统计
                    TokenUsage tokenUsage = metadata.tokenUsage();
                    System.out.println(tokenUsage.inputTokenCount());  // 输入 token 数
                    System.out.println(tokenUsage.outputTokenCount()); // 输出 token 数
                    System.out.println(tokenUsage.totalTokenCount());  // 总 token 数
                    // OpenAI 专属的 token 明细（缓存命中、推理 token 等）
                    if (tokenUsage instanceof OpenAiTokenUsage openAiTokenUsage) {
                        System.out.println(openAiTokenUsage.inputTokensDetails().cachedTokens());   // 缓存的输入 token 数
                        System.out.println(openAiTokenUsage.outputTokensDetails().reasoningTokens()); // 推理 token 数
                    }

                    // 在响应阶段依然可以拿到对应的请求
                    ChatRequest chatRequest = responseContext.chatRequest();
                    System.out.println(chatRequest);

                    System.out.println(responseContext.modelProvider());

                    // 读取请求阶段写入的属性，验证跨阶段数据共享
                    Map<Object, Object> attributes = responseContext.attributes();
                    System.out.println(attributes.get("my-attribute"));
                }

                // 出错时触发：可以捕获异常并排查问题
                @Override
                public void onError(ChatModelErrorContext errorContext) {
                    Throwable error = errorContext.error();
                    error.printStackTrace(); // 打印异常堆栈

                    // 出错时也能拿到对应的请求内容
                    ChatRequest chatRequest = errorContext.chatRequest();
                    System.out.println(chatRequest);

                    System.out.println(errorContext.modelProvider());

                    // 读取请求阶段写入的属性
                    Map<Object, Object> attributes = errorContext.attributes();
                    System.out.println(attributes.get("my-attribute"));
                }
            };

            // 创建模型并把监听器注册进去
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(System.getenv("OPENAI_API_KEY")) // 直接从环境变量读取 API Key
                    .modelName(GPT_4_O_MINI)
                    .listeners(List.of(listener)) // 注册监听器，启用可观测性
                    .build();

            // 发起一次普通的聊天，观察监听器打印的信息
            model.chat("给我讲一个关于 Java 的笑话");
        }
    }
}
