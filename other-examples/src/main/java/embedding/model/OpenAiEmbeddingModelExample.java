package embedding.model;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;

/**
 * 演示调用 OpenAI 的 Embedding 模型生成文本向量（Embedding）。
 * <p>
 * 通过 {@link OpenAiEmbeddingModel} 远程调用 OpenAI 的向量化接口，
 * 把文本转换成一串语义向量。这里使用 "demo" API Key 做演示。
 */
public class OpenAiEmbeddingModelExample {

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建 OpenAI Embedding 模型 → 对文本做向量化 → 打印响应结果。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 构建 OpenAI Embedding 模型
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey("demo")                    // 演示用 API Key
                .modelName(TEXT_EMBEDDING_3_SMALL) // 指定使用的模型名
                .build();

        // 把文本转成 Embedding 向量
        Response<Embedding> response = embeddingModel.embed("Hello, how are you?");
        System.out.println(response);
    }
}
