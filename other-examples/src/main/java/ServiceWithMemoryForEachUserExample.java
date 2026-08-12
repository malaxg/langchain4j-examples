import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 演示为"每个用户"分别维护独立的聊天记忆。
 * <p>
 * 通过 {@code @MemoryId} 标记的用户标识（这里用 int），配合 chatMemoryProvider
 * 为不同的 memoryId 创建各自独立的 {@link MessageWindowChatMemory}，
 * 从而让不同用户之间互不干扰，各自拥有自己的对话上下文。
 * <p>
 * 相关持久化版本请参考 {@link ServiceWithPersistentMemoryForEachUserExample}。
 */
public class ServiceWithMemoryForEachUserExample {

    /**
     * 见 {@link ServiceWithPersistentMemoryForEachUserExample}（每个用户持久化记忆版本）。
     */

    // 定义 AI 服务接口
    interface Assistant {

        // @MemoryId 表示用户的唯一标识（用于区分记忆），@UserMessage 是用户消息正文
        String chat(@MemoryId int memoryId, @UserMessage String userMessage);
    }

    /**
     * 程序入口主方法。
     * <p>
     * 演示两个用户（memoryId 分别为 1 和 2）各自拥有独立记忆：
     * 用户 1 说自己叫 Klaus，用户 2 说自己叫 Francine，
     * 之后各自"我的名字是什么"都会得到各自的名字。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 装配 AI 服务：chatMemoryProvider 根据 memoryId 动态创建各自的记忆对象
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 用户 1（Klaus）打招呼并报出名字
        System.out.println(assistant.chat(1, "Hello, my name is Klaus"));
        // 你好 Klaus！今天需要我帮你做点什么吗？

        // 用户 2（Francine）打招呼并报出名字（与用户 1 共用同一模型但记忆独立）
        System.out.println(assistant.chat(2, "Hello, my name is Francine"));
        // 你好 Francine！今天需要我帮你做点什么吗？

        // 用户 1 问自己的名字 → 应回答 Klaus（说明记忆按用户隔离生效）
        System.out.println(assistant.chat(1, "What is my name?"));
        // 你的名字是 Klaus。

        // 用户 2 问自己的名字 → 应回答 Francine
        System.out.println(assistant.chat(2, "What is my name?"));
        // 你的名字是 Francine。
    }
}