import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 演示如何使用底层（low-level）的 {@link ChatMemory} API 手动管理聊天记忆。
 * <p>
 * 这是最底层的记忆用法：自己创建 {@link ChatMemory}，
 * 自己决定把哪些消息加入记忆，再丢给模型进行多轮对话。
 * 若想使用高级（high-level）的 AI Services 方式，请参考 {@link ServiceWithMemoryExample}。
 */
public class ChatMemoryExamples {

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建基于 Token 数量限制的聊天记忆 → 创建模型 →
     * 自行把用户消息加入记忆 → 用记忆里的全部消息调用模型 → 把模型回答再存回记忆。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 创建一个聊天记忆对象：
        // TokenWindowChatMemory 表示"窗口"由 Token 数量来决定；
        // 通过 withMaxTokens 限制最多保留 300 个 Token，
        // 由 OpenAiTokenCountEstimator 负责估算每条消息占用的 Token 数。
        ChatMemory chatMemory = TokenWindowChatMemory.withMaxTokens(300, new OpenAiTokenCountEstimator(GPT_4_O_MINI));

        // 创建 OpenAI 聊天模型实例（低层用法：直接用模型对话，不走 AI Services）。
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)     // 配置 API Key
                .modelName(GPT_4_O_MINI)            // 指定使用的模型名称
                .build();

        // 使用低层 ChatMemory API 时，你对记忆有完全的控制权：
        // 你可以自行决定是否把某条消息加入记忆
        // （例如为了省 Token，可能不想存储 few-shot 少样本示例）。
        // 在保存之前，你也可以按需对消息进行加工/修改。

        // 第一轮对话：先把用户消息加入记忆，再用记忆中的全部消息去调用模型。
        chatMemory.add(userMessage("你好，我叫 Klaus"));
        AiMessage answer = model.chat(chatMemory.messages()).aiMessage();
        System.out.println(answer.text()); // 你好 Klaus！今天需要我帮你做点什么吗？
        chatMemory.add(answer); // 把模型的回答也加入记忆

        // 第二轮对话：由于记忆里保存了上一轮的问答，模型此时应该能记得"Klaus"这个名字。
        chatMemory.add(userMessage("我叫什么名字？"));
        AiMessage answerWithName = model.chat(chatMemory.messages()).aiMessage();
        System.out.println(answerWithName.text()); // 你的名字是 Klaus。
        chatMemory.add(answerWithName);
    }
}
