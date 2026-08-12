import dev.langchain4j.code.judge0.Judge0JavaScriptExecutionTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;

/**
 * 教程第 11 课：动态工具（Dynamic Tools）。
 * <p>
 * 上一课的 Calculator 是我们自己写的工具；本课使用 LangChain4j 提供的现成工具：
 * Judge0JavaScriptExecutionTool——它借助 Judge0（一个在线代码执行服务，通过 RapidAPI 调用）
 * 让模型真正"运行 JavaScript 代码"来解答问题。
 * <p>
 * 这类工具叫"动态工具"：模型遇到需要精确计算的数学/文本处理问题时，
 * 会自己编写一段 JavaScript 代码，交给该工具在远端执行，再用执行结果组织回答。
 * <p>
 * 前提：需要在环境变量中配置 RAPID_API_KEY（见 ApiKeys 类）。
 */
public class _11_ServiceWithDynamicToolsExample {

    /**
     * AI 服务接口：助手。
     */
    interface Assistant {

        /**
         * 与助手对话（助手可调用 Judge0 工具执行 JavaScript）。
         *
         * @param message 用户消息
         * @return 助手的回答
         */
        String chat(String message);
    }

    /**
     * 程序入口 main 方法：依次向助手提出三个需要精确计算的问题。
     *
     * @param args 命令行参数，本示例未使用
     */
    public static void main(String[] args) {

        // 1. 创建 Judge0 工具实例（需要 RapidAPI 的 Key 来调用在线代码执行服务）
        Judge0JavaScriptExecutionTool judge0Tool = new Judge0JavaScriptExecutionTool(ApiKeys.RAPID_API_KEY);

        // 2. 构建聊天模型：temperature(0.0) 让回答尽可能确定（避免模型"自由发挥"影响计算题）
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .temperature(0.0)
                .timeout(ofSeconds(60))
                .build();

        // 3. 组装 AI 服务：注入聊天模型 + 对话记忆（最多 20 条）+ 动态工具
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(judge0Tool)
                .build();

        // 4. 依次提问（大数开平方、按规则改字符串、跨时区日期计算——
        //    这类问题靠"心算"容易出错，模型会写 JS 代码交给工具执行）
        interact(assistant, "49506838032859 的平方根是多少？");
        interact(assistant, "把字符串 abcabc 的每第三个字母变成大写");
        interact(assistant, "从 1988 年 2 月 21 日 17:00 到 2014 年 4 月 12 日 04:00 之间有多少个小时？");
    }

    /**
     * 与助手交互一次：打印用户消息、助手回答，并空两行分隔不同的问题。
     *
     * @param assistant   AI 服务助手实例
     * @param userMessage 本次要发送的用户消息
     */
    private static void interact(Assistant assistant, String userMessage) {
        System.out.println("[用户]: " + userMessage);      // 打印用户问题
        String answer = assistant.chat(userMessage);       // 调用助手，得到回答
        System.out.println("[AI 助手]: " + answer);        // 打印助手回答
        System.out.println();                              // 两个空行，分隔不同问题，便于阅读
        System.out.println();
    }
}
