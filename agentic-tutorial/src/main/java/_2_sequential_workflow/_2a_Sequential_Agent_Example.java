package _2_sequential_workflow;

import _1_basic_agent.CvGenerator;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import util.ChatModelProvider;
import util.StringLoader;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.IOException;
import java.util.Map;

/**
 * 示例主类（2a）：演示【顺序工作流(Sequential workflow)】。
 *
 * <p>前面的 1a/1b 只有单个 Agent；这里我们构建两个 Agent：
 * - CvGenerator（读取人生故事，生成完整的主简历 master CV）；
 * - CvTailor（读取主简历，根据指令如职位描述/反馈进行定制）。
 * 然后用 sequenceBuilder 按【固定顺序】依次调用它们，并演示如何在两个 Agent 之间传参。
 *
 * <p>核心编排概念：
 * - AgenticServices.sequenceBuilder()：顺序工作流构建器；
 * - .subAgents(a, b)：加入子 Agent，顺序很重要（先 a 后 b）；
 * - .outputKey("tailoredCv")：定义组合 Agent 的最终输出是作用域中的哪个变量；
 * - AgenticScope：所有输入、中间变量、输出以及调用链都会被存储在其中，供高级用途使用。
 *
 * <p>与 2b 的区别：这里用 UntypedAgent（无类型），调用时需传 Map<String,Object> 参数；
 * 2b 则会用自定义类型化接口，调用更安全优雅。
 */
public class _2a_Sequential_Agent_Example {

    // 设置日志级别
    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);
    }

    // 1. 定义驱动所有 Agent 的底层模型
    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 2. 在本包中定义两个子 Agent 接口：
        //      - CvGenerator.java（生成长简历，来自 _1_basic_agent 包）
        //      - CvTailor.java（定制简历）

        // 3. 用 AgenticServices 分别构建两个 Agent
        CvGenerator cvGenerator = AgenticServices
                .agentBuilder(CvGenerator.class)   // 指定 Agent 接口类型
                .chatModel(CHAT_MODEL)             // 指定驱动模型
                .outputKey("masterCv")             // 输出键名：若要把该变量从 Agent1 传给 Agent2，
                                                   // 必须让这里的 outputKey 与第二个 Agent 接口
                                                   // CvTailor.java 中输入变量的名称一致
                .build();
        CvTailor cvTailor = AgenticServices
                .agentBuilder(CvTailor.class)
                .chatModel(CHAT_MODEL)             // 注意：也可以给不同 Agent 使用不同模型
                .outputKey("tailoredCv")           // 需要定义输出对象对应的键名
                                                   // 如果这里写成 "masterCv"，
                                                   // 就会把原来的主简历覆盖掉；此处我们不想覆盖，
                                                   // 但这是一个很有用的功能（用于中间变量覆写）
                .build();

        ////////////////// 无类型(UNTYPED)示例 //////////////////////

        // 4. 构建顺序工作流
        UntypedAgent tailoredCvGenerator = AgenticServices // 除非你自定义类型化接口（见 2b），否则使用 UntypedAgent
                .sequenceBuilder()                        // 顺序工作流构建器
                .subAgents(cvGenerator, cvTailor)          // 加入子 Agent，可任意多个，顺序很重要
                .outputKey("tailoredCv")                   // 组合 Agent 的最终输出变量
                                                           // 注意：输出可以是 AgenticScope 中的任意字段，
                                                           // 例如也可以输出 'masterCv'（尽管这里没意义）
                .build();

        // 4. 从 resources/documents/ 文本文件加载参数
        // - user_life_story.txt
        // - job_description_backend.txt
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");
        String instructions = "把简历调整为适配下面的职位描述。" + StringLoader.loadFromResource("/documents/job_description_backend.txt");

        // 5. 因为是无类型 Agent，需用 Map 传参（键必须与 Agent 接口中的变量名一致）
        Map<String, Object> arguments = Map.of(
                "lifeStory", lifeStory,       // 键名与 agent_interfaces/CvGenerator.java 中的变量名一致
                "instructions", instructions  // 键名与 agent_interfaces/CvTailor.java 中的变量名一致
        );

        // 5. 调用组合 Agent 生成定制简历
        String tailoredCv = (String) tailoredCvGenerator.invoke(arguments);

        // 6. 打印生成的定制简历
        System.out.println("=== 无类型定制简历 TAILORED CV UNTYPED ===");
        System.out.println((String) tailoredCv); // 可以观察到：若输入改为 job_description_fullstack.txt，
                                                 // 生成的简历会非常不同

        // 在例子 2b 中，我们将构建同样的顺序 Agent 但使用类型化输出，
        // 并查看 AgenticScope 中的内容
    }
}
