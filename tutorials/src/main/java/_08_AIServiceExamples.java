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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

/**
 * 教程第 8 课：AI 服务（AI Service）——LangChain4j 最强大、最常用的功能之一。
 * <p>
 * 站在企业开发的角度，AiService 是 LangChain4j 诸多抽象中最"像工程产品"的一个。
 * 它的本质是：用「声明式接口」把业务代码里的 AI 交互规范化、类型化、可测试化——
 * 你不再手写 {@code model.chat("一堆拼好的提示词")} 再手工解析返回字符串，而是把
 * 调用"伪装"成一个返回类型明确的普通 Java 方法，上层业务完全不感知背后是 LLM。
 * <p>
 * <b>一、核心能力（从下方示例提炼）</b>
 * <ul>
 *   <li><b>结构化输出</b>：LLM 返回的是文本，但业务系统要类型安全的数据。AiService
 *       能把回答自动解析成枚举、int/long/BigDecimal、LocalDate/LocalTime、自定义 POJO、
 *       List&lt;枚举&gt; 等，免除手写正则或脆弱的字符串解析。</li>
 *   <li><b>提示词模板管理</b>：通过 @SystemMessage / @UserMessage + {{变量}} + @V 把
 *       提示词管理起来，避免在业务代码里到处拼接字符串，也便于外置维护。</li>
 *   <li><b>注入对话记忆</b>：AiServices.builder().chatMemory(...) 让 AI 记住多轮对话；
 *       @MemoryId + chatMemoryProvider 实现多用户/多会话隔离。</li>
 *   <li><b>声明式人设/行为</b>：@SystemMessage 为不同业务场景定义不同"角色"的 AI，
 *       一个应用内可有多个 AiService 接口对应不同职能。</li>
 * </ul>
 * <p>
 * <b>二、对企业开发的价值</b>
 * <ul>
 *   <li>可测试性：接口方法类型明确，可 mock 掉 AIService 做单元测试。</li>
 *   <li>健壮性：类型解析、超时、异常统一处理，避免 LLM 输出不确定性污染业务层。</li>
 *   <li>可维护性：业务逻辑与"提示词 / OAI 调用细节"解耦，提示词团队与开发团队各司其职。</li>
 *   <li>成本/观测可控：可在 AiService 层统一加日志、限流、监控（结合后续的 Listener）。</li>
 * </ul>
 * <p>
 * <b>一句话总结</b>：AiService 是 LangChain4j 给 Java 开发者提供的、把大模型封装成
 * 「可声明、可注入、可测试的业务服务」的工程化层。企业里写 AI 功能应优先定义 AiService
 * 接口（类型安全、带记忆、带提示词模板），而不是直接裸调 model.chat()。
 * <p>
 * 本文件包含大量独立示例（每个是一个 static class，各自带 main 方法）：
 * 简单对话、系统消息 + 变量、抽取各种数据类型（情感/枚举/数字/日期/POJO）、
 * 给 POJO 字段加描述、以及带记忆的 AI 服务。运行某个示例时，直接运行对应的内部类即可。
 * 文件末尾新增了一个《企业实战示例》——问题提单 + 入库存 JSON + 多租户记忆隔离 的模拟。
 */
public class _08_AIServiceExamples {

    // 顶层共享的聊天模型：所有内部示例类复用它（仅 POJO 相关示例为了开启 json 模式会另建模型）
    static ChatModel model = Model.MODEL;

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
            @UserMessage("{{text}}")
            List<String> summarize(@V("text")String text, @V("n") int n);
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
            @UserMessage("分析{{it}}的情感倾向") // 只有一个参数的时候，不需要写@V
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

    ////////////////////////// 企业实战示例 /////////////////////////

