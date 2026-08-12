import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;

/**
 * OpenAiEmbeddingModelExamples：演示 OpenAI 的文本向量化（Embedding）能力。
 * Embedding 会把一段文本转换成高维向量（浮点数数组），
 * 向量之间的“距离”可以衡量语义相似度，常用于检索增强（RAG）、语义搜索、推荐等场景。
 */
public class OpenAiEmbeddingModelExamples {

    public static void main(String[] args) {

        // 创建 Embedding 模型
        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(TEXT_EMBEDDING_3_SMALL) // 使用 text-embedding-3-small 模型
                .build();

        // 把文本转换为向量，返回一个 Response，其中包含向量内容
        Response<Embedding> response = model.embed("我喜欢 Java");
        Embedding embedding = response.content(); // 取出 Embedding 向量对象

        // 打印向量（一个很长的一维浮点数数组）
        System.out.println(embedding);
    }
}
