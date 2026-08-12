import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * 集中管理各示例运行时所需的 API Key（密钥）。
 * <p>
 * 为了让所有示例都能使用，通常都会从这里读取 API Key。
 * 演示用途时可以直接使用 "demo" 这个 API Key，
 * 无需注册真实账号即可体验大部分功能。
 */
public class ApiKeys {

    // 可以为了演示直接使用 "demo" 这个 API Key。
    // 你可以在这里申请自己的 OpenAI API Key：https://platform.openai.com/account/api-keys
    // 获取方式：优先读取环境变量 OPENAI_API_KEY，若未设置则回退为 "demo"。
    public static final String OPENAI_API_KEY = getOrDefault(System.getenv("OPENAI_API_KEY"), "demo");

    // 你可以在这里申请自己的 Judge0 RapidAPI Key：https://rapidapi.com/judge0-official/api/judge0-ce
    // 直接读取环境变量 RAPID_API_KEY（用于在线执行 JavaScript 的 Judge0 工具）。
    public static final String RAPID_API_KEY = System.getenv("RAPID_API_KEY");
}
