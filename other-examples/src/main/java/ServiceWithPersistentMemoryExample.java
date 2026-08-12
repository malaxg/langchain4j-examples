import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static org.mapdb.Serializer.STRING;

/**
 * 演示使用持久化聊天记忆（Persistent ChatMemory）。
 * <p>
 * 与内存版不同，这里自定义了一个 {@link ChatMemoryStore}，借助 MapDB
 * 把聊天记录存成文件（chat-memory.db）。这样即使程序重启，
 * 之前存下的对话上下文依然可以恢复。
 * <p>
 * 相关示例：{@link ServiceWithMemoryExample}（内存记忆）、
 * {@link ServiceWithPersistentMemoryForEachUserExample}（每个用户持久化记忆）。
 */
public class ServiceWithPersistentMemoryExample {

    /**
     * 见 {@link ServiceWithMemoryExample} 和 {@link ServiceWithPersistentMemoryForEachUserExample}。
     */

    // 定义 AI 服务接口
    interface Assistant {

        String chat(String message);
    }

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建带持久化存储的聊天记忆 → 装配 AI 服务 → 第一轮对话保存名字。
     * 若把下面第 1、2 行注释掉，改用注释中的两行（直接询问名字）再次运行，
     * 助手仍能回忆起上一轮保存的名字，从而证明记忆被持久化。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 创建聊天记忆：指定自定义的持久化存储 PersistentChatMemoryStore
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)                            // 最多保留 10 条消息
                .chatMemoryStore(new PersistentChatMemoryStore()) // 使用持久化存储，写入本地文件
                .build();

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 装配 AI 服务
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .build();

        // 第一轮对话：告诉助手自己的名字（此记录会写入本地文件）
        String answer = assistant.chat("Hello! My name is Klaus.");
        System.out.println(answer); // 你好 Klaus！今天需要我帮你做点什么吗？

        // 现在，把上面这两行注释掉，取消下面两行的注释，然后再次运行。
        // 由于记忆已持久化，助手仍能记起上一轮的对话（名字是 Klaus）。

        // String answerWithName = assistant.chat("What is my name?");
        // System.out.println(answerWithName); // 你的名字是 Klaus。
    }

    // 也可以实现你自己的 ChatMemoryStore，把聊天记忆存储到任何你希望的地方（如数据库、Redis 等）
    static class PersistentChatMemoryStore implements ChatMemoryStore {

        // 打开/创建本地数据库文件，并开启事务
        private final DB db = DBMaker.fileDB("chat-memory.db").transactionEnable().make();
        // 用 memoryId 作为 key、消息的 JSON 字符串作为 value 的持久化 Map
        private final Map<String, String> map = db.hashMap("messages", STRING, STRING).createOrOpen();

        /**
         * 根据记忆 ID 读取消息列表（从 JSON 反序列化为 ChatMessage）。
         *
         * @param memoryId 记忆标识
         * @return 该记忆下的聊天消息列表
         */
        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            String json = map.get((String) memoryId);
            return messagesFromJson(json);
        }

        /**
         * 把消息列表保存（序列化为 JSON）到指定记忆 ID 下并提交事务。
         *
         * @param memoryId 记忆标识
         * @param messages 需要保存的聊天消息列表
         */
        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            String json = messagesToJson(messages);
            map.put((String) memoryId, json);
            db.commit(); // 提交事务，确保持久化
        }

        /**
         * 删除指定记忆 ID 下的消息并提交事务。
         *
         * @param memoryId 记忆标识
         */
        @Override
        public void deleteMessages(Object memoryId) {
            map.remove((String) memoryId);
            db.commit(); // 提交事务
        }
    }
}
