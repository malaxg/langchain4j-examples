import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

/**
 * 演示"结构化提示词"（StructuredPrompt）的用法。
 * <p>
 * 通过 {@code @StructuredPrompt} 注解一个普通 POJO 类，并用模板定义提示词，
 * 再借助 {@link StructuredPromptProcessor} 把该对象自动转换成最终发出的 {@link Prompt}。
 * 包含"单行模板"与"多行模板"两个示例。
 */
public class StructuredPromptTemplateExamples {

    // 供示例共享的 OpenAI 聊天模型
    static ChatModel model = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .timeout(ofSeconds(60)) // 设置 60 秒请求超时
            .build();

    /**
     * 示例：使用单行 {@code @StructuredPrompt} 模板。
     * <p>
     * 通过给 POJO 的属性赋值，自动填充模板中的变量来生成提示词。
     */
    static class Simple_Structured_Prompt_Example {

        // @StructuredPrompt 定义模板：{{dish}}、{{ingredients}} 会由该对象的同名属性填充
        @StructuredPrompt("Create a recipe of a {{dish}} that can be prepared using only {{ingredients}}")
        static class CreateRecipePrompt {

            private String dish;              // 菜名，对应 {{dish}}
            private List<String> ingredients; // 配料列表，对应 {{ingredients}}
        }

        public static void main(String[] args) {

            // 构造提示词对象并填入变量值
            CreateRecipePrompt createRecipePrompt = new CreateRecipePrompt();
            createRecipePrompt.dish = "salad";
            createRecipePrompt.ingredients = asList("cucumber", "tomato", "feta", "onion", "olives");

            // 用 StructuredPromptProcessor 把对象转换成最终的 Prompt
            Prompt prompt = StructuredPromptProcessor.toPrompt(createRecipePrompt);

            // 把 Prompt 转成用户消息发给模型，取回答
            AiMessage aiMessage = model.chat(prompt.toUserMessage()).aiMessage();
            System.out.println(aiMessage.text());
        }
    }

    /**
     * 示例：使用多行（数组）形式的 {@code @StructuredPrompt} 模板。
     * <p>
     * 多行字符串会被组合成一个结构化的提示词，
     * 用于要求模型按指定格式（如菜名、描述、准备时间、配料、步骤）输出。
     */
    static class Multi_Line_Structured_Prompt_Example {

        // 多行模板：要求模型按照给定的固定结构来组织回答
        @StructuredPrompt({
                "Create a recipe of a {{dish}} that can be prepared using only {{ingredients}}.",
                "Structure your answer in the following way:",

                "Recipe name: ...",
                "Description: ...",
                "Preparation time: ...",

                "Required ingredients:",
                "- ...",
                "- ...",

                "Instructions:",
                "- ...",
                "- ..."
        })
        static class CreateRecipePrompt {

            private String dish;              // 菜名，对应 {{dish}}
            private List<String> ingredients; // 配料列表，对应 {{ingredients}}
        }

        public static void main(String[] args) {

            // 构造提示词对象并填入变量值
            CreateRecipePrompt createRecipePrompt = new CreateRecipePrompt();
            createRecipePrompt.dish = "salad";
            createRecipePrompt.ingredients = asList("cucumber", "tomato", "feta", "onion", "olives");

            // 把对象转换成最终的 Prompt 再发给模型
            Prompt prompt = StructuredPromptProcessor.toPrompt(createRecipePrompt);

            AiMessage aiMessage = model.chat(prompt.toUserMessage()).aiMessage();
            System.out.println(aiMessage.text());
        }
    }
}
