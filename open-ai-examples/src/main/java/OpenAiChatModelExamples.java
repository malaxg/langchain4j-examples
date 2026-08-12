import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * OpenAiChatModelExamples：演示如何使用 OpenAI 的 Chat 对话模型。
 * 包含四个示例：
 * 1. 最简单的文本对话（单轮提问）；
 * 2. 图文混合输入（把图片连同文字一起发给模型）；
 * 3. 设置通用的请求参数（ChatRequestParameters）；
 * 4. 设置 OpenAI 专属的请求参数（OpenAiChatRequestParameters）。
 */
public class OpenAiChatModelExamples {

    // 示例一：最基础的文本对话
    static class Simple_Prompt {

        public static void main(String[] args) {

            // 使用构建器模式创建 OpenAI 聊天模型
            ChatModel chatModel = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY) // 传入 API Key（来自环境变量或默认值）
                    .modelName(GPT_4_O_MINI)        // 指定使用的模型名（GPT-4o mini）
                    .build();

            // 直接向模型提问，返回模型的文本回答
            String joke = chatModel.chat("给我讲一个关于 Java 的笑话");

            // 打印模型的回答
            System.out.println(joke);
        }
    }

    // 示例二：图文混合输入（视觉理解，vision）
    static class Image_Inputs {

        public static void main(String[] args) {

            ChatModel chatModel = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY) // 请使用你自己的 OpenAI API Key
                    .modelName(GPT_4_O_MINI)
                    .maxTokens(50) // 限制模型最多生成的 token 数量
                    .build();

            // 构造一条用户消息：同时包含文本和图片内容
            UserMessage userMessage = UserMessage.from(
                    TextContent.from("你看到了什么？"), // 文本内容
                    ImageContent.from("https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png") // 图片内容（URL）
            );

            // 把图文消息发给模型，得到响应
            ChatResponse chatResponse = chatModel.chat(userMessage);

            // 打印模型回答中的文本
            System.out.println(chatResponse.aiMessage().text());
        }
    }

    // 示例三：设置通用（跨厂商通用的）请求参数
    static class Setting_Common_ChatRequestParameters {

        public static void main(String[] args) {

            // 定义默认的请求参数（模型会用这些默认值发起对话）
            ChatRequestParameters defaultParameters = ChatRequestParameters.builder()
                    .modelName("gpt-4o")        // 模型名
                    .temperature(0.7)           // 采样温度：值越低回答越确定，越高越有创造性
                    .maxOutputTokens(100)       // 最大输出 token 数
                    // 还有很多其他通用参数，详情参见 ChatRequestParameters
                    .build();

            // 创建聊天模型并把默认参数设置进去
            ChatModel chatModel = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .defaultRequestParameters(defaultParameters) // 设置模型默认请求参数
                    .logRequests(true) // 打印请求日志，方便调试
                    .build();

            // 针对单次请求单独指定参数（会与默认参数合并，且覆盖默认值）
            ChatRequestParameters parameters = ChatRequestParameters.builder()
                    .modelName("gpt-4o-mini")
                    .temperature(1.0)
                    .maxOutputTokens(50)
                    .build();

            // 构造一次完整的聊天请求：消息 + 参数
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(UserMessage.from("给我讲一个关于 Java 的搞笑故事"))
                    .parameters(parameters) // 与默认参数合并，并覆盖同名默认值
                    .build();

            ChatResponse chatResponse = chatModel.chat(chatRequest);

            // 打印完整的响应对象（包含消息、元数据等）
            System.out.println(chatResponse);
        }
    }

    // 示例四：设置 OpenAI 专属的请求参数（seed 等）
    static class Setting_OpenAI_Specific_ChatRequestParameters {

        public static void main(String[] args) {

            OpenAiChatRequestParameters defaultParameters = OpenAiChatRequestParameters.builder()
                    .seed(12345) // seed：OpenAI 专属参数，固定随机种子让输出更可复现
                    // 还有很多其他 OpenAI 专属参数，详情参见 OpenAiChatRequestParameters
                    .modelName("gpt-4o") // 通用参数
                    .temperature(0.7)    // 通用参数
                    .maxOutputTokens(100) // 通用参数
                    .build();

            ChatModel chatModel = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .defaultRequestParameters(defaultParameters) // 设置模型默认请求参数
                    .logRequests(true) // 打印请求日志
                    .build();

            // 针对单次请求指定 OpenAI 专属参数
            OpenAiChatRequestParameters parameters = OpenAiChatRequestParameters.builder()
                    .seed(67890)         // OpenAI 专属参数：本次请求的随机种子
                    .modelName("gpt-4o-mini") // 通用参数
                    .temperature(1.0)    // 通用参数
                    .maxOutputTokens(50) // 通用参数
                    .build();

            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(UserMessage.from("给我讲一个关于 Java 的搞笑故事"))
                    .parameters(parameters) // 与默认参数合并，并覆盖同名默认值
                    .build();

            ChatResponse chatResponse = chatModel.chat(chatRequest);

            System.out.println(chatResponse);
        }
    }
}
