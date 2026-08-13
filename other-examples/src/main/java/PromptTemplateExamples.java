import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 演示如何使用提示词模板（PromptTemplate）动态生成提示词。
 * <p>
 * 模板通过 {@code {{变量}}} 占位符留出可替换位置，
 * 再调用 apply(...) 填入实际值生成最终的 {@link Prompt}。
 * 包含单个变量与多个变量两种用法。
 */
public class PromptTemplateExamples {

    /**
     * 示例：使用只含一个变量 {{it}} 的模板。
     * <p>
     * 当模板里只有一个变量时，可直接把单个值传给 apply()。
     */
    static class PromptTemplate_with_One_Variable_Example {

        public static void main(String[] args) {

            // 定义模板：{{it}} 是占位符，表示"它"
            PromptTemplate promptTemplate = PromptTemplate.from("用{{it}}说“你好”。");

            // 把 "德语" 填入 {{it}} 生成最终的 Prompt
            Prompt prompt = promptTemplate.apply("德语");

            System.out.println(prompt.text()); // 用德语说“你好”。
        }
    }

    /**
     * 示例：使用含多个变量 {{text}} 和 {{language}} 的模板。
     * <p>
     * 多变量时需要提供一个 Map，key 是变量名、value 是要填入的值。
     */
    static class PromptTemplate_With_Multiple_Variables_Example {

        public static void main(String[] args) {

            // 定义含两个占位符的模板
            PromptTemplate promptTemplate = PromptTemplate.from("用{{language}}说“{{text}}”。");

            // 用 Map 提供所有变量的值（key = 变量名，value = 值）
            Map<String, Object> variables = new HashMap<>();
            variables.put("text", "你好");        // {{text}} = "你好"
            variables.put("language", "德语");    // {{language}} = "德语"

            Prompt prompt = promptTemplate.apply(variables);

            System.out.println(prompt.text()); // 用德语说“你好”。
        }
    }
}
