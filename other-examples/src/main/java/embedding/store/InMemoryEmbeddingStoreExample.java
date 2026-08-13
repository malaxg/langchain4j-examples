package embedding.store;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

/**
 * 演示使用内存版 Embedding 存储（InMemoryEmbeddingStore）进行语义检索。
 * <p>
 * 流程：先把若干文本片段转成 Embedding 向量并存入内存库，
 * 再对一条查询文本向量化后做相似度检索，返回最相关的结果。
 * 该存储基于余弦相似度匹配，纯内存运行，适合演示与轻量场景。
 */
public class InMemoryEmbeddingStoreExample {

    /**
     * 程序入口主方法。
     * <p>
     * 流程：创建内存 Embedding 存储 → 创建 Embedding 模型 →
     * 把两条文本片段向量化并入库 → 用一条查询做相似度检索 → 打印最匹配结果。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {

        // 创建内存版 Embedding 存储（泛型参数为存储的文档类型 TextSegment）
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 创建本地（进程内）Embedding 模型：负责把文本转成向量
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // 文档 1：先包装成 TextSegment，再向量化，最后连同内容存入内存库
        TextSegment segment1 = TextSegment.from("我喜欢足球。");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        embeddingStore.add(embedding1, segment1);

        // 文档 2：同样流程
        TextSegment segment2 = TextSegment.from("今天天气很好。");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);

        // 构造查询：把查询文本向量化，并搜索库中与其最相似的片段
        Embedding queryEmbedding = embeddingModel.embed("你最喜欢的运动是什么？").content();
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding) // 查询向量
                .maxResults(1)                  // 最多返回 1 条结果
                .build();
        // 执行相似度检索，取最匹配的一条
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();
        EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);

        System.out.println(embeddingMatch.score()); // 0.8144288515898701（相似度得分，越高越相关）
        System.out.println(embeddingMatch.embedded().text()); // 我喜欢足球。（最匹配的文档内容）

        // 内存版 Embedding 存储支持序列化/反序列化为 JSON
        // String serializedStore = embeddingStore.serializeToJson();
        // InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromJson(serializedStore);

        // 也支持序列化/反序列化为文件
        // String filePath = "/home/me/embedding.store";
        // embeddingStore.serializeToFile(filePath);
        // InMemoryEmbeddingStore<TextSegment> deserializedStore = InMemoryEmbeddingStore.fromFile(filePath);
    }
}
