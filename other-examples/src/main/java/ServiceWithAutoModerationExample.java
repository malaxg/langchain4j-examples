import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiModerationModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Moderate;
import dev.langchain4j.service.ModerationException;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.model.openai.OpenAiModerationModelName.TEXT_MODERATION_LATEST;

/**
 * 演示 AI 服务的自动内容审核（AutoModeration）功能。
 * <p>
 * 通过 {@code @Moderate} 注解标记方法，并在装配 AI 服务时指定一个审核模型
 * （OpenAI Moderation 模型）。当用户输入或模型输出违反内容安全策略时，
 * 会抛出 {@link ModerationException} 异常。常用于过滤有害内容。
 */
public class ServiceWithAutoModerationExample {

    // 定义 AI 服务接口
    interface Chat {

        // @Moderate 表示该方法启用了自动审核（在调用前先检查输入是否违规）
        @Moderate
        String chat(String text);
    }

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建审核模型和聊天模型 → 装配到 AI 服务 → 尝试发送违规消息 → 捕获审核异常。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 创建 OpenAI 内容审核模型，用于检查输入/输出是否违反内容策略
        OpenAiModerationModel moderationModel = OpenAiModerationModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(TEXT_MODERATION_LATEST)
                .build();

        // 创建常规的聊天模型，负责实际对话
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 通过 AiServices.builder 手动装配 AI 服务：
        // 聊天模型负责对话，审核模型负责内容安全检查
        Chat chat = AiServices.builder(Chat.class)
                .chatModel(chatModel)
                .moderationModel(moderationModel)
                .build();

        try {
            // 发送一条带有攻击性/威胁性的消息，应该会被审核模型拦截
            chat.chat("I WILL KILL YOU!!!");
        } catch (ModerationException e) {
            // 捕获到审核异常，说明内容违反了安全策略
            System.out.println(e.getMessage());
            // 文本 "I WILL KILL YOU!!!" 违反了内容安全策略
        }
    }
}
