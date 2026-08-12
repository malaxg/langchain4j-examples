import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * ApiKeys 工具类：集中管理 API Key。
 * 它从环境变量 OPENAI_API_KEY 中读取 OpenAI 的 API Key；
 * 如果环境变量未设置，则回退到默认值 "demo"。
 */
public class ApiKeys {

    // 从环境变量 OPENAI_API_KEY 读取密钥，未设置时使用默认值 "demo"
    public static final String OPENAI_API_KEY = getOrDefault(System.getenv("OPENAI_API_KEY"), "demo");
}
