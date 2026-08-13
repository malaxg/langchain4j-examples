import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 最简单的"AI 服务"（AI Services）入门示例。
 * <p>
 * 演示如何用 {@link AiServices} 把一个普通接口自动实现为
 * 能调用 LLM 的代理对象：只需定义接口 + 创建模型，然后一行创建服务即可对话。
 */
public class SimpleServiceExample {

    // 定义 AI 服务接口：方法签名中只有一个 String 入参和一个 String 返回值，
    // LangChain4j 会把方法调用自动转换为一次 LLM 对话
    interface Assistant {

        String chat(String message);
    }

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建聊天模型 → 用 AiServices.create 生成 Assistant 代理 →
     * 调用接口方法与 LLM 对话 → 打印回答。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY) // 配置 API Key
                .modelName(GPT_4_O_MINI)        // 指定模型名称
                .build();

        // 使用 AiServices.create 根据接口生成代理对象，模型负责实际对话
        Assistant assistant = AiServices.create(Assistant.class, chatModel);

        // 调用接口方法与模型对话
        String answer = assistant.chat("你好");

        System.out.println(answer); // 你好！今天需要我帮你做点什么吗？
    }
}
