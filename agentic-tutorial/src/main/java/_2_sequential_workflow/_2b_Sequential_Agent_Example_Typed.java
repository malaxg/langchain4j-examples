package _2_sequential_workflow;

import _1_basic_agent.CvGenerator;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import util.AgenticScopePrinter;
import util.ChatModelProvider;
import util.StringLoader;
import util.log.CustomLogging;
import util.log.LogLevels;

import java.io.IOException;
import java.util.Map;

/**
 * 示例主类（2b）：演示【类型化(Typed)的顺序工作流 + 自定义输出 + 查看 AgenticScope】。
 *
 * <p>与 2a 的区别：实现同样的顺序工作流，但这次：
 * - 使用自定义类型化组合接口（SequenceCvGenerator），
 *   于是可以像普通方法一样携带参数调用（而无需用 Map 调 .invoke(argsMap)）；
 * - 用 .output(...) 自定义收集输出的方式（这里收集 3 个变量进一个 Map）；
 * - 调用后可取出并检查 AgenticScope（作用域），用于调试或测试。
 *
 * <p>核心编排概念：
 * - sequenceBuilder(SequenceCvGenerator.class)：绑定类型化接口；
 * - .subAgents(...)：加入子 Agent；
 * - .output(agenticScope -> ...)：自定义“输出适配器(handler)”，从作用域读取多个变量并打包成结果；
 * - agenticScope.readState(key)：从作用域读取某个（输入/中间/输出）变量；
 * - ResultWithAgenticScope：同时携带结果(result)和作用域(agenticScope)的返回封装。
 */
public class _2b_Sequential_Agent_Example_Typed {

    // 设置日志级别
    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 150);
    }

    /**
     * 我们将实现与 2a 相同的顺序工作流，但这次：
     * - 为组合 Agent 使用类型化接口（SequenceCvGenerator），
     *   从而能用带参数的方法调用，而不是 .invoke(argsMap)；
     * - 自定义输出的收集方式；
     * - 调用后取出并检查 AgenticScope（作用域），便于调试或测试。
     */

    // 1. 定义驱动所有 Agent 的底层模型
    private static final ChatModel CHAT_MODEL = ChatModelProvider.createChatModel();

    public static void main(String[] args) throws IOException {

        // 2. 在本包中定义顺序 Agent 接口：
        //      - SequenceCvGenerator.java
        // 其方法签名为：
        // ResultWithAgenticScope<Map<String, String>> generateTailoredCv(@V("lifeStory") String lifeStory, @V("instructions") String instructions);

        // 3. 和之前一样，用 AgenticServices 分别构建两个子 Agent
        CvGenerator cvGenerator = AgenticServices
                .agentBuilder(CvGenerator.class)
                .chatModel(CHAT_MODEL)
                .outputKey("masterCv") // 若想把该变量从 Agent1 传给 Agent2，
                                       // 必须让这里的输出键名匹配第二个 Agent 接口
                                       // agent_interfaces/CvTailor.java 中的输入变量名
                .build();
        CvTailor cvTailor = AgenticServices
                .agentBuilder(CvTailor.class)
                .chatModel(CHAT_MODEL)             // 注意：不同 Agent 也可使用不同模型
                .outputKey("tailoredCv")           // 需要定义输出对象的键名
                                                   // 若写成 "masterCv" 会覆盖原主简历；
                                                   // 此处不想覆盖，但这本身是个有用的功能
                .build();

        // 4. 从 resources/documents/ 的文本文件加载参数（这次无需放进 Map）
        // - user_life_story.txt
        // - job_description_backend.txt
        String lifeStory = StringLoader.loadFromResource("/documents/user_life_story.txt");
        String instructions = "把简历调整为适配下面的职位描述。" + StringLoader.loadFromResource("/documents/job_description_backend.txt");

        // 5. 构建带自定义输出处理的类型化顺序工作流
        SequenceCvGenerator sequenceCvGenerator = AgenticServices
                .sequenceBuilder(SequenceCvGenerator.class) // 指定类型化接口
                .subAgents(cvGenerator, cvTailor)           // 加入子 Agent，顺序即执行顺序
                .outputKey("bothCvsAndLifeStory")           // 自定义输出的键名（存于作用域）
                .output(agenticScope -> {                   // 自定义输出适配器：从作用域收集若干内部变量
                    Map<String, String> bothCvsAndLifeStory = Map.of(
                            "lifeStory", agenticScope.readState("lifeStory", ""),
                            "masterCv", agenticScope.readState("masterCv", ""),
                            "tailoredCv", agenticScope.readState("tailoredCv", "")
                    );
                    return bothCvsAndLifeStory;             // 返回打包后的结果
                    })
                .build();

        // 6. 调用类型化的组合 Agent（直接传带类型的参数，更安全优雅）
        ResultWithAgenticScope<Map<String,String>> bothCvsAndScope = sequenceCvGenerator.generateTailoredCv(lifeStory, instructions);

        // 7. 从返回结果中分别取出“结果”和“AgenticScope”
        AgenticScope agenticScope = bothCvsAndScope.agenticScope();      // 作用域：包含所有输入/中间/输出变量
        Map<String,String> bothCvsAndLifeStory = bothCvsAndScope.result(); // 结果：我们自定义打包的 Map

        // 打印三个变量（截断展示，避免刷屏）
        System.out.println("=== 用户信息 USER INFO（输入）===");
        String userStory = bothCvsAndLifeStory.get("lifeStory");
        System.out.println(userStory.length() > 100 ? userStory.substring(0, 100) + " [截断...]" : lifeStory);
        System.out.println("=== 主简历 MASTER CV（中间变量）===");
        String masterCv = bothCvsAndLifeStory.get("masterCv");
        System.out.println(masterCv.length() > 100 ? masterCv.substring(0, 100) + " [截断...]" : masterCv);
        System.out.println("=== 定制简历 TAILORED CV（输出）===");
        String tailoredCv = bothCvsAndLifeStory.get("tailoredCv");
        System.out.println(tailoredCv.length() > 100 ? tailoredCv.substring(0, 100) + " [截断...]" : tailoredCv);

        // 无类型(2a)和类型化(2b)的 Agent 会得到相同的 tailoredCv 结果
        // （任何差异都源自 LLM 的非确定性），
        // 但类型化版本使用起来更优雅，且因编译期类型检查而更安全

        // 打印 AgenticScope（作用域）的内容，展示其结构
        System.out.println("=== 作用域 AGENTIC SCOPE ===");
        System.out.println(AgenticScopePrinter.printPretty(agenticScope, 100));
        // 这会打印出（已填充）如下对象：
        // AgenticScope {
        //     memoryId = "e705028d-e90e-47df-9709-95953e84878c",
        //             state = {
        //                     bothCvsAndLifeStory = { // 输出
        //                             masterCv = "...",
        //                            lifeStory = "...",
        //                            tailoredCv = "..."
        //                     },
        //                     instructions = "...", // 输入与中间变量
        //                     tailoredCv = "...",
        //                     masterCv = "...",
        //                     lifeStory = "..."
        //             }
        // }
        System.out.println("=== 作为对话的作用域内容 CONTEXT（会话中所有消息）===");
        System.out.println(AgenticScopePrinter.printConversation(agenticScope.contextAsConversation(), 100));

    }
}