    /**
     * 企业实战综合示例：问题提单 + 入库存 JSON + 多租户记忆隔离。
     * <p>
     * 模拟企业里最常见的一个场景——"智能工单客服"：
     * 用户（或多租户里的某个租户）描述遇到的问题，AI 服务自动：
     * 1. <b>抽取结构化工单</b>：从自然语言里提取等级、所属模块、标题、描述，返回强类型的
     *    {@code Ticket} POJO（企业里可再序列化成 JSON 入库存数据库，例如用 Jackson/Gson/MyBatis 落库）。
     * 2. <b>按租户隔离记忆</b>：用 @MemoryId 区分不同租户，每个租户拥有独立的对话记忆，
     *    这样租户 A 能追问"我上一条说的是什么"，而租户 B 完全不受影响。
     * <p>
     * 这演示了 AiService 三件套在企业中的组合拳：
     * 结构化输出（Ticket）+ 多租户记忆隔离（@MemoryId + chatMemoryProvider）+ 声明式提示词模板。
     */
    static class Enterprise_Ticket_Agent_Example {

        /**
         * 优先级枚举。
         */
        enum Priority {
            LOW,        // 低
            MEDIUM,     // 中
            HIGH,       // 高
            CRITICAL    // 紧急
        }

        /**
         * 工单状态枚举。
         */
        enum Status {
            NEW,        // 新建
            IN_PROGRESS,// 处理中
            RESOLVED    // 已解决
        }

        /**
         * 工单 POJO：由 AI 从用户描述中抽取并填装的强类型结构体。
         * <p>
         * 在企业里，这样一个对象可以直接交给 Jackson/Gson/MyBatis 序列化入库，
         * 或放到消息队列传给下游工单系统——这正是"结构化输出"的工程价值：
         * 上层不用手写正则去解析 LLM 的自由文本。
         */
        static class Ticket {

            @Description("工单唯一编号，例如 T-1001")
            private String ticketId;

            @Description("优先级，只能取枚举值")
            private Priority priority;

            @Description("所属模块，例如 登录/订单/支付/客服")
            private String module;

            @Description("工单标题，一句话概括，不超过 20 字")
            private String title;

            @Description("问题的详细描述，2到3句话，忠实于用户原话，不要虚构")
            private String description;

            @Description("工单状态，新建时固定为 NEW")
            private Status status;

            @Override
            public String toString() {
                return "Ticket { ticketId=" + ticketId +
                        ", priority=" + priority +
                        ", module='" + module + '\'' +
                        ", title='" + title + '\'' +
                        ", description='" + description + '\'' +
                        ", status=" + status +
                        " }";
            }
        }

        /**
         * AI 服务接口：智能工单代理。
         * <p>
         * 三个方法分别对应三件事：
         * - {@code createTicket}    只做一件事：抽取结构化工单（入库存 JSON 的原材料）。
         * - {@code chat}            多租户客服对话（带按租户隔离的记忆），让用户能追问、能补充信息。
         * - {@code shortTicket}     让 AI 用一句话概括问题，作为对话中的"速记"输出。
         */
        interface TicketAgent {

            /**
             * 从用户的一句话里抽取一张结构化工单。
             *
             * @param description 用户描述的问题
             * @return 强类型工单对象（可直接序列化入库）
             */
            @SystemMessage("你是企业的智能客服，负责把用户的报障转成结构化工单。")
            @UserMessage("请从下面的问题描述中抽取一张工单：{{description}}")
            Ticket createTicket(@V("description") String description);

            /**
             * 多租户客服对话，自动带当前租户的独立记忆。
             *
             * @param tenantId    租户标识（每个租户独立记忆）
             * @param userMessage 用户本轮消息
             * @return 客服回复
             */
            @SystemMessage("你是企业的客服代表，语气专业、耐心、简洁。回答前可结合本租户此前的对话记忆。")
            String chat(@MemoryId String tenantId, @UserMessage String userMessage);

            /**
             * 用一句话概括问题（演示返回类型可复用结构化输出思路，这里简单返回 String）。
             *
             * @param description 用户描述的问题
             * @return 一句话概括
             */
            @UserMessage("请用一句话概括下面问题：{{description}}")
            String shortTicket(@V("description") String description);
        }

        /**
         * 模拟的"工单仓库"（内存版）。真实企业里这一步通常是：
         * 把 AI 抽出的 {@code Ticket} 对象 → 序列化成 JSON → 写入数据库 / 消息队列 / 工单系统。
         */
        static class TicketRepository {

            private final AtomicLong seq = new AtomicLong(1000);
            // 用租户 id 分组存放，模拟"每个租户各自一张工单表"
            private final Map<String, Map<String, Ticket>> store = new ConcurrentHashMap<>();

