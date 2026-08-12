import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

/**
 * 教程第 3 课：提示词模板（Prompt Template）。
 * <p>
 * 提示词模板 = "带有占位符的字符串" + "变量值"，两者结合后生成真正的提示词。
 * 这样同一个模板可以复用：只需要替换不同的变量值，就能批量生成大量内容各异的提示词，
 * 而不必每次手动拼接字符串。本课演示两种方式：
 * 1. Simple_Prompt_Template_Example：用简单的 {{变量名}} 占位符；
 * 2. Structured_Prompt_Template_Example：用 @StructuredPrompt 注解定义结构化的多行提示词。
 */
public class _03_PromptTemplate {

    /**
     * 简单提示词模板示例：用 {@code {{dishType}}}、{@code {{ingredients}}} 占位符，
     * 把"菜品种类"和"食材"动态填入模板，生成一份菜谱请求。
     */
    static class Simple_Prompt_Template_Example {

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 构建聊天模型（打开日志开关便于观察）

            // 1. 定义提示词模板：{{dishType}} 和 {{ingredients}} 是待填充的占位符
            String template = "请为{{dishType}}创建一个菜谱，需要用到以下食材：{{ingredients}}";
            PromptTemplate promptTemplate = PromptTemplate.from(template);   // 把字符串解析成模板对象

            // 2. 准备占位符对应的变量值（key 必须和模板里的占位符名一致）
            Map<String, Object> variables = new HashMap<>();
            variables.put("dishType", "烤箱菜");                // 填入 {{dishType}}
            variables.put("ingredients", "土豆、番茄、羊乳酪、橄榄油"); // 填入 {{ingredients}}

            // 3. 用变量值"渲染"模板：把占位符替换成实际内容，得到最终的提示词
            Prompt prompt = promptTemplate.apply(variables);

            // 4. 把渲染好的提示词文本发给模型，prompt.text() 取出纯文本内容
            String response = Model.MODEL.chat(prompt.text());

            // 5. 打印模型生成的菜谱
            System.out.println(response);
        }

    }

    /**
     * 结构化提示词模板示例：用 @StructuredPrompt 注解直接在一个类上声明多行提示词。
     * <p>
     * 模板里的占位符 {{dish}}、{{ingredients}} 会自动对应 CreateRecipePrompt 类的
     * 同名成员变量（dish、ingredients），这比手动维护 Map 更清晰、不易出错。
     */
    static class Structured_Prompt_Template_Example {

        /**
         * 用注解声明结构化提示词模板。
         * 每行字符串会被拼接成一段完整提示词，{{dish}} 和 {{ingredients}} 是占位符，
         * 会由处理器自动用同名成员变量的值填充。
         */
        @StructuredPrompt({
                "创建一个只用{{ingredients}}就能做出的{{dish}}菜谱。",
                "请按照下面的格式来组织你的回答：",

                "菜名：...",
                "描述：...",
                "准备时间：...",

                "所需食材：",
                "- ...",
                "- ...",

                "制作步骤：",
                "- ...",
                "- ..."
        })
        static class CreateRecipePrompt {

            String dish;                 // 占位符 {{dish}} 对应的变量：菜品种类
            List<String> ingredients;    // 占位符 {{ingredients}} 对应的变量：食材列表

            /**
             * 构造方法：一次性传入菜品种类和食材列表。
             *
             * @param dish        菜品种类
             * @param ingredients 食材列表
             */
            CreateRecipePrompt(String dish, List<String> ingredients) {
                this.dish = dish;
                this.ingredients = ingredients;
            }
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 构建聊天模型
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    .timeout(ofSeconds(60))
                    .build();

            // 1. 创建携带模板变量的对象：菜品是"沙拉"，食材是黄瓜、番茄、羊乳酪、洋葱、橄榄
            Structured_Prompt_Template_Example.CreateRecipePrompt createRecipePrompt = new Structured_Prompt_Template_Example.CreateRecipePrompt(
                    "沙拉",
                    asList("黄瓜", "番茄", "羊乳酪", "洋葱", "橄榄")
            );

            // 2. 让结构化提示词处理器把 CreateRecipePrompt 对象转换成最终提示词
            //    （读取 @StructuredPrompt 注解的模板，并用对象的字段值填充占位符）
            Prompt prompt = StructuredPromptProcessor.toPrompt(createRecipePrompt);

            // 3. 把提示词发给模型生成菜谱
            String recipe = model.chat(prompt.text());

            // 4. 打印生成的菜谱
            System.out.println(recipe);
        }
    }
}
