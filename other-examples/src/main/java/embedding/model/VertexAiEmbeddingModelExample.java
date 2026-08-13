package embedding.model;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;

/**
 * 演示调用 Google Cloud Vertex AI 的 Embedding 模型生成文本向量。
 * <p>
 * 通过 {@link VertexAiEmbeddingModel} 调用 Google Vertex AI 的
 * textembedding-gecko 模型，把文本转换成语义向量。
 * 使用时需要配置好 Google Cloud 相应认证信息。
 */
public class VertexAiEmbeddingModelExample {

    /**
     * 程序入口主方法。
     * <p>
     * 流程：配置端点、项目、区域等信息构建模型 → 对文本向量化 → 打印响应。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 构建 Vertex AI Embedding 模型
        EmbeddingModel embeddingModel = VertexAiEmbeddingModel.builder()
                .endpoint("us-central1-aiplatform.googleapis.com:443") // 推理端点地址
                .project("langchain4j")                                // GCP 项目 ID
                .location("us-central1")                               // 区域
                .publisher("google")                                   // 发布者
                .modelName("textembedding-gecko@001")                  // 使用的模型名
                .build();

        // 把文本转成 Embedding 向量
        Response<Embedding> response = embeddingModel.embed("你好，最近怎么样？");
        System.out.println(response);
    }
}
