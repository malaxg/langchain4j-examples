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
 * 教程第 9 课：按用户隔离的"持久化"对话记忆。
 * <p>
 * 第 8 课的记忆只存在内存里，程序一重启就丢了。本课演示如何把对话记忆
 * 持久化到磁盘：通过实现 LangChain4j 的 ChatMemoryStore 接口，把记忆序列化成
 * JSON 字符串存入 MapDB（一个轻量级嵌入式数据库文件），程序重启后记忆依然还在。
 * <p>
 * 每个 memoryId（用户/会话标识）对应一份独立的记忆，
 * 这样多个用户各自拥有自己的历史上下文，互不串扰。
 */
public class _09_ServiceWithPersistentMemoryForEachUserExample {

    /**
     * AI 服务接口：助手。
     */
    interface Assistant {

        /**
         * 与助手对话（按 memoryId 隔离记忆，并持久化存储）。
         *
         * @param memoryId    用户/会话标识：不同 id 使用不同记忆
         * @param userMessage 本轮用户消息
         * @return 助手的回答
         */
        String chat(@MemoryId int memoryId, @UserMessage String userMessage);
    }

    /**
     * 程序入口 main 方法。
     *
     * @param args 命令行参数，本示例未使用
     */
    public static void main(String[] args) {

        // 1. 创建"持久化记忆存储"：记忆会被写入磁盘上的数据库文件
        PersistentChatMemoryStore store = new PersistentChatMemoryStore();

        // 2. 创建"记忆提供者"：为每个 memoryId 构建一份记忆。
        //    每份记忆最多保留 10 条消息，并通过 chatMemoryStore 落到磁盘上
        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(store)
                .build();

        // 3. 构建聊天模型
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 4. 组装 AI 服务：注入聊天模型 + 按用户提供记忆
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(chatMemoryProvider)
                .build();

        // 5. 第一次运行：用户 1 和用户 2 分别自我介绍，记忆被保存到磁盘
        System.out.println(assistant.chat(1, "你好，我叫克劳斯（Klaus）"));
        System.out.println(assistant.chat(2, "你好，我叫弗朗辛（Francine）"));

        // 6. 现在，把上面两行代码注释掉，并取消下面两行的注释，然后再次运行程序。
        //    你会发现尽管本次运行时"自我介绍"并未再次发生，
        //    但 AI 仍记得各用户的名字——因为记忆已经从上一次运行中持久化到了磁盘。

        // System.out.println(assistant.chat(1, "我叫什么名字？"));
        // System.out.println(assistant.chat(2, "我叫什么名字？"));
    }

    /**
     * ChatMemoryStore 的自定义实现：把对话记忆持久化到本地 MapDB 数据库。
     * <p>
     * 你可以仿照它实现自己的存储（比如存进关系型数据库），只要实现
     * getMessages / updateMessages / deleteMessages 三个方法即可。
     */
    static class PersistentChatMemoryStore implements ChatMemoryStore {

        // 创建（或打开）本地数据库文件 multi-user-chat-memory.db，并开启事务支持
        private final DB db = DBMaker.fileDB("multi-user-chat-memory.db").transactionEnable().make();
        // 在数据库中维护一张"memoryId -> JSON 字符串"的哈希表（键为整数、值为字符串）
        private final Map<Integer, String> map = db.hashMap("messages", INTEGER, STRING).createOrOpen();

        /**
         * 按记忆 id 取出历史消息列表。
         *
         * @param memoryId 记忆/会话标识
         * @return 该记忆下的历史消息列表（从 JSON 反序列化还原）
         */
        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            String json = map.get((int) memoryId);      // 从表中取出 JSON 字符串
            return messagesFromJson(json);              // 把 JSON 反序列化成 List<ChatMessage>
        }

        /**
         * 保存（覆盖）某个记忆 id 的历史消息。
         *
         * @param memoryId 记忆/会话标识
         * @param messages 最新的历史消息列表
         */
        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            String json = messagesToJson(messages);     // 把消息列表序列化成 JSON 字符串
            map.put((int) memoryId, json);              // 写回哈希表
            db.commit();                                // 提交事务，把改动真正落到磁盘
        }

        /**
         * 删除某个记忆 id 的历史消息。
         *
         * @param memoryId 记忆/会话标识
         */
        @Override
        public void deleteMessages(Object memoryId) {
            map.remove((int) memoryId);                 // 从表中移除该记忆
            db.commit();                                // 提交事务，把改动真正落到磁盘
        }
    }
}
