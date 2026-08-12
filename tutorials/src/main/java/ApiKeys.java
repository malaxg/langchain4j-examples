import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * 本类集中管理教程中所有示例用到的 API Key（密钥）。
 * <p>
 * 它的作用是：把读取环境变量的逻辑统一放在一处，这样各个示例类只需写
 * {@code ApiKeys.OPENAI_API_KEY} 就能拿到 OpenAI 的密钥，不用各自重复读取环境变量。
 * <p>
 * 使用前请先在你的操作系统中设置环境变量，例如：{@code OPENAI_API_KEY=sk-xxxx}。
 */
public class ApiKeys {

    /**
     * OpenAI 的 API Key。
     * 从环境变量 OPENAI_API_KEY 中读取；如果没设置，就回退（fallback）成字符串 "demo"，
     * 这样示例代码在不配密钥时也能跑起来（但只会得到模拟响应）。
     */
    public static final String OPENAI_API_KEY = getOrDefault(System.getenv("OPENAI_API_KEY"), "demo");

    /**
     * RapidAPI 的 API Key（仅用于 _11_ServiceWithDynamicToolsExample 调用 Judge0 在线代码执行服务）。
     * 这里没有提供默认值，如果环境变量 RAPID_API_KEY 未设置则为 null。
     */
    public static final String RAPID_API_KEY = System.getenv("RAPID_API_KEY");
}
