import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 演示如何把自定义方法定义为"工具"（@Tool），并装配进 AI 服务。
 * <p>
 * 把 Calculator 类里的方法用 {@code @Tool} 注解标记后注册给模型，
 * 当 LLM 遇到需要精确计算的问题（如字符串长度、加法、开平方）时，
 * 会主动调用这些工具方法而不是凭空猜测，从而得到准确结果。
 */
public class ServiceWithToolsExample {

    // 同时可以查看 spring-boot-example 模块中的
    // CustomerSupportApplication 和 CustomerSupportApplicationTest

    /**
     * 自定义的计算器工具类：每个 @Tool 方法都会暴露给 LLM 调用。
     */
    static class Calculator {

        // @Tool 注解用于描述这个工具的功能，字符串内容会作为工具说明发给 LLM
        @Tool("Calculates the length of a string")
        int stringLength(String s) {
            System.out.println("Called stringLength with s='" + s + "'");
            return s.length();
        }

        @Tool("Calculates the sum of two numbers")
        int add(int a, int b) {
            System.out.println("Called add with a=" + a + ", b=" + b);
            return a + b;
        }

        @Tool("Calculates the square root of a number")
        double sqrt(int x) {
            System.out.println("Called sqrt with x=" + x);
            return Math.sqrt(x);
        }
    }

    // 定义 AI 服务接口
    interface Assistant {

        String chat(String userMessage);
    }

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建聊天模型（开启严格工具模式）→ 装配 AI 服务并注册 Calculator 工具 →
     * 提出一个需要精确计算的数学问题，观察模型调用工具的整个过程。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .strictTools(true) // 开启严格工具输出模式。参考：https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-tools
                .build();

        // 装配 AI 服务：通过 .tools(new Calculator()) 把计算器工具注册给模型
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new Calculator()) // 注册工具，模型可主动调用
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 一个需要精确计算的问题：
        // 先分别求 "hello" 和 "world" 这两个单词的字母个数并相加，再开平方
        String question = "What is the square root of the sum of the numbers of letters in the words \"hello\" and \"world\"?";

        String answer = assistant.chat(question);

        System.out.println(answer);
        // "hello" 和 "world" 的字母个数之和的开平方，约为 3.162（模型通过调用工具算出）
    }
}
