package utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * OllamaImage：关于 Ollama 的 Docker 镜像名称与本地镜像解析的工具类。
 * <p>
 * 角色/作用：集中管理 Ollama 在 Docker 中的镜像名，并提供"本地镜像判定"能力——
 * 如果本地已经存在包含某个已拉取模型的镜像，就复用本地的，避免重复下载。
 * 初学者可以把它理解成：决定"这次启动 Ollama 容器时该用哪个镜像"。
 * </p>
 */
public class OllamaImage {

    // Ollama 官方基础镜像：latest 表示最新版本，模型通过容器内命令再拉取
    public static final String OLLAMA_IMAGE = "ollama/ollama:latest";

    /**
     * 生成一个"预置了指定模型"的本地镜像名。
     * 命名规则为 "tc-<基础镜像名>-<模型名>"，例如 tc-ollama/ollama:latest-llama3.1。
     *
     * @param modelName 要在镜像里预置的模型名称（如 llama3.1）
     * @return 拼接后的本地镜像名称字符串
     */
    public static String localOllamaImage(String modelName) {
        return String.format("tc-%s-%s", OllamaImage.OLLAMA_IMAGE, modelName);
    }

    // 例子中使用的模型名称：llama3.1
    public static final String LLAMA_3_1 = "llama3.1";

    /**
     * 解析最终要使用的 Docker 镜像：如果本地已有预置了模型的镜像就复用它，
     * 否则回退到使用官方的 baseImage。
     *
     * @param baseImage      基础镜像名（如 ollama/ollama:latest）
     * @param localImageName 查找本地的镜像名（由 localOllamaImage 生成）
     * @return 决定好的 DockerImageName 对象，供容器使用
     */
    public static DockerImageName resolve(String baseImage, String localImageName) {
        DockerImageName dockerImageName = DockerImageName.parse(baseImage);
        DockerClient dockerClient = DockerClientFactory.instance().client(); // 获取 Docker 客户端
        // 列出本地所有名称与 localImageName 匹配的镜像
        List<Image> images = dockerClient.listImagesCmd().withReferenceFilter(localImageName).exec();
        if (images.isEmpty()) {
            // 本地没有对应镜像，就使用官方基础镜像（需要现场拉取模型）
            return dockerImageName;
        }
        // 本地已有该镜像：把它作为 baseImage 的兼容替代品（复用它，可省去重新下载模型）
        return DockerImageName.parse(localImageName).asCompatibleSubstituteFor(baseImage);
    }
}
