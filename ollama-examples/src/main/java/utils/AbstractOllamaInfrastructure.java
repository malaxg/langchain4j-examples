package utils;

import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static utils.OllamaImage.LLAMA_3_1;
import static utils.OllamaImage.localOllamaImage;

/**
 * AbstractOllamaInfrastructure：Ollama 相关例子的公共基础设施（基类）。
 * <p>
 * 角色/作用：统一处理"Ollama 从哪来"的问题。提供了两种使用方式：
 * <ul>
 *   <li>① 已设置环境变量 OLLAMA_BASE_URL（例如本机已装 Ollama）：直接连本机已有的 Ollama，无需 Docker</li>
 *   <li>② 未设置环境变量：用 Testcontainers 自动下载并启动一个官方 Ollama Docker 容器，并预装模型</li>
 * </ul>
 * 初学者需要重点理解：OLLAMA_BASE_URL 就是 Ollama 服务的宿主地址（如 http://localhost:11434）。
 * </p>
 */
public class AbstractOllamaInfrastructure {

    // Ollama 服务的基础地址；来自环境变量 OLLAMA_BASE_URL
    public static final String OLLAMA_BASE_URL = System.getenv("OLLAMA_BASE_URL");
    // 例子统一使用的模型名称：llama3.1
    public static final String MODEL_NAME = LLAMA_3_1;

    // 保存 Ollama 容器实例（仅在走 Docker 方案时非空）
    public static LangChain4jOllamaContainer ollama;

    // 静态初始化块：类第一次被加载时执行一次
    static {
        if (isNullOrEmpty(OLLAMA_BASE_URL)) {
            // 没有设置外部 Ollama 地址 → 使用 Testcontainers 启动 Docker 容器
            String localOllamaImage = localOllamaImage(MODEL_NAME); // 生成"预置模型"的本地镜像名
            // 创建容器并指定要预装的模型；resolve 会优先复用本地已有的镜像
            ollama = new LangChain4jOllamaContainer(OllamaImage.resolve(OllamaImage.OLLAMA_IMAGE, localOllamaImage))
                    .withModel(MODEL_NAME);
            ollama.start();                    // 启动容器（首次会下载镜像与模型，耗时较长）
            ollama.commitToImage(localOllamaImage); // 把带模型的容器提交成镜像，下次启动可复用
        }
    }

    /**
     * 获取最终要连接使用的 Ollama 服务地址。
     *
     * @param ollama 已启动的 Ollama 容器实例
     * @return 若有外部地址则返回该地址，否则返回容器自动分配的端点（宿主地址）
     */
    public static String ollamaBaseUrl(LangChain4jOllamaContainer ollama) {
        if (isNullOrEmpty(OLLAMA_BASE_URL)) {
            // 走 Docker 方案：使用容器暴露的端点地址
            return ollama.getEndpoint();
        } else {
            // 走外部已装 Ollama 方案：使用环境变量里的宿主地址（如 http://localhost:11434）
            return OLLAMA_BASE_URL;
        }
    }
}
