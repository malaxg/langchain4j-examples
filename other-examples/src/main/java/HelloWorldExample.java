import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 最基础的 LangChain4j Hello World 示例。
 * <p>
 * 演示了如何创建一个聊天模型并直接与模型进行单次对话（没有记忆，没有 AI Services 封装）。
 * 这是学习 LangChain4j 的起点。
 */
public class HelloWorldExample {

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建 OpenAI 聊天模型 → 直接调用 model.chat(...) 发送一条消息 → 打印回答。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 创建模型实例：
        // 通过建造者（Builder）模式配置 API Key 和模型名称。
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY) // 配置 API Key
                .modelName(GPT_4_O_MINI)        // 指定使用的模型名称
                .build();

        // 开始与模型交互：把用户消息发给模型，并拿到回答字符串
        String answer = model.chat("Hello world!");

        System.out.println(answer); // 你好！今天需要我帮你做点什么吗？
    }
}
