package _1_basic_agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import domain.Cv;

/**
 * Agent 接口（实体接口）：带结构化输出的简历生成 Agent。
 *
 * <p>与 {@link CvGenerator}（返回 String）的区别在于：
 * 本接口的 {@code generateCv} 方法返回的是一个自定义 Java 对象 {@link Cv}，
 * 因此 LangChain4j 会要求 LLM 输出符合 {@code Cv} 类结构的 JSON，
 * 再反序列化成语义化、类型安全的 Java 对象，而不是一段自由文本。
 *
 * <p>本场景里 LLM 扮演“简历生成器”这个 Agent（智能体），
 * 输入一段人生/职业经历的叙述（life story），输出结构化简历对象。
 */
public interface CvGeneratorStructuredOutput {

    /**
     * 生成结构化 CV 的方法。
     *
     * @UserMessage 定义发送给 LLM 的用户消息（提示词模板）。
     *   模板中的 {{lifeStory}} 是占位符，运行时会被 @V("lifeStory") 注入的真实文本替换。
     * @V("lifeStory") 把方法的 String 参数绑定到模板占位符 lifeStory。
     * @Agent 声明这是一个 Agent（智能体）类型接口，并给出对 LLM 的职责描述（工具/意图描述）。
     *
     * @param userInfo 用户的“人生/职业轨迹”叙述文本，会注入到 {{lifeStory}}。
     * @return 结构化的 Cv 对象（由 LLM 输出的 JSON 反序列化而来）。
     */
    @UserMessage("""
            这里是我的人生与职业轨迹信息，
            请你把它整理成一份清晰完整的简历(Resume/CV)。
            不要虚构事实，也不要遗漏任何技能或经历。
            这份简历稍后还会被进一步打磨，现阶段请确保内容完整。
            我的经历如下：{{lifeStory}}
            """)
    @Agent("根据用户提供的信息生成一份干净的简历")
    Cv generateCv(@V("lifeStory") String userInfo);
}
