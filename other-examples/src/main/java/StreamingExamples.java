import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.data.message.SystemMessage.systemMessage;
import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.model.openai.OpenAiLanguageModelName.GPT_3_5_TURBO_INSTRUCT;
import static java.util.Arrays.asList;

/**
 * 演示底层流式输出（Streaming）API 的两种用法：
 * <p>
 * 1. 流式聊天模型（StreamingChatModel）——用于多轮聊天；
 * 2. 流式语言模型（StreamingLanguageModel）——用于单轮文本补全。
 * <p>
 * 两者都通过回调接口逐段接收模型输出的内容，并用 CompletableFuture
 * 等待最终完整结果。
 */
public class StreamingExamples {

    /**
     * 示例：使用 {@link StreamingChatModel} 进行流式多轮聊天。
     * <p>
     * 通过自定义 {@link StreamingChatResponseHandler} 接收增量内容与最终响应。
     */
    static class StreamingChatModel_Example {

        public static void main(String[] args) {

            // 抱歉，"demo" API Key 不支持流式输出，请使用你自己的 Key。
            // 创建流式聊天模型
            StreamingChatModel model = OpenAiStreamingChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    .build();

            // 组装一组聊天消息：一条系统消息（设定角色为"非常喜欢讽刺的助手"）+ 一条用户消息
            List<ChatMessage> messages = asList(
                    systemMessage("You are a very sarcastic assistant"),
                    userMessage("Tell me a joke")
            );

            // 用于异步收集最终的完整响应
            CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();

            // 发起流式聊天：传入消息列表和一个流式响应处理器
            model.chat(messages, new StreamingChatResponseHandler() {

                // 每生成一段增量内容就会被回调，这里边接收边打印（不带换行）
                @Override
                public void onPartialResponse(String partialResponse) {
                    System.out.print(partialResponse);
                }

                // 全部生成完毕时回调，把完整响应填入 future
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    futureChatResponse.complete(completeResponse);
                }

                // 出错时回调，把异常填入 future
                @Override
                public void onError(Throwable error) {
                    futureChatResponse.completeExceptionally(error);
                }
            });

            futureChatResponse.join(); // 阻塞等待流式输出完成
        }
    }

    /**
     * 示例：使用 {@link StreamingLanguageModel} 进行流式单轮文本补全。
     * <p>
     * 通过 {@link StreamingResponseHandler} 接收逐个 token 的结果。
     */
    static class StreamingLanguageModel_Example {

        public static void main(String[] args) {

            // 抱歉，"demo" API Key 不支持流式输出，请使用你自己的 Key。
            // 创建流式语言模型
            StreamingLanguageModel model = OpenAiStreamingLanguageModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_3_5_TURBO_INSTRUCT)
                    .build();

            // 用于异步收集最终的完整响应
            CompletableFuture<Response<String>> futureResponse = new CompletableFuture<>();

            // 生成文本（单轮补全），传入一个流式响应处理器
            model.generate("Tell me a joke", new StreamingResponseHandler<>() {

                // 每生成一个 token 就会被回调，这里边接收边打印
                @Override
                public void onNext(String token) {
                    System.out.print(token);
                }

                // 生成完成时回调，把完整响应填入 future
                @Override
                public void onComplete(Response<String> response) {
                    futureResponse.complete(response);
                }

                // 出错时回调，把异常填入 future
                @Override
                public void onError(Throwable error) {
                    futureResponse.completeExceptionally(error);
                }
            });

            futureResponse.join(); // 阻塞等待流式输出完成
        }
    }
}
