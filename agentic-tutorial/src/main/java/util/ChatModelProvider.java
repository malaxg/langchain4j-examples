package util;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 工具类：Chat 模型工厂。
 * 集中创建并返回一个 ChatModel（聊天模型），供所有 Agent 复用，
 * 避免在每个示例里重复编写模型初始化代码。
 * 默认使用 OpenAI（gpt-4o-mini），也可按需切换为 CEREBRAS 等快速推理提供商。
 */
public class ChatModelProvider {
    
    // 默认创建 OpenAI 模型并开启请求/响应日志
    public static ChatModel createChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName(GPT_4_O_MINI)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
    
    // 默认使用 OpenAI 提供商，并根据开关决定是否打印日志
    public static ChatModel createChatModel(boolean enableLogging) {
        return createChatModel("OPENAI", enableLogging);
    }
    
    // 使用指定提供商（默认开启日志）
    public static ChatModel createChatModel(String provider) {
        return createChatModel(provider, true);
    }
    
    // 核心方法：按提供商名称创建模型；CEREBRAS 使用快速推理 API，其余走 OpenAI
    public static ChatModel createChatModel(String provider, boolean enableLogging) {
        if ("CEREBRAS".equalsIgnoreCase(provider)) {
            return OpenAiChatModel.builder()
                    .baseUrl("https://api.cerebras.ai/v1")
                    .apiKey(System.getenv("CEREBRAS_API_KEY"))
                    .modelName("llama-4-scout-17b-16e-instruct")
                    .logRequests(enableLogging)
                    .logResponses(enableLogging)
                    .build();
        } else {
            return OpenAiChatModel.builder()
                    .apiKey(System.getenv("OPENAI_API_KEY"))
                    .modelName(GPT_4_O_MINI)
                    .logRequests(enableLogging)
                    .logResponses(enableLogging)
                    .build();
        }
    }
}