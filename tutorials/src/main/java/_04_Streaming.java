import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 教程第 4 课：流式输出（Streaming）。
 * <p>
 * 前面课程用的是"一次性等待完整回答"（同步返回）；流式输出则是一边生成一边返回：
 * 模型每生成一小段文本，就会立刻回调 handler 的 onPartialResponse() 方法，
 * 让用户能看到"打字机"式的逐字输出效果，而不是干等很久。
 * <p>
 * 本课还顺带演示了用 OpenAiTokenCountEstimator 估算提示词的 Token 数量
 * （Token 是 LLM 处理文本的基本单位，计费也按 Token 计算）。
 */
public class _04_Streaming {

    /**
     * 程序入口 main 方法。
     *
     * @param args 命令行参数，本示例未使用
     */
    public static void main(String[] args) {

        // 构建流式聊天模型：与普通 ChatModel 不同的是要用 OpenAiStreamingChatModel

        // 准备提示词：让模型写一首关于程序员和空指针（null-pointer）的短诗
        String prompt = "请写一首关于程序员和空指针的搞笑短诗，最多 10 行";

        // 估算并打印这个提示词的"字符数"和"Token 数"，帮助理解两者区别（Token 数通常小于字符数）
        System.out.println("字符数：" + prompt.length());
        System.out.println("Token 数：" + new OpenAiTokenCountEstimator(GPT_4_O_MINI).estimateTokenCountInText(prompt));

        // 创建一个 CompletableFuture：用它来"等待"流式响应最终完成。
        // 因为流式接口是异步回调的，main 方法不能直接拿到返回值，
        // 所以先在回调里把结果放进 future，最后再 join() 阻塞等待。
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();

        // 调用流式聊天接口，并传入一个 StreamingChatResponseHandler 匿名类来处理三种事件：
        Model.STREAM_MODULE.chat(prompt, new StreamingChatResponseHandler() {

            /**
             * 每生成一小段文本就回调一次：把这一段立即打印出来（不换行，实现打字机效果）。
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            /**
             * 全部内容生成完毕时回调：打印结束提示，并让 future 正常完成（存下完整响应）。
             */
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("\n\n流式输出完成");
                futureChatResponse.complete(completeResponse);
            }

            /**
             * 生成过程中出错时回调：把异常放进 future，这样 join() 会抛出该异常。
             */
            @Override
            public void onError(Throwable error) {
                futureChatResponse.completeExceptionally(error);
            }
        });

        // 阻塞等待流式响应全部完成（一直等到 onCompleteResponse 或 onError 被调用）
        futureChatResponse.join();
    }
}
