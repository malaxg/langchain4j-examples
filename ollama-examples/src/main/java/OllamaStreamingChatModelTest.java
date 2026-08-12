import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import utils.AbstractOllamaInfrastructure;

import java.util.concurrent.CompletableFuture;

/**
 * OllamaStreamingChatModelTest：演示如何使用 Ollama 的"流式聊天模型"（StreamingChatModel）。
 * <p>
 * 角色/作用：通过继承 AbstractOllamaInfrastructure 复用 Ollama 的启动逻辑，
 * 展示流式输出——即模型不是一次性给出完整答案，而是把内容分成一段一段（partialResponse）
 * 逐步推送过来，适合实现打字机式效果。
 * </p>
 * 配置要点（面向初学者）：
 * <ul>
 *   <li>.baseUrl(ollamaBaseUrl(ollama))：连接本机已装的 Ollama（http://localhost:11434）
 *       或 Testcontainers 启动的容器端点</li>
 *   <li>.modelName(MODEL_NAME)：指定使用的模型（llama3.1）</li>
 * </ul>
 */
@Testcontainers
class OllamaStreamingChatModelTest extends AbstractOllamaInfrastructure {

    /**
     * 如果你本机已经运行了 Ollama，
     * 请设置环境变量 OLLAMA_BASE_URL（例如 http://localhost:11434）。
     * 如果未设置该环境变量，
     * Testcontainers 会自动下载并启动 Ollama Docker 容器（可能需数分钟）。
     * 若使用 Docker 方案，需保证本机已安装并运行 Docker。
     */

    /**
     * 测试用例：流式对话示例。
     * 请求模型生成一篇关于 Java 与 AI 的 100 词诗歌，并通过回调逐段接收输出。
     */
    @Test
    void streaming_example() {

        // 构建 Ollama 流式聊天模型，连接基础设施提供的服务地址与模型
        StreamingChatModel model = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama)) // Ollama 服务宿主地址
                .modelName(MODEL_NAME)          // 使用的模型：llama3.1
                .build();

        // 用户消息
        String userMessage = "请写一首关于 Java 和 AI 的 100 词诗";

        // 用一个 CompletableFuture 封装最终的完整响应，便于等待流式过程结束
        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        // 调用流式对话，并注册回调处理器
        model.chat(userMessage, new StreamingChatResponseHandler() {

            /**
             * 每收到一段增量内容时回调：把这一段字符打印出来，实现逐步输出。
             *
             * @param partialResponse 本次推送的增量文本片段
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            /**
             * 流式响应全部结束并汇成完整响应时回调。
             *
             * @param completeResponse 完整的最终聊天响应
             */
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureResponse.complete(completeResponse); // 让主线程知道已经结束
            }

            /**
             * 出错时回调，把异常传给 future。
             *
             * @param error 发生的异常
             */
            @Override
            public void onError(Throwable error) {
                futureResponse.completeExceptionally(error);
            }
        });

        // 阻塞等待整个流式过程完成
        futureResponse.join();
    }
}
