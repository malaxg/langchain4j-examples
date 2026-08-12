import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 教程第 10 课：给 AI 服务注入"工具"（Tools / 函数调用）。
 * <p>
 * 普通聊天模型只会"动嘴"，而工具（Tool）让模型能够"动手"：
 * 模型在回答过程中如果觉得需要调用某个方法（比如算长度、求和、开平方），
 * 就会主动调用你注册的工具，拿到结果后再继续组织回答。
 * <p>
 * 实现步骤：1) 用 @Tool 注解把 Java 方法声明成工具；
 * 2) 通过 AiServices.builder().tools(...) 把工具注册进 AI 服务。
 * 模型会自动判断"这个问题该用哪个工具、传什么参数"。
 */
public class _10_ServiceWithToolsExample {

    // 该用法也可以在 spring-boot-example 模块中的
    // CustomerSupportApplication 和 CustomerSupportApplicationTest 里查看实际案例。

    /**
     * 计算器：其中的方法都被 @Tool 声明为可供 LLM 调用的工具。
     * <p>
     * 注意：这些方法不能有自定义的逻辑改动（本示例保持最简单），
     * @Tool 括号里的字符串是"工具描述"，模型根据描述来决定何时调用哪个工具。
     */
    static class Calculator {

        /**
         * 工具：计算字符串的长度。
         *
         * @param s 待计算的字符串
         * @return 字符串的字符个数
         */
        @Tool("计算字符串的长度")
        int stringLength(String s) {
            System.out.println("已调用 stringLength()，参数 s='" + s + "'");
            return s.length();
        }

        /**
         * 工具：计算两个数字的和。
         *
         * @param a 第一个加数
         * @param b 第二个加数
         * @return 两数之和
         */
        @Tool("计算两个数字的和")
        int add(int a, int b) {
            System.out.println("已调用 add()，参数 a=" + a + ", b=" + b);
            return a + b;
        }

        /**
         * 工具：计算一个数的平方根。
         *
         * @param x 待开平方的数
         * @return x 的平方根
         */
        @Tool("计算一个数的平方根")
        double sqrt(int x) {
            System.out.println("已调用 sqrt()，参数 x=" + x);
            return Math.sqrt(x);
        }
    }

    /**
     * AI 服务接口：助手。
     */
    interface Assistant {

        /**
         * 与助手对话（助手可调用注册好的工具来解题）。
         *
         * @param userMessage 用户消息
         * @return 助手的回答
         */
        String chat(String userMessage);
    }

    /**
     * 程序入口 main 方法。
     *
     * @param args 命令行参数，本示例未使用
     */
    public static void main(String[] args) {

        // 构建聊天模型。
        // 注意！工具的调用依赖真实的模型接口，使用 "demo" 的测试 API Key 时不支持工具功能
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .strictTools(true) // 开启"严格工具"模式：模型只能调用你注册的工具（参考文档见注释行 URL）
                .build();

        // 组装 AI 服务：注入聊天模型 + 工具（new Calculator()）+ 对话记忆
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new Calculator())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 提问：求"hello"和"world"两个单词字母个数之和的平方根。
        // 模型会依次调用 stringLength("hello")、stringLength("world")、add(5, 5)、sqrt(10) 四步来完成计算。
        // 注：hello 有 5 个字母，world 有 5 个字母，和为 10，10 的平方根约等于 3.162
        String question = "求 \"hello\" 和 \"world\" 两个单词字母个数之和的平方根是多少？";

        String answer = assistant.chat(question);

        System.out.println(answer);
        // 输出示例："hello" 和 "world" 两个单词字母个数之和的平方根约为 3.162。
    }
}