            /**
             * 保存一张工单（自动分配编号，并记到对应租户下）。
             *
             * @param tenantId 租户标识
             * @param ticket   未编号的工单对象
             * @return 保存后的工单（带编号）
             */
            Ticket save(String tenantId, Ticket ticket) {
                // 为工单生成唯一编号
                if (ticket.ticketId == null) {
                    ticket.ticketId = "T-" + seq.incrementAndGet();
                }
                store.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>()).put(ticket.ticketId, ticket);
                return ticket;
            }

            /**
             * 打印某个租户的全部工单（模拟"查库"）。
             *
             * @param tenantId 租户标识
             */
            void dump(String tenantId) {
                System.out.println("  —— 租户 " + tenantId + " 的工单列表 ——");
                store.getOrDefault(tenantId, Map.of()).values()
                        .forEach(t -> System.out.println("     " + t));
            }
        }

        /**
         * 程序入口 main 方法。
         */
        public static void main(String[] args) {

            // 开启 json 模式，让 POJO 抽取更可靠（严格结构输出）
            ChatModel ticketModel = OpenAiChatModel.builder()
                    .apiKey(ApiKeys.OPENAI_API_KEY)
                    .modelName(GPT_4_O_MINI)
                    .responseFormat("json_schema")
                    .strictJsonSchema(true)
                    .timeout(ofSeconds(60))
                    .build();

            // 内存版工单仓库
            TicketRepository repository = new TicketRepository();

            // 1) 面向"抽取结构化工单"的 AiService：注入上面开启了 json 模式的模型
            TicketAgent ticketAgent = AiServices.builder(TicketAgent.class)
                    .chatModel(ticketModel)
                    // 多租户记忆隔离：每个租户 id 各分配一份独立的记忆（最多 10 条消息）
                    .chatMemoryProvider(tenantId -> MessageWindowChatMemory.withMaxMessages(10))
                    .build();

            // 2) 租户 "acme" 报障：AI 自动抽取成一张结构化工单
            System.out.println("场景一：租户 acme 报障，AI 抽取结构化工单");
            String complaint = "我们线上支付模块在高峰期经常超时，很多客户付款失败，非常紧急，请帮忙处理！";
            Ticket t1 = ticketAgent.createTicket(complaint);
            // 企业落库：把这张工单"存起来"（真实场景 = 序列化 JSON 后入库/发消息）
            repository.save("acme", t1);
            System.out.println("  抽取出的工单（可序列化 JSON 入库）：" + t1);

            // 3) 租户 "globex" 也来报障，各自独立
            System.out.println("\n场景二：租户 globex 报障，AI 抽取结构化工单");
            Ticket t2 = ticketAgent.createTicket("我们登录页偶尔 500，请问能排查一下吗？");
            repository.save("globex", t2);
            System.out.println("  抽取出的工单：" + t2);

            // 4) 多租户记忆隔离验证：并入不同的 @MemoryId，AI 记住各自上下文
            System.out.println("\n场景三：多租户记忆隔离——acme 能追问自己的上下文");
            ticketAgent.chat("acme", "我是 acme 的运维，刚才的支付超时工单能加急吗？");
            // 追问：因为带了 acme 的记忆，AI 能"记得"刚才是支付超时的事
            String followUp = ticketAgent.chat("acme", "我刚才报的问题，处理优先级是多少？可以查我之前说的话吗？");
            System.out.println("  acme 追问：" + followUp);
            // 租户 globex 问"我刚才说的是什么"——它只记得自己的登录 500，不记得 acme 的支付超时
            String otherTenant = ticketAgent.chat("globex", "我刚才说的是什么问题？");
            System.out.println("  globex 问自己的上下文：" + otherTenant);

            // 5) 一句话速记输出
            System.out.println("\n场景四：一句话概括");
            String summary = ticketAgent.shortTicket("我们支付模块周五晚上 8 点开始大批量报错，客户都下不了单。");
            System.out.println("  概括：" + summary);

            // 6) 查库视角：各租户工单彼此隔离
            System.out.println("\n场景五：查看各租户工单（模拟查库）");
            repository.dump("acme");
            repository.dump("globex");
        }
    }
}
