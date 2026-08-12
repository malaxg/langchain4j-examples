package _1_basic_agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent 接口（实体接口）：最简单的简历生成 Agent（基础版）。
 *
 * <p>它把用户的“人生/职业经历叙述”转换为一段自由文本格式的完整简历。
 *
 * <p>这就是最基础的 Agent 写法：
 * - 用 interface 声明一个“智能体”；
 * - @Agent 注解标记这是一个 Agent 类型接口；
 * - @UserMessage 定义发给 LLM 的提示词模板；
 * - @V("lifeStory") 把方法参数绑定到模板占位符。
 *
 * <p>注意：单独的单个 Agent 用途有限（用 AiService 也能实现），
 * Agent 的真正价值在于后续教程中把它和其他 Agent 组合成各种工作流。
 */
public interface CvGenerator {

    /**
     * 生成简历的方法。
     *
     * @UserMessage：发给 LLM 的用户提示词模板。
     *   {{lifeStory}} 是占位符，运行时会替换成 @V("lifeStory") 传入的真实内容。
     * @Agent：声明该接口是一个 Agent，description 描述其职责，便于 LLM/框架理解用途。
     *
     * @param userInfo 用户的人生/职业轨迹叙述文本（注入到 {{lifeStory}}）。
     * @return 生成的简历，纯文本 String。
     */
    @UserMessage("""
            这里是我的人生与职业轨迹信息，
            请你把它整理成一份清晰完整的简历(Resume/CV)。
            不要虚构事实，也不要遗漏任何技能或经历。
            这份简历稍后还会被进一步打磨，现阶段请确保内容完整。
            只返回简历本身，不要附带其他任何文字。
            我的经历如下：{{lifeStory}}
            """)
    @Agent("根据用户提供的信息生成一份干净的简历")
    String generateCv(@V("lifeStory") String userInfo);
}
