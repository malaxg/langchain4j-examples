package _2_sequential_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.V;

import java.util.Map;

/**
 * Agent 接口（实体接口）：【类型化(Typed)的顺序组合 Agent 接口】。
 *
 * <p>与 2a 使用 UntypedAgent（无类型）不同，本接口是“用户自定义的类型化组合接口”：
 * 把顺序工作流封装成带签名的方法，从而可以像调用普通方法一样使用，
 * 并且借助编译期类型检查更安全、更优雅。
 *
 * <p>方法返回类型 ResultWithAgenticScope<...> 表示“结果 + AgenticScope（作用域）”：
 * - result()：自定义输出的结果；
 * - agenticScope()：包含工作流中所有输入/中间/输出变量的作用域，可用于调试或测试。
 */
public interface SequenceCvGenerator {

    /**
     * 顺序执行“生成主简历 → 定制简历”这两个子 Agent 的组合方法。
     *
     * @Agent 声明这是组合 Agent 接口，description 说明其能力描述。
     * @V("lifeStory") 和 @V("instructions") 是该方法两个参数，
     * 会分别送入第一个子 Agent(CvGenerator)和第二个子 Agent(CvTailor)的输入变量。
     *
     * @param lifeStory 用户人生经历（第 1 个 Agent 的输入）
     * @param instructions 定制指令（第 2 个 Agent 的输入）
     * @return 包含“结果(自定义输出 + 各中间变量)”和“AgenticScope”的封装对象
     */
    @Agent("根据用户提供的信息生成简历并按指令定制,不要太长,避免空行")
    ResultWithAgenticScope<Map<String, String>> generateTailoredCv(@V("lifeStory") String lifeStory, @V("instructions") String instructions);
}
