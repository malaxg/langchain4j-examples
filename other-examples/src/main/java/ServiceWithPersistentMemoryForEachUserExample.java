import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static org.mapdb.Serializer.INTEGER;
import static org.mapdb.Serializer.STRING;

/**
 * 演示为每个用户提供"持久化"的聊天记忆。
 * <p>
 * 结合 {@code @MemoryId}、chatMemoryProvider 与自定义的持久化 {@link ChatMemoryStore}：
 * 为不同用户（memoryId）各自创建独立的 MessageWindowChatMemory，
 * 并把聊天记录通过 MapDB 写入本地文件（multi-user-chat-memory.db），
 * 即使程序重启，各用户的对话上下文依然可以恢复。
 */
public class ServiceWithPersistentMemoryForEachUserExample {

    // 定义 AI 服务接口：@MemoryId 表示用户标识，@UserMessage 是用户消息正文
    interface Assistant {

        String chat(@MemoryId int memoryId, @UserMessage String userMessage);
    }

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建持久化存储 → 用 chatMemoryProvider 为每个用户创建持久化记忆 →
     * 装配 AI 服务 → 让用户 1、2 各自报出名字。
     * 若把下面两行注释掉、取消注释中两行再次运行，
     * 各用户仍能记起自己上一轮报出的名字（证明记忆已按用户持久化）。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 创建一个共享的持久化存储（MapDB 本地文件）
        PersistentChatMemoryStore store = new PersistentChatMemoryStore();

        // chatMemoryProvider：根据各用户的 memoryId 动态构建独立的持久化记忆，
        // manual .id(memoryId) 指定记忆标识，并共用同一个持久化存储
        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(store)
                .build();

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 装配 AI 服务：指定 chatMemoryProvider，让不同用户各用各的记忆
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(chatMemoryProvider)
                .build();

        // 用户 1 和用户 2 各自报出名字（会被分别持久化到本地文件）
        System.out.println(assistant.chat(1, "你好，我叫 Klaus"));
        System.out.println(assistant.chat(2, "嗨，我叫 Francine"));

        // 现在，把上面两行注释掉，取消下面两行注释，然后再次运行。
        // 由于记忆已按用户持久化，各用户仍能记起自己的名字。

        // System.out.println(assistant.chat(1, "我叫什么名字？"));
        // System.out.println(assistant.chat(2, "我叫什么名字？"));
    }

    /**
     * 自定义的 {@link ChatMemoryStore} 实现：把聊天记录以 JSON 形式存入 MapDB 本地文件。
     * <p>
     * 你也可以实现自己的 ChatMemoryStore，把聊天记忆存储到任何你希望的地方
     * （例如数据库、Redis 等）。这里用 memoryId（int）作为 key。
     */
    static class PersistentChatMemoryStore implements ChatMemoryStore {

        // 打开/创建本地数据库文件，并开启事务
        private final DB db = DBMaker.fileDB("multi-user-chat-memory.db").transactionEnable().make();
        // 用 memoryId(int) 作为 key、消息的 JSON 字符串作为 value 的持久化 Map
        private final Map<Integer, String> map = db.hashMap("messages", INTEGER, STRING).createOrOpen();

        /**
         * 根据记忆 ID 读取消息列表（从 JSON 反序列化为 ChatMessage）。
         *
         * @param memoryId 记忆标识
         * @return 该记忆下的聊天消息列表
         */
        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            String json = map.get((int) memoryId);
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
            map.put((int) memoryId, json);
            db.commit(); // 提交事务，确保持久化
        }

        /**
         * 删除指定记忆 ID 下的消息并提交事务。
         *
         * @param memoryId 记忆标识
         */
        @Override
        public void deleteMessages(Object memoryId) {
            map.remove((int) memoryId);
            db.commit(); // 提交事务
        }
    }
}
