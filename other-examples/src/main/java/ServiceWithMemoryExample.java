import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 演示如何在 AI 服务中装配聊天记忆（ChatMemory），实现多轮对话。
 * <p>
 * 使用高级 API（AI Services）+ {@link ChatMemory}，让助手在多次对话之间
 * 记住上下文信息（例如用户名）。与前一个示例的区别是这里用 MessageWindowChatMemory
 * 按"消息条数"来限制记忆窗口。
 * <p>
 * 更多相关示例：{@link ServiceWithMemoryForEachUserExample}（每个用户各自的记忆）、
 * {@link ServiceWithPersistentMemoryExample}（持久化记忆）。
 * 低层 {@link ChatMemory} API 的用法见 {@link ChatMemoryExamples}。
 */
public class ServiceWithMemoryExample {

    // 定义 AI 服务接口
    interface Assistant {

        String chat(String message);
    }

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建带记忆的聊天模型 → 创建聊天记忆对象 → 通过 AiServices.builder
     * 装配模型与记忆 → 连续两轮对话验证助手能记住名字。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 创建聊天记忆：MessageWindowChatMemory 按消息条数限制窗口，这里最多保留 10 条
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 通过 AiServices.builder 装配 AI 服务：把聊天记忆注入 assistant
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory) // 注入记忆，使多轮对话具备上下文
                .build();

        // 第一轮对话：告诉助手自己的名字
        String answer = assistant.chat("你好！我叫 Klaus。");
        System.out.println(answer); // 你好 Klaus！今天需要我帮你做点什么吗？

        // 第二轮对话：由于有记忆，助手能记起名字并回答
        String answerWithName = assistant.chat("我叫什么名字？");
        System.out.println(answerWithName); // 你的名字是 Klaus。
    }
}
