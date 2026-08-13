package embedding.model;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.output.Response;

import static java.time.Duration.ofSeconds;

/**
 * 演示调用 Hugging Face 的 Embedding 模型生成文本向量（Embedding）。
 * <p>
 * 通过 {@link HuggingFaceEmbeddingModel} 远程调用 Hugging Face 的推理端点，
 * 把一段文本转换成一串表示其语义的数字向量。
 * 注意：需要先在环境变量 HF_API_KEY 中配置 Hugging Face 的 Access Token。
 */
public class HuggingFaceEmbeddingModelExample {

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建 HuggingFace Embedding 模型 → 对文本做向量化 →
     * 打印模型返回的响应。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 构建 HuggingFace Embedding 模型
        EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(System.getenv("HF_API_KEY"))                 // 读取环境变量中的 Access Token
                .modelId("sentence-transformers/all-MiniLM-L6-v2")        // 指定要使用的模型
                .waitForModel(true)                                        // 若模型尚未就绪则等待其部署完成
                .timeout(ofSeconds(60))                                    // 设置请求超时 60 秒
                .build();

        // 把文本转成 Embedding 向量
        Response<Embedding> response = embeddingModel.embed("你好，最近怎么样？");
        System.out.println(response);
    }
}
