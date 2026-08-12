import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 教程第 0 课：LangChain4j 的"你好，世界"（Hello World）入门示例。
 * <p>
 * 演示 LangChain4j 最基础的能力：创建一个 OpenAI 聊天模型（ChatModel），
 * 向它发送一条用户消息，并把模型的回答打印到控制台。
 * 整个流程只需要三行关键代码：构建模型 → 调用 chat() → 打印结果。
 */
public class _00_HelloWorld {

    /**
     * 程序入口 main 方法。
     *
     * @param args 命令行参数，本示例未使用
     */
    public static void main(String[] args) {

        // 1. 构建聊天模型：使用"建造者模式"（Builder Pattern）配置模型
        //    - apiKey:    指定 OpenAI 的 API Key（来自 ApiKeys 类）
        //    - modelName: 指定要使用的模型，GPT_4_O_MINI 是 OpenAI 的轻量级模型（又快又便宜）
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 2. 调用 chat() 方法，把用户消息发送给模型，并同步等待模型返回完整回答
        String answer = model.chat("打个招呼，说一句你好");

        // 3. 把模型的回答打印到控制台
        System.out.println(answer);
    }
}
