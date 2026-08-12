package utils;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

/**
 * LangChain4jOllamaContainer：对 Testcontainers 的 OllamaContainer 做了一层"自动拉取模型"的封装。
 * <p>
 * 角色/作用：Testcontainers 启动的 Ollama 容器默认不含任何模型。本类在容器启动后，
 * 自动在容器内执行 `ollama pull <model>` 命令，把指定的模型下载好，这样后续就能直接使用。
 * 初学者可以把它理解成"能自动预装模型的 Ollama 容器"。
 * </p>
 */
public class LangChain4jOllamaContainer extends OllamaContainer {

    // 日志记录器，用于输出拉取模型的进度信息
    private static final Logger log = LoggerFactory.getLogger(LangChain4jOllamaContainer.class);

    // 要预装进容器的模型名称（可为 null，表示不预装）
    private String model;

    /**
     * 构造函数：基于指定的 Docker 镜像创建容器。
     *
     * @param dockerImageName 要使用的 Ollama Docker 镜像
     */
    public LangChain4jOllamaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    /**
     * 设置要在容器内预装（pull）的模型名称。
     *
     * @param model 模型名称（如 llama3.1）
     * @return 返回当前对象本身，方便链式调用
     */
    public LangChain4jOllamaContainer withModel(String model) {
        this.model = model;
        return this;
    }

    /**
     * 容器启动完成时自动回调：如果指定了模型，就在容器内执行 ollama pull 下载该模型。
     * 注意：这里刻意保留了 Docker 相关的英文日志，仅做说明。
     *
     * @param containerInfo 容器启动后的信息（含容器状态）
     */
    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if (this.model != null) {
            try {
                // Start pulling the 'xx' model ... would take several minutes ...（开始拉取模型，耗时较长）
                log.info("Start pulling the '{}' model ... would take several minutes ...", this.model);
                // 在容器内执行 "ollama pull <model>" 命令下载模型
                ExecResult r = execInContainer("ollama", "pull", this.model);
                log.info("Model pulling competed! {}", r); // 模型拉取完成日志
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error pulling model", e); // 拉取失败则抛出运行时异常
            }
        }
    }
}
