package _1_basic_agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import util.ChatModelProvider;
import util.StringLoader;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.IOException;

/**
 * 示例主类（1a）：演示【最基础的 Agent 构建与调用】。
 *
 * <p>这是整套教程的第 1 个例子，演示如何使用 AgenticServices 的
 * agentBuilder 构建一个 Agent，并用 @UserMessage 中的提示词让它干活。
 *
 * <p>与后续例子的区别：
 * - 这里只有一个独立的 Agent，不涉及任何组合/工作流；
 * - 输出是自由文本 String（而 1b 是结构化 CV 对象）；
 * - 从 2a 开始才会把多个 Agent 串成“工作流(workflow)”。
 *
 * <p>作用流程（编排逻辑）：
 * 1. 定义模型 ChatModel
 * 2. 定义 Agent 行为（见 CvGenerator 接口）
 * 3. 用 AgenticServices.agentBuilder(...).chatModel(...).build() 构建 Agent
 * 4. 加载用户的“人生故事”文本
 * 5. 调用 cvGenerator.generateCv(lifeStory) 让 Agent 生成简历
 * 6. 打印结果
 */
public class _1a_Basic_Agent_Example {

    /**
     * 这段类级 Javadoc 原本位于类中部，是对整个例子的解释：
     * 本示例演示如何使用基本的 Agent 语法。
     * 注意：单独的 Agent 只有在与其他 Agent 组合时才真正有用，
     * 在下一步教程中我们会展示组合方式。如果只有一个 Agent，
     * 直接用 AiService 即可。
     * 这个基础 Agent 把用户的人生故事转换成一份清晰完整的简历。
     * 注意：因为生成的简历会很长、模型需要较长时间，所以运行较慢。
     */

    // 设置日志级别：控制能从模型调用中看到多少日志
    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);  // PRETTY=美化输出, 300=展示的长度/详细程度
    }

    // 1. 定义驱动该 Agent 的底层模型（ChatModel = 聊天大模型）
    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 2. 定义 Agent 的行为 —— 见同包下的 CvGenerator.java 接口

        // 3. 用 AgenticServices 创建/构建 Agent
        CvGenerator cvGenerator = AgenticServices
                .agentBuilder(CvGenerator.class)   // 指定要构建的 Agent 接口类型
                .chatModel(CHAT_MODEL)             // 指定驱动该 Agent 的大模型
                .outputKey("masterCv")             // 可选：定义该 Agent 输出对象的 key（键名）
                                                   // 这个 key 在未来组合工作流、在 AgenticScope 中传递变量时会用到
                .build();                          // 构建完成

        // 4. 从 resources/documents/user_life_story.txt 加载文本文件
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");

        // 5. 调用 Agent 生成简历（参数会自动注入到 @UserMessage 模板的 {{lifeStory}}）
        String cv = cvGenerator.generateCv(lifeStory);

        // 6. 打印生成的简历
        System.out.println("=== 简历 CV ===");
        System.out.println(cv);

        // 在例子 1b 中，我们会构建同样功能的 Agent，但采用结构化输出（返回 Cv 对象）
    }
}