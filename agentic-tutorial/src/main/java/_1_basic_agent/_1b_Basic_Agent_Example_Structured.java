package _1_basic_agent;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import domain.Cv;
import util.ChatModelProvider;
import util.StringLoader;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.IOException;

/**
 * 示例主类（1b）：演示【结构化输出的 Agent】。
 *
 * <p>与 1a 的区别：本示例实现的是同一个 CvGenerator Agent，
 * 区别在于返回类型变成了自定义 Java 对象 {@code Cv}（见 domain/Cv.java），
 * 而不是自由文本 String。LLM 会按要求输出 JSON，再由框架反序列化为 Cv 对象，
 * 以此获得类型安全、方便程序处理的输出。
 *
 * <p>编排流程（和 1a 几乎一致）：
 * 1. 定义模型
 * 2. 定义 Agent 行为（CvGeneratorStructuredOutput 接口，返回 Cv 对象）
 * 3. 用 agentBuilder 构建 Agent
 * 4. 加载人生故事文本
 * 5. 调用得到 Cv 对象并打印
 */
public class _1b_Basic_Agent_Example_Structured {

    // 设置日志级别：控制模型调用的日志输出详细程度
    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);
    }

    // 1. 定义驱动该 Agent 的底层模型
    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 2. 定义 Agent 行为 —— 见 CvGeneratorStructuredOutput.java 接口（返回 Cv 对象）

        // 3. 用 AgenticServices 创建/构建 Agent
        CvGeneratorStructuredOutput cvGeneratorStructuredOutput = AgenticServices
                .agentBuilder(CvGeneratorStructuredOutput.class) // 指定 Agent 接口类型
                .chatModel(CHAT_MODEL)                          // 指定驱动模型
                .build();                                       // 构建完成

        // 4. 从 resources/documents/user_life_story.txt 加载人生故事文本
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");

        // 5. 从 Agent 中获取一个 Cv 对象（框架自动把 LLM 输出的 JSON 反序列化为 Cv）
        Cv cvStructured = cvGeneratorStructuredOutput.generateCv(lifeStory);

        // 打印结构化的 Cv 对象（调用其 toString()）
        System.out.println("\n\n=== 简历对象 CV OBJECT ===");
        System.out.println(cvStructured);
    }
}