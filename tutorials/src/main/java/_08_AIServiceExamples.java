import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
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

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

/**
 * 教程第 8 课：AI 服务（AI Service）——LangChain4j 最强大、最常用的功能之一。
 * <p>
 * AI Service 的思路是：你只需定义一个"接口"，接口里声明方法的参数和返回类型，
 * 再用 {@code AiServices.create()} 生成接口的实现，LangChain4j 就会自动：
 *  - 把方法参数拼进提示词发给 LLM；
 *  - 把 LLM 返回的文本自动解析成接口声明的返回类型（String、int、枚举、POJO、List 等）。
 * <p>
 * 本文件包含大量独立示例（每个是一个 static class，各自带 main 方法）：
 * 简单对话、系统消息 + 变量、抽取各种数据类型（情感/枚举/数字/日期/POJO）、
 * 给 POJO 字段加描述、以及带记忆的 AI 服务。运行某个示例时，直接运行对应的内部类即可。
 */
public class _08_AIServiceExamples {

    // 顶层共享的聊天模型：所有内部示例类复用它（仅 POJO 相关示例为了开启 json 模式会另建模型）
    static ChatModel model = OpenAiChatModel.builder()
            .apiKey(ApiKeys.OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .timeout(ofSeconds(60))
            .build();

    ////////////////// 简单示例 //////////////////////

    /**
     * 最简单的 AI 服务：接口只有一个方法 {@code String chat(String message)}。
     * 调用它 = 把 message 作为用户消息发给 LLM 并返回回答文本。
     */
    static class Simple_AI_Service_Example {

        /**
         * AI 服务接口：只需要声明方法签名，不需要写任何实现。
         */
        interface Assistant {

            /**
             * 把用户消息发给 LLM，返回回答。
             *
             * @param message 用户消息
             * @return LLM 的回答（String）
             */
            String chat(String message);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 1. 用 AiServices.create() 为接口生成实现，并注入聊天模型
            Assistant assistant = AiServices.create(Assistant.class, model);

            // 2. 准备一条用户消息：让 AI 把一段法语金融术语翻译出来
            //    （单引号内的法语原文是待翻译的内容）
            String userMessage = "翻译：'Plus-Values des cessions de valeurs mobilières, de droits sociaux et gains assimilés'";

            // 3. 调用接口方法，自动完成"发消息给模型 → 拿回回答"
            String answer = assistant.chat(userMessage);

            // 4. 打印回答
            System.out.println(answer);
        }
    }

    ////////////////// 带系统消息和变量 //////////////////////

    /**
     * 带 @SystemMessage 的 AI 服务：用注解设定 AI 的身份和行为。
     * 每次调用接口方法时，注解里的系统消息会自动包含在发给 LLM 的消息中。
     */
    static class AI_Service_with_System_Message_Example {

        /**
         * AI 服务接口：Chef（厨师）。@SystemMessage 设置了 AI 扮演一名专业厨师。
         */
        interface Chef {

            /**
             * 回答用户的烹饪问题。
             *
             * @param question 用户的问题
             * @return AI 的回答
             */
            @SystemMessage("你是一位专业厨师。你为人友善、礼貌、说话简洁。")
            String answer(String question);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 生成 Chef 接口的实现
            Chef chef = AiServices.create(Chef.class, model);

            // 调用方法：除了用户消息，系统消息"你是专业厨师…"也会自动发给模型
            String answer = chef.answer("鸡肉通常需要烤多久？");

            // 打印回答（示例输出：通常每面大约烤 10-15 分钟……）
            System.out.println(answer);
        }
    }

    /**
     * 带 @SystemMessage、@UserMessage 和模板变量的 AI 服务。
     * <p>
     * 模板里可以用 {{变量名}} 占位符，占位符的值来自方法参数：
     * 参数上用 @V("变量名") 标注哪个参数对应哪个占位符。
     * 还有一个特殊写法：@UserMessage 直接加在 String 参数上，
     * 表示"这个参数本身的内容就是用户消息"。
     */
    static class AI_Service_with_System_and_User_Messages_Example {

        /**
         * AI 服务接口：文本工具。
         */
        interface TextUtils {

            /**
             * 翻译文本。
             *
             * @param text     要翻译的文本（对应占位符 {{text}}）
             * @param language 目标语言（对应占位符 {{language}}）
             * @return 翻译结果
             */
            @SystemMessage("你是一位专业的{{language}}翻译员")
            @UserMessage("请翻译下面的文本：{{text}}")
            String translate(@V("text") String text, @V("language") String language);

            /**
             * 把用户文本概括成 n 个要点。
             *
             * @param text 用户文本（作为用户消息传入）
             * @param n    要生成的要点数量（对应占位符 {{n}}）
             * @return 要点列表，每项是一个字符串
             */
            @SystemMessage("把用户的每条消息概括成{{n}}个要点。只输出要点本身。")
            List<String> summarize(@UserMessage String text, @V("n") int n);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 生成 TextUtils 接口的实现
            TextUtils utils = AiServices.create(TextUtils.class, model);

            // 1. 调用 translate：把"你好，最近怎么样？"翻译成意大利语
            //    （参数值会自动填入 {{text}} 和 {{language}} 占位符）
            String translation = utils.translate("你好，最近怎么样？", "意大利语");
            System.out.println(translation); // 输出示例：意大利语译文，如 Ciao, come stai?

            // 2. 准备一段关于人工智能的文本，稍后让 AI 概括成 3 个要点
            String text = "人工智能（AI）是计算机科学的一个分支，其目标是制造出能够模仿人类智能的机器。"
                    + "这既包括识别模式或语音这类简单任务，也包括做决策或预测这类更复杂的任务。";

            // 3. 调用 summarize：返回类型是 List<String>，LangChain4j 会自动把回答解析成列表
            List<String> bulletPoints = utils.summarize(text, 3);
            // 打印每个要点
            bulletPoints.forEach(System.out::println);
            // 输出示例：
            // [
            // "- 人工智能是计算机科学的一个分支",
            // "- 它旨在制造能模仿人类智能的机器",
            // "- 它既能完成简单任务，也能完成复杂任务"
            // ]
        }
    }

    //////////////////// 抽取不同的数据类型 ////////////////////

    /**
     * 从文本中抽取"情感倾向"（返回类型是枚举）。
     * <p>
     * 演示把 LLM 的输出自动解析成枚举常量：只要返回类型是枚举，
     * LangChain4j 就会让模型从枚举常量里选一个作为回答。
     */
    static class Sentiment_Extracting_AI_Service_Example {

        /**
         * 情感倾向的枚举：正面 / 中性 / 负面。
         */
        enum Sentiment {
            POSITIVE, NEUTRAL, NEGATIVE
        }

        /**
         * AI 服务接口：情感分析器。
         */
        interface SentimentAnalyzer {

            /**
             * 分析文本的情感倾向。
             *
             * @param text 待分析的文本（作为 {{it}} 填入用户消息）
             * @return 情感倾向枚举（POSITIVE / NEUTRAL / NEGATIVE）
             */
            @UserMessage("分析{{it}}的情感倾向")
            Sentiment analyzeSentimentOf(String text);

            /**
             * 判断文本是否是正面情感。
             *
             * @param text 待分析的文本
             * @return 布尔值：正面为 true，否则为 false
             */
            @UserMessage("{{it}}的情感是正面的吗？")
            boolean isPositive(String text);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 生成情感分析器接口的实现
            SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

            // 分析"这很好！"的情感倾向
            Sentiment sentiment = sentimentAnalyzer.analyzeSentimentOf("这很好！");
            System.out.println(sentiment); // 输出示例：POSITIVE（正面）

            // 判断"这很糟糕！"是否是正面情感
            boolean positive = sentimentAnalyzer.isPositive("这很糟糕！");
            System.out.println(positive); // 输出示例：false（否）
        }
    }

    /**
     * 从酒店评价中抽取"问题类别"列表（返回类型是 List&lt;枚举&gt;）。
     * <p>
     * 演示：输入一篇长评论，AI 自动判断其中涉及哪些问题类别，
     * LangChain4j 会把模型输出解析成 List&lt;IssueCategory&gt;。
     * 模板中用 ||| 作为"定界符"包裹 {{it}}，帮助模型清晰地区分待分析内容。
     */
    static class Hotel_Review_AI_Service_Example {

        /**
         * 酒店可能存在的问题类别枚举。
         */
        public enum IssueCategory {
            MAINTENANCE_ISSUE,        // 设施维护问题
            SERVICE_ISSUE,            // 服务问题
            COMFORT_ISSUE,            // 舒适度问题
            FACILITY_ISSUE,           // 设施配套问题
            CLEANLINESS_ISSUE,        // 清洁问题
            CONNECTIVITY_ISSUE,       // 网络连接问题
            CHECK_IN_ISSUE,           // 入住办理问题
            OVERALL_EXPERIENCE_ISSUE  // 整体体验问题
        }

        /**
         * AI 服务接口：酒店评价问题分析器。
         */
        interface HotelReviewIssueAnalyzer {

            /**
             * 分析一段酒店评价，找出其中涉及的所有问题类别。
             *
             * @param review 评价文本（填入 {{it}} 占位符）
             * @return 问题类别列表
             */
            @UserMessage("请分析下面这段酒店评价涉及的问题：|||{{it}}|||")
            List<IssueCategory> analyzeReview(String review);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 生成接口实现
            HotelReviewIssueAnalyzer hotelReviewIssueAnalyzer = AiServices.create(HotelReviewIssueAnalyzer.class, model);

            // 一篇酒店评价：位置好、房间大，但空调坏了、客房服务慢……
            String review = "我们在酒店的入住体验喜忧参半。位置很完美，离海滩只有几步之遥，"
                    + "让我们的日常出行非常方便。房间宽敞、装修精美，提供了一个舒适宜人的环境。"
                    + "然而，入住期间我们遇到了几个问题。房间的空调无法正常工作，导致晚上非常不舒服。"
                    + "另外客房服务很慢，我们不得不打多次电话才拿到额外的毛巾。"
                    + "尽管员工很友好、早餐自助也很棒，但这些问题还是明显影响了我们的入住体验。";

            // 调用方法：返回 List<IssueCategory>
            List<IssueCategory> issueCategories = hotelReviewIssueAnalyzer.analyzeReview(review);

            // 输出示例：[MAINTENANCE_ISSUE（维护问题）, SERVICE_ISSUE（服务问题）, COMFORT_ISSUE（舒适度问题）, OVERALL_EXPERIENCE_ISSUE（整体体验问题）]
            System.out.println(issueCategories);
        }
    }

    /**
     * 从文本中抽取"数字"（返回类型支持 int / long / BigInteger / float / double / BigDecimal）。
     * <p>
     * 演示同一个思路针对不同的数值返回类型都适用：
     * LangChain4j 会根据返回类型自动把模型输出的数字解析成对应的 Java 数值类型。
     */
    static class Number_Extracting_AI_Service_Example {

        /**
         * AI 服务接口：数字抽取器。六个方法都用 {{it}} 填入同一段文本，
         * 区别只在返回类型不同。
         */
        interface NumberExtractor {

            @UserMessage("从{{it}}中提取数字")
            int extractInt(String text);

            @UserMessage("从{{it}}中提取数字")
            long extractLong(String text);

            @UserMessage("从{{it}}中提取数字")
            BigInteger extractBigInteger(String text);

            @UserMessage("从{{it}}中提取数字")
            float extractFloat(String text);

            @UserMessage("从{{it}}中提取数字")
            double extractDouble(String text);

            @UserMessage("从{{it}}中提取数字")
            BigDecimal extractBigDecimal(String text);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 生成接口实现
            NumberExtractor extractor = AiServices.create(NumberExtractor.class, model);

            // 一段文本：超级计算机"深思"（Deep Thought）经过无数万年计算后，
            // 宣布生命、宇宙以及一切的终极答案是"四十二"。
            String text = "经过无数万年的计算，超级计算机“深思”终于宣布："
                    + "生命、宇宙以及一切的终极答案是四十二。";

            // 依次用不同的数值返回类型抽取（模型应能识别出答案是 42）
            int intNumber = extractor.extractInt(text);
            System.out.println(intNumber); // 输出示例：42

            long longNumber = extractor.extractLong(text);
            System.out.println(longNumber); // 输出示例：42

            BigInteger bigIntegerNumber = extractor.extractBigInteger(text);
            System.out.println(bigIntegerNumber); // 输出示例：42

            float floatNumber = extractor.extractFloat(text);
            System.out.println(floatNumber); // 输出示例：42.0

            double doubleNumber = extractor.extractDouble(text);
            System.out.println(doubleNumber); // 输出示例：42.0

            BigDecimal bigDecimalNumber = extractor.extractBigDecimal(text);
            System.out.println(bigDecimalNumber); // 输出示例：42.0
        }
    }

    /**
     * 从文本中抽取"日期和时间"（返回类型支持 LocalDate / LocalTime / LocalDateTime）。
     * <p>
     * 演示 LLM 输出如何自动解析成 Java 8 的时间类型，省去手工解析日期字符串的麻烦。
     */
    static class Date_and_Time_Extracting_AI_Service_Example {

        /**
         * AI 服务接口：日期时间抽取器。
         */
        interface DateTimeExtractor {

            @UserMessage("从{{it}}中提取日期")
            LocalDate extractDateFrom(String text);

            @UserMessage("从{{it}}中提取时间")
            LocalTime extractTimeFrom(String text);

            @UserMessage("从{{it}}中提取日期和时间")
            LocalDateTime extractDateTimeFrom(String text);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 生成接口实现
            DateTimeExtractor extractor = AiServices.create(DateTimeExtractor.class, model);

            // 一段文本：宁静笼罩着 1968 年的那个夜晚，距离午夜还差一刻钟，
            // 正值独立日庆祝活动结束之后。
            String text = "1968 年的那个夜晚弥漫着宁静，当时离午夜还有十五分钟，"
                    + "独立日（7 月 4 日）的庆祝活动刚刚结束。";

            // 抽取日期
            LocalDate date = extractor.extractDateFrom(text);
            System.out.println(date); // 输出示例：1968-07-04

            // 抽取时间
            LocalTime time = extractor.extractTimeFrom(text);
            System.out.println(time); // 输出示例：23:45

            // 抽取日期和时间
            LocalDateTime dateTime = extractor.extractDateTimeFrom(text);
            System.out.println(dateTime); // 输出示例：1968-07-04T23:45
        }
    }

    /**
     * 从文本中抽取一个 POJO 对象（Person）。
     * <p>
     * 演示返回类型为自定义 Java 类时，LangChain4j 会让模型输出结构化数据，
     * 并自动填装成 Person 对象。这里还开启了 OpenAI 的"json_schema"严格模式，
     * 强制模型输出合法 JSON，让结果更可靠。
     */
    static class POJO_Extracting_AI_Service_Example {

        /**
         * 人（POJO）。字段会由模型根据文本内容自动填值。
         */
        static class Person {

            @Description("人的名") // 可以加可选描述，帮助 LLM 更准确地理解该字段的含义
            private String firstName;
            private String lastName;
            private LocalDate birthDate;

            /**
             * 把 Person 对象转成可读的字符串（便于打印查看结果）。
             *
             * @return Person 的字符串表示
             */
            @Override
            public String toString() {
                return "Person {" +
                        " firstName = \"" + firstName + "\"" +
                        ", lastName = \"" + lastName + "\"" +
                        ", birthDate = " + birthDate +
                        " }";
            }
        }

        /**
         * AI 服务接口：人物抽取器。
         */
        interface PersonExtractor {

            /**
             * 从一段文本中抽取一个人物信息。
             *
             * @param text 包含人物信息的文本
             * @return 解析出的 Person 对象
             */
            @UserMessage("从下面的文本中抽取一个人物信息：{{it}}")
            Person extractPersonFrom(String text);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 注意：抽取 POJO 时，如果所用 LLM 支持"json 模式"（例如 OpenAI、Azure OpenAI、
            // Vertex AI Gemini、Ollama 等），建议开启该模式以获得更可靠的结果。
            // 开启后，LLM 会被强制输出合法的 JSON。
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    .responseFormat("json_schema")                    // 要求模型以 JSON 结构输出
                    .strictJsonSchema(true) // 严格模式：强制输出符合声明的 JSON 结构（参考文档见注释行 URL）
                    .timeout(ofSeconds(60))
                    .build();

            // 生成接口实现（注入开启了 json 模式的模型）
            PersonExtractor extractor = AiServices.create(PersonExtractor.class, model);

            // 一段文本：1968 年，独立日的余晖中，一个名叫 John 的孩子在宁静的夜空下出生，
            // 这个新生儿姓 Doe，标志着一场新旅程的开始。
            String text = "1968 年，独立日的余晖尚存，一个名叫 John 的孩子在宁静的夜空下呱呱坠地。"
                    + "这个姓 Doe 的新生儿，标志着一场崭新旅程的开始。";

            // 调用方法：模型从文本中识别出 John Doe 和出生日期，并填充成 Person 对象
            Person person = extractor.extractPersonFrom(text);

            // 输出示例：Person { firstName = "John", lastName = "Doe", birthDate = 1968-07-04 }
            System.out.println(person);
        }
    }

    ////////////////////// 字段描述 ////////////////////////

    /**
     * 用 @Description 给 POJO 字段加"描述"的示例。
     * <p>
     * 当模型需要生成更复杂、更讲究格式的内容时，用 @Description 给每个字段
     * 补充约束条件（如字数、风格），模型会严格按照这些要求生成内容。
     * <p>
     * 本示例演示一个"大厨"接口：既可以直接给一串食材生成菜谱（返回 Recipe 对象），
     * 也可以传入一个结构化提示词对象（CreateRecipePrompt）来生成菜谱。
     */
    static class POJO_With_Descriptions_Extracting_AI_Service_Example {

        /**
         * 菜谱（POJO）。@Description 为每个字段提供生成约束。
         */
        static class Recipe {

            @Description("简短标题，最多 3 个字")
            private String title;

            @Description("简短描述，最多 2 句话")
            private String description;

            @Description("每个步骤用 6 到 8 个字描述，步骤之间要押韵")
            private List<String> steps;

            private Integer preparationTimeMinutes;

            /**
             * 把 Recipe 对象转成可读字符串。
             *
             * @return Recipe 的字符串表示
             */
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

        /**
         * 结构化提示词：用注解声明"菜谱请求"模板，占位符 {{dish}}、{{ingredients}}
         * 会自动对应同名字段（dish、ingredients）。
         */
        @StructuredPrompt("创建一个只用{{ingredients}}就能做出的{{dish}}菜谱")
        static class CreateRecipePrompt {

            private String dish;              // 菜品种类
            private List<String> ingredients; // 食材列表
        }

        /**
         * AI 服务接口：大厨。两个方法都返回 Recipe 对象。
         */
        interface Chef {

            /**
             * 直接传入若干食材，生成菜谱。
             *
             * @param ingredients 可变数量的食材（会合并成一句话发给模型）
             * @return Recipe 菜谱对象
             */
            Recipe createRecipeFrom(String... ingredients);

            /**
             * 传入结构化的菜谱请求对象，生成菜谱。
             *
             * @param prompt 菜谱请求（含菜品和食材）
             * @return Recipe 菜谱对象
             */
            Recipe createRecipe(CreateRecipePrompt prompt);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 与上一个 POJO 示例相同：抽取 POJO 时开启 json 模式，让模型输出合法 JSON
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    .responseFormat("json_schema")
                    .strictJsonSchema(true) // https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-json-mode
                    .timeout(ofSeconds(60))
                    .build();

            // 生成接口实现
            Chef chef = AiServices.create(Chef.class, model);

            // 方式一：直接给一串食材，让 AI 生成菜谱
            Recipe recipe = chef.createRecipeFrom("黄瓜", "番茄", "羊乳酪", "洋葱", "橄榄", "柠檬");

            System.out.println(recipe);
            // 输出示例：
            // Recipe {
            // title = "希腊沙拉",
            // description = "清爽的蔬菜与羊乳酪组合，佐以清新的油醋汁。",
            // steps = [
            // "黄瓜番茄切块",
            // "加入洋葱橄榄",
            // "上面撒上羊乳酪",
            // "淋上料汁开吃"
            // ],
            // preparationTimeMinutes = 10
            // }

            // 方式二：用结构化提示词对象（通过注解 + 字段值）生成另一份菜谱
            CreateRecipePrompt prompt = new CreateRecipePrompt();
            prompt.dish = "烤箱菜";
            prompt.ingredients = asList("黄瓜", "番茄", "羊乳酪", "洋葱", "橄榄", "土豆");

            Recipe anotherRecipe = chef.createRecipe(prompt);
            System.out.println(anotherRecipe);
            // 输出示例：一个符合约束的 Recipe 对象（内容随机）……
        }
    }


    ////////////////////////// 带记忆 /////////////////////////

    /**
     * 带对话记忆的 AI 服务。
     * <p>
     * 通过 AiServices.builder() 给接口注入 chatMemory（消息窗口记忆），
     * 这样 AI 服务就能记住多轮对话，实现"上下文连续"的聊天体验。
     */
    static class ServiceWithMemoryExample {

        /**
         * AI 服务接口：助手。
         */
        interface Assistant {

            /**
             * 与助手对话（自动带上历史记忆）。
             *
             * @param message 本轮用户消息
             * @return 助手的回答
             */
            String chat(String message);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 创建"消息窗口"记忆：最多保留最近 10 条消息
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

            // 用建造者模式组装 AI 服务：注入聊天模型 + 对话记忆
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    .chatMemory(chatMemory)
                    .build();

            // 第一轮：自我介绍
            String answer = assistant.chat("你好！我叫克劳斯（Klaus）。");
            System.out.println(answer); // 输出示例：你好 Klaus！今天有什么可以帮您？

            // 第二轮：因为 AI 记住了上轮内容，所以它能回答出用户的名字
            String answerWithName = assistant.chat("我叫什么名字？");
            System.out.println(answerWithName); // 输出示例：你的名字是 Klaus。
        }
    }

    /**
     * 为"每个用户"分别维护记忆的 AI 服务。
     * <p>
     * 通过 @MemoryId 标注方法参数作为"用户/会话标识"，再用 chatMemoryProvider
     * 提供"每个标识对应一份记忆"的逻辑。这样不同用户之间互不干扰，
     * 每个用户都能拥有自己独立的对话上下文。
     */
    static class ServiceWithMemoryForEachUserExample {

        /**
         * AI 服务接口：助手。
         */
        interface Assistant {

            /**
             * 与助手对话（带"按用户隔离"的记忆）。
             *
             * @param memoryId   会话/用户标识：不同 id 使用不同的记忆
             * @param userMessage 本轮用户消息
             * @return 助手的回答
             */
            String chat(@MemoryId int memoryId, @UserMessage String userMessage);
        }

        /**
         * 程序入口 main 方法。
         *
         * @param args 命令行参数，本示例未使用
         */
        public static void main(String[] args) {

            // 生成接口实现：chatMemoryProvider 为每个 memoryId 新建一份独立的记忆（最多 10 条消息）
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                    .build();

            // 用户 1（memoryId=1）自我介绍
            System.out.println(assistant.chat(1, "你好，我叫克劳斯（Klaus）"));
            // 输出示例：Hi Klaus！今天有什么可以帮您？

            // 用户 2（memoryId=2）自我介绍
            System.out.println(assistant.chat(2, "你好，我叫弗朗辛（Francine）"));
            // 输出示例：Hello Francine！今天有什么可以帮您？

            // 用户 1 再次提问：因为记忆按 id 隔离，AI 应该记得 id=1 的用户叫 Klaus
            System.out.println(assistant.chat(1, "我叫什么名字？"));
            // 输出示例：你的名字是 Klaus。

            // 用户 2 提问：AI 应该记得 id=2 的用户叫 Francine
            System.out.println(assistant.chat(2, "我叫什么名字？"));
            // 输出示例：你的名字是 Francine。
        }
    }
}
