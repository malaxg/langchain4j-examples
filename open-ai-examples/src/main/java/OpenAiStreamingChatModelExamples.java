import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * OpenAiStreamingChatModelExamples：演示 OpenAI 的流式（Streaming）聊天能力。
 * 流式模式下，模型不是一次性返回完整回答，而是把回答分成多块逐步推送给客户端，
 * 适合需要“打字机”式实时输出效果的场景（如聊天机器人逐字显示回答）。
 */
public class OpenAiStreamingChatModelExamples {

    public static void main(String[] args) {

        // 创建流式聊天模型（注意：这里用的是 StreamingChatModel 接口）
        StreamingChatModel chatModel = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 用 CompletableFuture 阻塞主线程，等待流式对话完成（异步回调模型）
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();

        // 发起流式对话，并注册一个回调处理器
        chatModel.chat("给我讲一个关于 Java 的笑话", new StreamingChatResponseHandler() {

            // 每次收到一块（部分）回答时被调用，这里直接把内容打印出来，实现逐字输出
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse); // 注意是 print，不是 println
            }

            // 整个回答流结束时被调用，把完整响应放入 Future，解除主线程阻塞
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureChatResponse.complete(completeResponse);
            }

            // 出错时被调用，把异常放入 Future
            @Override
            public void onError(Throwable error) {
                futureChatResponse.completeExceptionally(error);
            }
        });

        // 阻塞等待流式响应全部完成
        futureChatResponse.join();
    }
}
