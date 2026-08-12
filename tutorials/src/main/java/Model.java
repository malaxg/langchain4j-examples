import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;

public class Model {
    public static final ChatModel MODEL = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)   // OpenAI 的 API Key
            .modelName(GPT_4_O_MINI)          // 使用的模型名称
            .temperature(0.6)                  // "温度"参数，取值 0~2。越小回答越确定/保守，越大越随机/有创意（0.3 属于比较稳定）
            .timeout(ofSeconds(100))            // 请求超时时间：60 秒。超过则抛出异常，避免无限等待
            .logRequests(true)                 // 打印发送给 OpenAI 的请求日志，便于排查问题
            .logResponses(true)                // 打印 OpenAI 返回的响应日志，便于查看模型原始输出
            .build();                          // 调用 build() 完成建造，得到可用的模型对象

    public static final OpenAiStreamingChatModel STREAM_MODULE = OpenAiStreamingChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .temperature(0.6)                  // "温度"参数，取值 0~2。越小回答越确定/保守，越大越随机/有创意（0.3 属于比较稳定）
            .timeout(ofSeconds(100))
            .logRequests(true)                 // 打印发送给 OpenAI 的请求日志，便于排查问题
            .logResponses(true)                // 打印 OpenAI 返回的响应日志，便于查看模型原始输出
            .build();
}
