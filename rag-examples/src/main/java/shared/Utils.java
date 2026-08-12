package shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Scanner;

import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * 共享工具类：被各个 RAG 示例复用的帮助方法。
 * 包括：读取 OpenAI API Key、在控制台与助手交互、把 classpath 资源路径转换成 Path、构造 glob 匹配器等。
 */
public class Utils {

    /**
     * OpenAI API Key：优先读取环境变量 OPENAI_API_KEY，
     * 若未设置则使用默认值 "demo"（此时示例运行时会因认证失败而报错，属正常现象）。
     */
    public static final String OPENAI_API_KEY = getOrDefault(System.getenv("OPENAI_API_KEY"), "demo");

    /**
     * 在控制台启动一个与助手的多轮对话。
     * 流程：循环读取用户在控制台输入的一行问题 -> 调用 {@link Assistant#answer(String)} 得到回答 -> 打印回答。
     * 输入 "exit"（不区分大小写）即可退出对话。
     *
     * @param assistant 实现 {@link Assistant} 接口的 AI 助手
     */
    public static void startConversationWith(Assistant assistant) {
        Logger log = LoggerFactory.getLogger(Assistant.class);
        try (Scanner scanner = new Scanner(System.in)) { // try-with-resources：结束循环后自动关闭输入流
            while (true) { // 无限循环，直到用户输入 exit 才跳出
                log.info("==================================================");
                log.info("用户: "); // 提示用户输入
                String userQuery = scanner.nextLine(); // 读取用户输入的一整行文本
                log.info("==================================================");

                if ("exit".equalsIgnoreCase(userQuery)) { // 判断是否退出；equalsIgnoreCase 使 "EXIT"/"Exit" 也能生效
                    break;
                }

                String agentAnswer = assistant.answer(userQuery); // 让 AI 助手回答问题
                log.info("==================================================");
                log.info("助手: " + agentAnswer); // 打印助手的回答
            }
        }
    }

    /**
     * 构造一个 glob 模式匹配器（例如 "*.txt"），用于按文件名模式过滤文档。
     *
     * @param glob 通配符模式字符串，例如 "*.txt"
     * @return 匹配器，可用 matcher.matches(路径) 判断某个路径是否匹配该模式
     */
    public static PathMatcher glob(String glob) {
        return FileSystems.getDefault().getPathMatcher("glob:" + glob); // "glob:" 前缀是 PathMatcher 的固定语法
    }

    /**
     * 把 classpath 下的相对资源路径转换成文件系统上的 Path。
     * 例如把 "documents/miles-of-smiles-terms-of-use.txt" 转换为该资源在磁盘上的绝对路径。
     * 注意：这个方法的输入必须是 classpath 中的资源路径（也就是 resources 目录下的文件）。
     *
     * @param relativePath classpath 下的相对路径（资源路径）
     * @return 对应的绝对 Path
     * @throws RuntimeException 当资源路径无法转换为 URI 时抛出（例如资源不存在）
     */
    public static Path toPath(String relativePath) {
        try {
            URL fileUrl = Utils.class.getClassLoader().getResource(relativePath); // 通过类加载器定位 classpath 资源
            return Paths.get(fileUrl.toURI()); // 把资源 URL 转换成本地文件系统路径
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
