import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 教程第 5 课：多轮对话记忆（Chat Memory）。
 * <p>
 * 普通聊天模型是无状态的：它不记得上一轮说了什么。而"对话记忆"组件
 * 会把历史消息保存起来，每次发请求时把历史一起发给模型，从而让模型能"记得"之前的对话。
 * <p>
 * 本课使用 TokenWindowChatMemory：按 Token 数量窗口限制记忆大小
 * （最多保存 1000 个 Token，超出的最旧消息会被自动丢弃）。
 * <p>
 * 对话流程：依次加入系统消息（设定身份）→ 用户消息 → 获取 AI 回复 → 把回复也加入记忆，
 * 这样下一轮提问时模型就能结合上文作答。
 */
public class _05_Memory {

    /**
     * 程序入口 main 方法：演示带记忆的两轮对话。
     *
     * @param args           命令行参数，本示例未使用
     * @throws ExecutionException 等待异步结果时出错（内部异常包装）
     * @throws InterruptedException 等待异步结果时线程被中断
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 构建流式聊天模型
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 创建对话记忆：最多保留 1000 个 Token。
        // 传入 OpenAiTokenCountEstimator 是为了用 OpenAI 的分词规则来统计每条消息占多少 Token
        ChatMemory chatMemory = TokenWindowChatMemory.withMaxTokens(1000, new OpenAiTokenCountEstimator(GPT_4_O_MINI));

        // 1. 创建"系统消息"：设定 AI 的身份和背景。
        //    告诉它它是一位资深开发者，正在给另一位资深开发者讲解一个电商平台项目
        SystemMessage systemMessage = SystemMessage.from(
                "你是一位资深开发者，正在向另一位资深开发者讲解，"
                        + "你们正在做的项目是一个电商平台，后端是 Java，"
                        + "数据库是 Oracle，并且使用了 Spring Data JPA");
        chatMemory.add(systemMessage);   // 把系统消息加入记忆

        // 2. 第一轮用户提问：如何为大型电商平台优化数据库查询（并要求简短回答）
        UserMessage userMessage1 = userMessage(
                "如何为大型电商平台优化数据库查询？请用三到五行简短回答。");
        chatMemory.add(userMessage1);    // 把用户消息加入记忆

        // 打印本轮的用户提问，并提示接下来打印 AI 的回答
        System.out.println("[用户]: " + userMessage1.singleText());
        System.out.print("[AI]: ");

        // 3. 用"记忆里的全部消息"调用流式模型，拿到第一轮 AI 回答（内部会流式打印）
        AiMessage aiMessage1 = streamChat(model, chatMemory);
        chatMemory.add(aiMessage1);      // 重要！把 AI 回答也加入记忆，模型才能记住本轮内容

        // 4. 第二轮用户提问：让 AI 针对第一点给出具体代码示例（这是"靠记忆才能回答"的追问）
        UserMessage userMessage2 = userMessage(
                "请针对你刚才说的第一点，给出一个具体的实现示例？要简短，代码最多 10 行。");
        chatMemory.add(userMessage2);    // 把第二轮的提问加入记忆

        System.out.println("\n\n[用户]: " + userMessage2.singleText());
        System.out.print("[AI]: ");

        // 5. 再次用记忆里的全部消息调用模型——此时包含第一轮问答，模型能记住上文并正确回答
        AiMessage aiMessage2 = streamChat(model, chatMemory);
        chatMemory.add(aiMessage2);      // 第二轮回答也加入记忆，为后续更多轮对话做准备
    }

    /**
     * 用记忆中的所有消息调用流式模型，边生成边打印，最后返回完整的 AI 消息。
     *
     * @param model      流式聊天模型
     * @param chatMemory 对话记忆（调用时会取出其中的全部历史消息发给模型）
     * @return 本次对话模型生成的完整 AiMessage（包含 AI 的完整回答文本）
     * @throws ExecutionException   等待异步结果时出错
     * @throws InterruptedException 等待异步结果时线程被中断
     */
    private static AiMessage streamChat(OpenAiStreamingChatModel model, ChatMemory chatMemory)
            throws ExecutionException, InterruptedException {

        // 用 CompletableFuture 桥接"异步回调"与"同步等待"
        CompletableFuture<AiMessage> futureAiMessage = new CompletableFuture<>();

        // 创建流式响应处理器：
        StreamingChatResponseHandler handler = new StreamingChatResponseHandler() {

            /**
             * 每生成一小段文本就立即打印出来（不换行，模拟打字机效果）。
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            /**
             * 生成完成时：把完整的 AiMessage 存入 future，让 get() 能拿到结果。
             */
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureAiMessage.complete(completeResponse.aiMessage());
            }

            /**
             * 出错时：这里选择忽略异常（保持示例简单）。
             */
            @Override
            public void onError(Throwable throwable) {
            }
        };

        // 把记忆中的全部历史消息发给模型（chatMemory.messages() 返回 List<ChatMessage>）
        model.chat(chatMemory.messages(), handler);
        // 阻塞等待生成完成，然后返回完整的 AI 消息
        return futureAiMessage.get();
    }
}
