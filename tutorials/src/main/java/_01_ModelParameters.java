import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;

/**
 * 教程第 1 课：配置模型参数。
 * <p>
 * 演示在构建 OpenAI 聊天模型时如何配置各种参数，例如：
 * temperature（随机性/创造性）、timeout（超时时间）、
 * logRequests/logResponses（日志开关，便于调试）。
 * 这些参数会直接影响模型生成回答的"性格"和可靠性。
 */
public class _01_ModelParameters {

    /**
     * 程序入口 main 方法。
     *
     * @param args 命令行参数，本示例未使用
     */
    public static void main(String[] args) {

        // OpenAI 各参数的含义可参考官方文档：https://platform.openai.com/docs/api-reference/chat/create
        // （下文会逐个介绍本例用到的参数）

        // 构建聊天模型并配置参数：

        // 准备要发送给模型的提示词（prompt）：让模型用三句话解释如何画出一幅漂亮的画
        String prompt = "用三句话解释如何画出一幅漂亮的画";

        // 把提示词发送给模型，同步获取回答
        String response = Model.MODEL.chat(prompt);

        // 打印模型回答
        System.out.println(response);
    }
}
