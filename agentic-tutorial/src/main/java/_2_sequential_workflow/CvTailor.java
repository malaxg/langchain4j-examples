package _2_sequential_workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent 接口（实体接口）：简历定制(Tailor) Agent。
 *
 * <p>它接收“主简历(master CV)”和“定制指令(instructions，如职位描述/反馈)”，
 * 输出一份针对指令量身定制的简历。这是“顺序工作流(sequential workflow)”中的第 2 步，
 * 上游是 _1_basic_agent 包里的 CvGenerator（第 1 步生成主简历）。
 *
 * <p>本接口同时演示了 @SystemMessage（系统提示词）与 @UserMessage（用户提示词）的用法：
 * - @SystemMessage：设定 Agent 的系统角色/行为准则，模板中的 {{masterCv}} 注入主简历；
 * - @UserMessage：提供本次调用的具体指令，模板中的 {{instructions}} 注入定制指令。
 */
public interface CvTailor {

    /**
     * 按指定的指令定制/修改简历。
     *
     * @Agent 声明这是一个 Agent，description 说明其职责（供框架/LLM 理解）。
     * @SystemMessage 定义系统提示词（含 {{masterCv}} 占位符，注入主简历）。
     * @UserMessage 定义用户提示词（含 {{instructions}} 占位符，注入定制指令）。
     * 两个占位符分别由方法参数的 @V("masterCv") 和 @V("instructions") 绑定注入。
     *
     * @param masterCv 主简历文本
     * @param instructions 定制指令（职位描述/反馈等）
     * @return 定制后的简历文本
     */
    @Agent("根据具体指令定制一份简历")
    @SystemMessage("""
                这里是一份需要根据具体职位描述、反馈或其他指令来定制(量身定制)的简历。
                你可以为了满足要求而让简历看起来更好，但不要虚构事实。
                如果删除无关内容能让简历更符合指令要求，你可以删掉它们。
                目标是让求职者获得面试机会，并且简历内容经得起面试验证。不要写得太长。
                主简历：{{masterCv}}
                """)
    @UserMessage("""
                以下是定制这份简历的指令：{{instructions}}
                """)
    String tailorCv(@V("masterCv") String masterCv, @V("instructions") String instructions);
}
