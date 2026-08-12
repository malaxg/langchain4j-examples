package dev.langchain4j.example.utils;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecution;
import org.assertj.core.api.AbstractAssert;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.util.IterableUtil.isNullOrEmpty;

/**
 * 为 {@link Result} 类定制的 AssertJ 断言工具。
 *
 * <p>这些断言让测试可以直接检查 Agent 的执行过程与结果，例如：
 * 是否执行了某个工具、是否没有执行工具、是否检索到了知识库中的内容等。</p>
 */
public class ResultAssert extends AbstractAssert<ResultAssert, Result<?>> {

    /**
     * 构造断言对象。
     *
     * @param actual 实际的 {@link Result} 对象
     */
    public ResultAssert(Result<?> actual) {
        super(actual, ResultAssert.class);
    }

    /**
     * 静态入口方法，用于启动断言链。
     *
     * @param actual 实际的 {@link Result} 对象
     * @return 断言对象
     */
    public static ResultAssert assertThat(Result<?> actual) {
        return new ResultAssert(actual);
    }

    /**
     * 断言：只执行了指定的某一个工具。
     *
     * @param toolName 期望执行的工具名
     * @return 当前断言对象（支持链式调用）
     */
    public ResultAssert onlyToolWasExecuted(String toolName) {

        isNotNull();

        List<ToolExecution> toolExecutions = actual.toolExecutions();
        // 一个工具都没执行 → 断言失败
        if (isNullOrEmpty(toolExecutions)) {
            failWithMessage("期望执行工具 <%s>，但实际一个工具都没有执行");
        }

        // 收集所有已执行工具的名字
        Set<String> executedToolNames = toolExecutions.stream()
                .map(toolExecution -> toolExecution.request().name())
                .collect(toSet());

        // 期望的工具没有被执行 → 断言失败
        if (!executedToolNames.contains(toolName)) {
            failWithMessage("期望执行工具 <%s>，但实际执行的是其他工具：<%s>",
                    toolName, executedToolNames);
        }

        // 除了期望的工具外还执行了其他工具 → 断言失败
        if (executedToolNames.size() > 1) {
            failWithMessage("期望只执行工具 <%s>，但还额外执行了其他工具：<%s>",
                    toolName, executedToolNames);
        }

        return this;
    }

    /**
     * 断言：没有执行任何工具。
     *
     * @return 当前断言对象（支持链式调用）
     */
    public ResultAssert noToolsWereExecuted() {

        isNotNull();

        List<ToolExecution> toolExecutions = actual.toolExecutions();
        // 只要有工具被执行 → 断言失败
        if (!isNullOrEmpty(toolExecutions)) {
            failWithMessage("期望不执行任何工具，但实际执行了：<%s>", toolExecutions);
        }

        return this;
    }

    /**
     * 断言：RAG 检索到的来源中应包含指定文本。
     *
     * @param text 期望在检索来源中出现的文本
     * @return 当前断言对象（支持链式调用）
     */
    public ResultAssert retrievedSourcesContain(String text) {

        isNotNull();

        List<Content> sources = actual.sources();
        // 没有检索到任何来源 → 断言失败
        if (isNullOrEmpty(sources)) {
            failWithMessage("期望能检索到来源，但实际没有检索到任何来源");
        }

        // 提取所有检索来源的文本
        List<String> sourceTexts = sources.stream()
                .map(source -> source.textSegment().text())
                .toList();

        // 若没有任何来源包含指定文本 → 断言失败，并把实际来源打印出来方便排查
        if (sourceTexts.stream().noneMatch(sourceText -> sourceText.contains(text))) {
            failWithMessage("期望在检索来源中找到文本 <%s>，但实际内容如下：\n%s",
                    text,
                    sourceTexts.stream().collect(joining("\n- ", "- ", "")));
        }

        return this;
    }
}
