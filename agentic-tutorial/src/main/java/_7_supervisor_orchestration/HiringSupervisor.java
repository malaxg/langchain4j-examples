package _7_supervisor_orchestration;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.agentic.supervisor.AgentInvocation;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * Agent 接口：招聘监督者（类型化 Supervisor 的接口定义）。
 * 这是一个"监督者（Supervisor）" Agent 的入口接口：由 LLM 动态决定接下来调用哪个子 Agent、
 * 调用几次、以什么顺序调用，实现"自组织"的编排（区别于写死的确定性工作流）。
 * 返回 ResultWithAgenticScope，既拿到最终结果（String），又能拿到执行过程中不断演化的 AgenticScope 上下文。
 */
public interface HiringSupervisor {
    @Agent("负责协调候选人评估与决策的顶层招聘监督者")
    ResultWithAgenticScope<String> invoke(@V("request") String request, @V("supervisorContext") String supervisorContext);
}
