package dev.langchain4j.example.utils;

import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.output.JsonSchemas;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * "裁判模型"断言工具。
 *
 * <p>思路：用一个额外的、更强的大模型（judgeModel）去评估被测 Agent 的回复，
 * 看回复是否满足测试者写下的若干"条件（condition）"。</p>
 *
 * <p>例如条件可以是 "does not mention any dates"（不提及任何日期）。
 * 裁判模型会被要求对每个条件给出"满足 / 不满足 / 不确定"的评估结果。</p>
 */
public class JudgeModelAssertions {

    /**
     * 单个条件的评估结果。
     */
    private enum ConditionAssessmentResult {

        // 满足 / 不满足 / 不确定
        SATISFIED, NOT_SATISFIED, NOT_SURE
    }

    /**
     * 单个条件的评估记录：条件序号 + 推理过程 + 评估结果。
     */
    private record ConditionAssessment(
            int conditionIndex,       // 条件序号（对应传入条件的下标）
            String reasoning,         // 裁判模型的推理说明
            ConditionAssessmentResult result) {  // 评估结果
    }

    /**
     * 一次对话中所有条件的评估结果集合（用于 JSON 反序列化）。
     */
    private record ConditionAssessments(List<ConditionAssessment> conditionAssessments) {
    }

    // JSON 解析器，用于把裁判模型的输出解析成对象
    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    // 要求裁判模型以 JSON 格式返回，并预先定义好 JSON Schema（对应 ConditionAssessments）
    private static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
            .type(JSON)
            .jsonSchema(JsonSchemas.jsonSchemaFrom(ConditionAssessments.class).get())
            .build();

    /**
     * 静态入口：传入裁判模型，开始构造断言。
     *
     * @param judgeModel 裁判大模型
     * @return 断言构造器
     */
    public static ModelAssertion with(ChatModel judgeModel) {
        return new ModelAssertion(judgeModel);
    }

    /**
     * 持有裁判模型的断言构造器。
     */
    public static class ModelAssertion {

        private final ChatModel judgeModel;

        /**
         * 构造器：校验并保存裁判模型。
         *
         * @param judgeModel 裁判大模型
         */
        ModelAssertion(ChatModel judgeModel) {
            this.judgeModel = ensureNotNull(judgeModel, "judgeModel");
        }

        /**
         * 对指定文本发起断言。
         *
         * @param text 被测的 Agent 回复文本
         * @return 文本断言对象
         */
        public TextAssertion assertThat(String text) {
            return new TextAssertion(judgeModel, text);
        }
    }

    /**
     * 对一段文本进行条件评估的断言对象。
     */
    public static class TextAssertion {

        private final ChatModel judgeModel;
        private final String text;

        /**
         * 构造器：保存裁判模型与被测文本。
         *
         * @param judgeModel 裁判大模型
         * @param text       被测的 Agent 回复文本
         */
        TextAssertion(ChatModel judgeModel, String text) {
            this.judgeModel = ensureNotNull(judgeModel, "judgeModel");
            this.text = ensureNotNull(text, "text");
        }

        /**
         * 断言：被测文本满足给定的全部条件（可变参数版本）。
         *
         * @param conditions 一个或多个评估条件
         * @return 当前断言对象（支持链式调用）
         */
        public TextAssertion satisfies(String... conditions) {
            return satisfies(asList(conditions));
        }

        /**
         * 断言：被测文本满足给定的全部条件（集合版本）。
         *
         * @param conditions 评估条件列表
         * @return 当前断言对象（支持链式调用）
         */
        public TextAssertion satisfies(List<String> conditions) {

            ensureNotEmpty(conditions, "conditions");

            // 把每个条件包装成 <condition0>...</condition0>，便于裁判模型逐条定位
            StringBuilder conditionsFormatted = new StringBuilder();
            int i = 0;
            for (String condition : conditions) {
                conditionsFormatted.append("<condition%s>%s</condition%s>".formatted(i, condition, i++));
                conditionsFormatted.append("\n");
            }

            // 构造发送给裁判模型的请求：系统提示 + 被测文本
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(
                            SystemMessage.from("""
                                    请判断下面的文本是否满足以下条件？
                                    %s
                                    请针对每个条件给出：条件序号、推理过程和评估结果。
                                    """.formatted(conditionsFormatted)
                            ),
                            UserMessage.from("<text>%s</text>".formatted(text))
                    )
                    .parameters(ChatRequestParameters.builder()
                            .responseFormat(RESPONSE_FORMAT)  // 要求 JSON 格式输出
                            .build())
                    .build();

            // 调用裁判模型
            ChatResponse chatResponse = judgeModel.chat(chatRequest);

            // 解析 JSON 输出
            String json = chatResponse.aiMessage().text();
            try {
                ConditionAssessments conditionAssessments = JSON_MAPPER.readValue(json, ConditionAssessments.class);

                // 汇总所有"未满足"的条件
                List<String> failures = new ArrayList<>();

                for (ConditionAssessment assessment : conditionAssessments.conditionAssessments) {
                    if (assessment.result != ConditionAssessmentResult.SATISFIED) {
                        failures.add("""
                                条件 %s: %s
                                推理过程: %s
                                """.formatted(
                                assessment.conditionIndex, conditions.get(assessment.conditionIndex),
                                assessment.reasoning
                        ));
                    }
                }

                // 只要有条件不满足，就断言失败并打印详细信息
                if (!failures.isEmpty()) {
                    fail("文本 '%s' 有下列条件未满足：\n\n%s"
                            .formatted(text, String.join("\n", failures)));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return this;
        }
    }
}
