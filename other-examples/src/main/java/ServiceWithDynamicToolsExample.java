import dev.langchain4j.code.judge0.Judge0JavaScriptExecutionTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static java.time.Duration.ofSeconds;

/**
 * 演示把"动态工具"装配进 AI 服务使用。
 * <p>
 * 这里使用的工具是 Judge0 JavaScript 执行工具
 * （通过 Judge0 RapidAPI 在云端执行 JavaScript 代码）。
 * 当 LLM 遇到需要精确计算的数学问题时，会调用该工具执行一段 JS 代码，
 * 从而得到准确的结果，而不是靠模型"猜"。
 */
public class ServiceWithDynamicToolsExample {

    // 定义 AI 服务接口
    interface Assistant {

        String chat(String message);
    }

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建 Judge0 工具 → 创建聊天模型 → 通过 AiServices.builder
     * 装配模型、记忆与工具 → 提出几个需要精确计算的数学问题。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 创建用于执行 JavaScript 的工具，构造时传入 Judge0 的 RapidAPI Key
        Judge0JavaScriptExecutionTool judge0Tool = new Judge0JavaScriptExecutionTool(ApiKeys.RAPID_API_KEY);

        // 创建聊天模型：temperature=0 让输出更确定，适合数学计算类任务
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .temperature(0.0)           // 温度设为 0，让回答更确定、更少随机性
                .timeout(ofSeconds(60))     // 设置请求超时时间为 60 秒
                .build();

        // 通过 AiServices.builder 装配 AI 服务：
        // chatMemory 提供最大 20 条消息的窗口记忆，tools 注册可被模型调用的工具
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(judge0Tool) // 让 LLM 可以调用 Judge0 执行 JS 来计算
                .build();

        // 提出需要精确计算的数学问题（模型会借助工具执行 JS 算出结果）
        interact(assistant, "What is the square root of 49506838032859?");
        interact(assistant, "Capitalize every third letter: abcabc");
        interact(assistant, "What is the number of hours between 17:00 on 21 Feb 1988 and 04:00 on 12 Apr 2014?");
    }

    /**
     * 辅助方法：向助手提问并打印用户与助手的对话内容。
     *
     * @param assistant    AI 助手对象
     * @param userMessage 用户发送的消息
     */
    private static void interact(Assistant assistant, String userMessage) {
        System.out.println("[User]: " + userMessage);
        String answer = assistant.chat(userMessage);
        System.out.println("[Assistant]: " + answer);
        System.out.println();
        System.out.println();
    }
}
