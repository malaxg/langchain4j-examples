import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.Arrays.asList;

/**
 * 一组"AI 服务"（AI Services）高级用法的综合示例集合。
 * <p>
 * 通过 {@link AiServices} 把接口方法自动映射为对 LLM 的调用，
 * 演示如何让模型返回枚举、数字、日期、POJO 等结构化结果，
 * 以及如何配置 {@code @SystemMessage} / {@code @UserMessage} /
 * {@code @UserName} 等注解。每个内部类都是一个独立可运行的示例。
 */
public class OtherServiceExamples {

    // 供各示例共享的 OpenAI 聊天模型实例
    static ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY) // 配置 API Key
            .modelName(GPT_4_O_MINI)        // 指定模型名称
            .build();

    /**
     * 示例：让 AI 服务返回枚举（情感分析）。
     * <p>
     * 演示 AI Services 自动把 LLM 的输出字符串转换为枚举类型，
     * 也演示了返回布尔值的用法。
     */
    static class Sentiment_Extracting_AI_Service_Example {

        // 情感类别枚举
        enum Sentiment {
            POSITIVE, NEUTRAL, NEGATIVE; // 正面、中性、负面
        }

        // 定义 AI 服务接口：方法签名决定了模型输出的类型/格式
        interface SentimentAnalyzer {

            // @UserMessage 指定发送给 LLM 的用户消息模板，{{it}} 代表方法入参 text
            @UserMessage("分析 {{it}} 的情感倾向")
            // 返回类型是枚举，LangChain4j 会让模型输出对应的枚举值
            Sentiment analyzeSentimentOf(String text);

            @UserMessage("{{it}} 是否带有正面情感？")
            // 返回类型是 boolean，模型只需回答 yes/no
            boolean isPositive(String text);
        }

        public static void main(String[] args) {

            // 使用 AiServices.create 根据接口自动生成代理对象
            SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, chatModel);

            // 接口方法返回枚举，模型会根据消息输出 POSITIVE / NEUTRAL / NEGATIVE
            Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf("这很好！");
            System.out.println(sentiment); // POSITIVE（正面）

            boolean positive = sentimentAnalyzer.isPositive("这很糟糕！");
            System.out.println(positive); // false（不是正面）
        }
    }


    /**
     * 示例：让 AI 服务从文本中提取数字，并转换为各种数值类型。
     * <p>
     * 同样的文本，只要把接口方法的返回类型改成 int / long / BigInteger /
     * float / double / BigDecimal，LangChain4j 就会自动把模型输出转换成对应类型。
     */
    static class Number_Extracting_AI_Service_Example {

        // 定义 AI 服务接口，方法返回类型决定数字被转换成的 Java 类型
        interface NumberExtractor {

            @UserMessage("从 {{it}} 中提取数字")
            int extractInt(String text); // 返回 int

            @UserMessage("从 {{it}} 中提取数字")
            long extractLong(String text); // 返回 long

            @UserMessage("从 {{it}} 中提取数字")
            BigInteger extractBigInteger(String text); // 返回 BigInteger

            @UserMessage("从 {{it}} 中提取数字")
            float extractFloat(String text); // 返回 float

            @UserMessage("从 {{it}} 中提取数字")
            double extractDouble(String text); // 返回 double

            @UserMessage("从 {{it}} 中提取数字")
            BigDecimal extractBigDecimal(String text); // 返回 BigDecimal
        }

        public static void main(String[] args) {

            NumberExtractor extractor = AiServices.create(NumberExtractor.class, chatModel);

            // 一段中文文本，其中隐藏着答案"42"
            String text = "超级计算机“深思”经过无数年的运算，终于宣布：关于生命、宇宙以及一切终极问题的答案是四十二。";

            int intNumber = extractor.extractInt(text);
            System.out.println(intNumber); // 42

            long longNumber = extractor.extractLong(text);
            System.out.println(longNumber); // 42

            BigInteger bigIntegerNumber = extractor.extractBigInteger(text);
            System.out.println(bigIntegerNumber); // 42

            float floatNumber = extractor.extractFloat(text);
            System.out.println(floatNumber); // 42.0

            double doubleNumber = extractor.extractDouble(text);
            System.out.println(doubleNumber); // 42.0

            BigDecimal bigDecimalNumber = extractor.extractBigDecimal(text);
            System.out.println(bigDecimalNumber); // 42.0
        }
    }


    /**
     * 示例：让 AI 服务从文本中提取日期和时间，并转换为 Java 的日期时间类型。
     * <p>
     * 根据返回类型 {@link LocalDate} / {@link LocalTime} / {@link LocalDateTime}，
     * LangChain4j 会生成对应的日期时间对象。
     */
    static class Date_and_Time_Extracting_AI_Service_Example {

        // 定义 AI 服务接口，方法返回类型决定提取的日期时间类型
        interface DateTimeExtractor {

            @UserMessage("从 {{it}} 中提取日期")
            LocalDate extractDateFrom(String text); // 只提取日期部分

            @UserMessage("从 {{it}} 中提取时间")
            LocalTime extractTimeFrom(String text); // 只提取时间部分

            @UserMessage("从 {{it}} 中提取日期和时间")
            LocalDateTime extractDateTimeFrom(String text); // 同时提取日期和时间
        }

        public static void main(String[] args) {

            DateTimeExtractor extractor = AiServices.create(DateTimeExtractor.class, chatModel);

            // 一段中文文本，里面隐含着日期 1968-07-04 和 23:45 这两个时间信息
            String text = "1968 年独立日庆典结束后的那个傍晚，四周一片宁静，离午夜只差 15 分钟。";

            LocalDate date = extractor.extractDateFrom(text);
            System.out.println(date); // 1968-07-04

            LocalTime time = extractor.extractTimeFrom(text);
            System.out.println(time); // 23:45

            LocalDateTime dateTime = extractor.extractDateTimeFrom(text);
            System.out.println(dateTime); // 1968-07-04T23:45
        }
    }


    /**
     * 示例：让 AI 服务把文本解析成一个自定义的 POJO 对象（Person）。
     * <p>
     * 结合"json_schema / strictJsonSchema"严格 JSON 模式，让模型强制输出合法 JSON，
     * 再由 LangChain4j 反序列化成 Java 对象，可显著提升结构化抽取的可靠性。
     */
    static class POJO_Extracting_AI_Service_Example {

        // 需要被填写的 POJO 类
        static class Person {

            // @Description 是可选的字段描述，帮助 LLM 更准确地理解每个字段的含义
            @Description("一个人的名字") // 此为发送给 LLM 的描述文本，帮助模型正确填充 Person 字段
            // 你也可以添加可选描述，帮助 LLM 更好地理解某个字段
            private String firstName;
            private String lastName;
            private LocalDate birthDate;

            @Override
            public String toString() {
                return "Person {" +
                        " firstName = \"" + firstName + "\"" +
                        ", lastName = \"" + lastName + "\"" +
                        ", birthDate = " + birthDate +
                        " }";
            }
        }

        // 定义 AI 服务接口，返回类型是 Person
        interface PersonExtractor {

            @UserMessage("从以下文本中提取一个人物信息: {{it}}")
            Person extractPersonFrom(String text);
        }

        public static void main(String[] args) {

            // 单独创建一个 ChatModel（这里开启了 JSON 严格模式）
            ChatModel chatModel = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    // 当抽取 POJO、且使用的 LLM 支持"json mode"特性时
                    // （例如 OpenAI、Azure OpenAI、Vertex AI Gemini、Ollama 等），
                    // 建议开启 json mode 以获得更可靠的结果。
                    // 开启该特性后，LLM 会被强制输出合法的 JSON。
                    .responseFormat("json_schema") // 指定响应格式为 JSON Schema
                    .strictJsonSchema(true) // 开启严格 JSON Schema 校验。参考：https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-json-mode
                    .logRequests(true)  // 打印请求日志，便于调试
                    .logResponses(true) // 打印响应日志，便于调试
                    .build();

            PersonExtractor extractor = AiServices.create(PersonExtractor.class, chatModel);

            // 一段描述人物信息的中文文本，从中要提取出 John Doe 与其出生日期
            String text = "1968 年，在独立日余音未尽时，一个名叫 John 的孩子在宁静的夜空下诞生。"
                    + "这个姓 Doe 的新生儿，开启了他人生的新旅程。";


            Person person = extractor.extractPersonFrom(text);

            System.out.println(person); // Person { firstName = "John", lastName = "Doe", birthDate = 1968-07-04 }
        }
    }


    /**
     * 示例：让 AI 服务把文本解析成带字段描述的 POJO（Recipe 菜谱），
     * 并通过 {@code @StructuredPrompt} 结构化的 prompt 对象来生成提示词。
     * <p>
     * 展示了两种调用方式：直接用可变参数传配料，或传入装配好的 prompt 对象。
     */
    static class POJO_With_Descriptions_Extracting_AI_Service_Example {

        // 菜谱 POJO，每个字段的 @Description 都用于指导 LLM 生成内容
        static class Recipe {

            @Description("简短的标题，最多 3 个单词") // 发送给 LLM：短标题，最多 3 个单词
            private String title;

            @Description("简短的描述，最多 2 句话") // 发送给 LLM：简短描述，最多 2 句话
            private String description;

            @Description("每一步用 4 个单词描述，步骤之间要押韵") // 发送给 LLM：每步 4 个单词且需押韵
            private List<String> steps;

            private Integer preparationTimeMinutes;

            @Override
            public String toString() {
                return "Recipe {" +
                        " title = \"" + title + "\"" +
                        ", description = \"" + description + "\"" +
                        ", steps = " + steps +
                        ", preparationTimeMinutes = " + preparationTimeMinutes +
                        " }";
            }
        }

        // @StructuredPrompt 用模板定义结构化提示词，{{dish}} 和 {{ingredients}} 是占位符
        // （由对象的属性自动填充）
        @StructuredPrompt("创作一道只用{{ingredients}}就能制作的{{dish}}的菜谱")
        static class CreateRecipePrompt {

            private String dish;              // 菜名（对应 {{dish}}）
            private List<String> ingredients; // 可用配料（对应 {{ingredients}}）
        }

        // Chef 接口：返回类型 Recipe 告诉模型需要输出结构化的菜谱
        interface Chef {

            // 方式一：直接用可变参数传入配料
            Recipe createRecipeFrom(String... ingredients);

            // 方式二：传入装配好的结构化 prompt 对象
            Recipe createRecipe(CreateRecipePrompt prompt);
        }

        public static void main(String[] args) {

            ChatModel chatModel = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    // 当抽取 POJO、且使用的 LLM 支持"json mode"特性时
                    // （例如 OpenAI、Azure OpenAI、Vertex AI Gemini、Ollama 等），
                    // 建议开启 json mode 以获得更可靠的结果。
                    // 开启该特性后，LLM 会被强制输出合法的 JSON。
                    .responseFormat("json_schema") // 指定响应格式为 JSON Schema
                    .strictJsonSchema(true) // 严格 JSON Schema。参考：https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-json-mode
                    .logRequests(true)  // 打印请求日志
                    .logResponses(true) // 打印响应日志
                    .build();

            Chef chef = AiServices.create(Chef.class, chatModel);

            // 方式一调用：传入若干配料，模型生成对应菜谱
            Recipe recipe = chef.createRecipeFrom("黄瓜", "西红柿", "羊奶酪", "洋葱", "橄榄");

            System.out.println(recipe);
            // 可能的输出（模型输出内容不固定）：
            // Recipe {
            //     title = "希腊沙拉",
            //     description = "由蔬菜和羊奶酪搭配油醋汁调制的清爽沙拉。",
            //     steps = [
            //         "切黄瓜和番茄",
            //         "加入洋葱和橄榄",
            //         "撒上羊奶酪",
            //         "淋上酱汁享用!"
            //     ],
            //     preparationTimeMinutes = 10
            // }


            // 方式二调用：先构造结构化 prompt 对象再调用
            CreateRecipePrompt prompt = new CreateRecipePrompt();
            prompt.dish = "沙拉";
            prompt.ingredients = asList("黄瓜", "西红柿", "羊奶酪", "洋葱", "橄榄");

            Recipe anotherRecipe = chef.createRecipe(prompt);
            System.out.println(anotherRecipe);
            // 输出的菜谱结构同上方 ...
        }
    }


    /**
     * 示例：在 AI 服务接口中使用 {@code @SystemMessage} 设置系统提示词。
     * <p>
     * 系统消息用来设定 LLM 的角色和行为准则，在每次对话前自动注入。
     */
    static class AI_Service_with_System_Message_Example {

        interface Chef {

            // @SystemMessage 指定系统提示词，设定 LLM 扮演一个专业的、友好礼貌且简洁的厨师
            @SystemMessage("你是一名专业厨师。你待人友好、礼貌且言简意赅。")
            String answer(String question);
        }

        public static void main(String[] args) {

            Chef chef = AiServices.create(Chef.class, chatModel);

            String answer = chef.answer("鸡肉应该烤多久？");
            System.out.println(answer); // 烤鸡肉通常每面需要大约 10-15 分钟，具体取决于 ...
        }
    }


    /**
     * 示例：在 AI 服务接口中同时使用 {@code @SystemMessage} 和 {@code @UserMessage}，
     * 并利用 {@code @V} 把方法参数绑定到消息模板中的变量。
     * <p>
     * 也演示了返回 {@code List<String>}（例如把总结内容拆成多行）的用法。
     */
    static class AI_Service_with_System_and_User_Messages_Example {

        interface TextUtils {

            // @V 用于把方法参数绑定到模板变量：
            // text 绑定到 {{text}}，language 绑定到 {{language}}
            @SystemMessage("你是一名专业的{{language}}翻译")
            @UserMessage("翻译以下文本：{{text}}")
            String translate(@V("text") String text, @V("language") String language);

            // 返回 List<String>：让模型用 {{n}} 条要点总结每条用户消息，仅输出要点即可
            // @UserMessage 直接修饰参数，表示该参数就是用户消息本体
            @SystemMessage("将用户的每条消息总结成 {{n}} 条要点。只提供要点内容。")
            List<String> summarize(@UserMessage String text, @V("n") int n);
        }

        public static void main(String[] args) {

            TextUtils utils = AiServices.create(TextUtils.class, chatModel);

            // 把"你好，最近怎么样？"翻译成意大利语
            String translation = utils.translate("你好，最近怎么样？", "意大利语");
            System.out.println(translation); // Ciao, come stai?


            // 一段要总结为要点的中文文本
            String text = "人工智能（AI）是计算机科学的一个分支，旨在创造能够模仿人类智能的机器。"
                    + "这可以涵盖从识别图案或语音等简单任务，到做出决策或预测等更复杂的任务。";

            // 用 3 条要点总结上文
            List<String> bulletPoints = utils.summarize(text, 3);
            System.out.println(bulletPoints);
            // [
            //     "- AI 是计算机科学的一个分支",
            //     "- 它的目标是创造模仿人类智能的机器",
            //     "- 它可以执行简单或复杂的任务"
            // ]
        }
    }


    /**
     * 示例：把系统消息和用户消息的模板放到外部资源文件中，再通过
     * {@code @SystemMessage(fromResource = ...)} / {@code @UserMessage(fromResource = ...)} 加载。
     * <p>
     * 模板文件位于 classpath 的 resources 目录下，便于集中管理和修改。
     */
    static class AI_Service_with_System_and_User_Messages_loaded_from_resources_Example {

        interface TextUtils {

            // 从 resources 中加载系统/用户消息模板
            @SystemMessage(fromResource = "/translator-system-prompt-template.txt")
            @UserMessage(fromResource = "/translator-user-prompt-template.txt")
            String translate(@V("text") String text, @V("language") String language);
        }

        public static void main(String[] args) {

            TextUtils utils = AiServices.create(TextUtils.class, chatModel);

            String translation = utils.translate("你好，最近怎么样？", "意大利语");
            System.out.println(translation); // Ciao, come stai?
        }
    }


    /**
     * 示例：在 AI 服务接口中使用 {@code @UserName} 注入用户名称。
     * <p>
     * 被 {@code @UserName} 修饰的参数会作为"用户名"传给模型，便于个性化回应。
     */
    static class AI_Service_with_UserName_Example {

        interface Assistant {

            // @UserName 参数表示用户名，@UserMessage 参数表示用户消息正文
            String chat(@UserName String name, @UserMessage String message);
        }

        public static void main(String[] args) {

            Assistant assistant = AiServices.create(Assistant.class, chatModel);

            String answer = assistant.chat("Klaus", "嗨，如果你能看到我的名字就告诉我。");
            System.out.println(answer); // 你好！你的名字是 Klaus。今天需要我帮你做点什么吗？
        }
    }

    /**
     * 示例：根据 {@code @MemoryId} 动态提供系统消息。
     * <p>
     * 通过 systemMessageProvider 提供函数，可以根据 memoryId 返回不同的系统提示词，
     * 从而在同一接口下为不同会话/用户定制角色设定。
     */
    static class AI_Service_with_Dynamic_System_Message_Example {

        interface Assistant {

            // @MemoryId 标记会话/用户标识，配合 systemMessageProvider 使用
            String chat(@MemoryId String memoryId, @UserMessage String userMessage);
        }

        public static void main(String[] args) {

            // 根据 memoryId 返回不同的系统提示词：memoryId 为 1 时用户被称作"陛下"
            Function<Object, String> systemMessageProvider = (memoryId) -> {
                if (memoryId.equals("1")) {
                    return "你是一个乐于助人的助手，用户希望被称作“陛下”。";
                } else {
                    return "你是一个乐于助人的助手。";
                }
            };

            // 使用 AiServices.builder 手动装配：指定模型并配置动态系统消息提供者
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(chatModel)
                    .systemMessageProvider(systemMessageProvider)
                    .build();

            System.out.println(assistant.chat("1", "你好")); // 你好，陛下！今天有什么能为您效劳？
            System.out.println(assistant.chat("2", "你好")); // 你好！今天需要我帮你做点什么吗？
        }
    }
}
