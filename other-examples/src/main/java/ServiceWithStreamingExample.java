import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 演示 AI 服务的流式输出（Streaming）功能。
 * <p>
 * 使用流式聊天模型 {@link StreamingChatModel} 和 {@link TokenStream}，
 * 让 LLM 的回答内容像打字机一样逐段/逐 token 输出，不必等待全部生成完毕，
 * 适合对实时性要求较高的场景。
 */
public class ServiceWithStreamingExample {

    // 定义 AI 服务接口：返回类型 TokenStream 表示以流式方式接收回答
    interface Assistant {

        TokenStream chat(String message);
    }

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建流式聊天模型 → 装配 AI 服务 → 启动流式对话 →
     * 通过回调收集增量内容和最终完整响应。
     *
     * @param args 命令行参数（本示例不使用）
     * @throws Exception 等待响应的阻塞过程可能抛出超时等异常
     */
    public static void main(String[] args) throws Exception {

        // 抱歉，"demo" API Key 尚不支持流式输出，请使用你自己的 Key。
        // 创建流式聊天模型：模型会在生成过程中不断回调结果
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 创建 AI 服务（返回 TokenStream）
        Assistant assistant = AiServices.create(Assistant.class, model);

        // 启动流式对话：此时不会立即返回完整结果，而是返回一个 TokenStream
        TokenStream tokenStream = assistant.chat("给我讲个笑话");

        // 用一个 Future 来异步收集最终的完整响应
        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        // 配置流式回调链：
        // onPartialResponse：每生成一段增量内容就打印（System.out::print 边接收边打印，不带换行）
        // onCompleteResponse：全部生成完毕时，把完整响应填入 future
        // onError：出错时把异常填入 future
        // start()：真正开始流式请求
        tokenStream.onPartialResponse(System.out::print)
                .onCompleteResponse(futureResponse::complete)
                .onError(futureResponse::completeExceptionally)
                .start();

        // 阻塞等待最多 30 秒，拿到完整响应
        ChatResponse chatResponse = futureResponse.get(30, SECONDS);
        System.out.println("\n" + chatResponse);
    }
}
