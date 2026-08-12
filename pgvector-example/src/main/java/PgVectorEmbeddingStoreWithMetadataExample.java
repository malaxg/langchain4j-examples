
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 在 pgvector 向量存储中结合"元数据过滤"（Metadata Filter）进行检索的例子。
 * <p>
 * 角色/作用：在基础向量入库之上，给每个文本片段（TextSegment）附加元数据
 * （例如属于哪个用户 userId），随后在检索时用过滤器（Filter）只搜索满足某个
 * 元数据条件的结果。这常用于"按用户隔离数据"等多租户场景。
 * </p>
 * 配置要点（面向初学者）：
 * <ul>
 *   <li>Metadata.metadata(...)：为文本片段附加键值对形式的元数据</li>
 *   <li>metadataKey("userId").isEqualTo("1")：构造过滤条件，只匹配 userId 为 "1" 的记录</li>
 *   <li>.filter(...)：把过滤条件传入检索请求，实现带条件的向量搜索</li>
 * </ul>
 */
public class PgVectorEmbeddingStoreWithMetadataExample {

    /**
     * 程序入口：演示带元数据过滤的向量入库与检索流程。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {

        // 指定要启动的 Docker 镜像：pgvector 数据库容器
        DockerImageName dockerImageName = DockerImageName.parse("pgvector/pgvector:pg16");
        // try-with-resources：程序结束自动关闭容器
        try (PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(dockerImageName)) {
            postgreSQLContainer.start(); // 启动数据库

            // 1) 创建嵌入模型：把文本转成向量
            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            // 2) 配置 pgvector 向量存储，连接 Testcontainers 启动的数据库容器
            EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                    .host(postgreSQLContainer.getHost())               // 主机地址
                    .port(postgreSQLContainer.getFirstMappedPort())    // 端口
                    .database(postgreSQLContainer.getDatabaseName())   // 数据库名
                    .user(postgreSQLContainer.getUsername())           // 用户名
                    .password(postgreSQLContainer.getPassword())       // 密码
                    .table("test")                                     // 表名
                    .dimension(embeddingModel.dimension())             // 向量维度
                    .build();

            // 3) 向量入库：两段文本都附带 userId 元数据（分别属于用户 1 和用户 2）
            TextSegment segment1 = TextSegment.from("我喜欢足球。", Metadata.metadata("userId", "1"));
            Embedding embedding1 = embeddingModel.embed(segment1).content();
            embeddingStore.add(embedding1, segment1); // 带元数据入库

            TextSegment segment2 = TextSegment.from("我喜欢篮球。", Metadata.metadata("userId", "2"));
            Embedding embedding2 = embeddingModel.embed(segment2).content();
            embeddingStore.add(embedding2, segment2);

            // 查询向量：用户提问也转成向量
            Embedding queryEmbedding = embeddingModel.embed("你最喜欢的运动是什么？").content();

            // ===== 检索用户 1 的数据 =====

            // 构造过滤条件：只保留 userId 为 "1" 的记录
            Filter onlyForUser1 = metadataKey("userId").isEqualTo("1");

            EmbeddingSearchRequest embeddingSearchRequest1 = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding) // 查询向量
                    .filter(onlyForUser1)           // 加上用户 1 的过滤条件
                    .build();

            // 执行搜索：在"用户 1 的数据"中查找最相关片段
            EmbeddingSearchResult<TextSegment> embeddingSearchResult1 = embeddingStore.search(embeddingSearchRequest1);
            EmbeddingMatch<TextSegment> embeddingMatch1 = embeddingSearchResult1.matches().get(0); // 取最相关的一条

            System.out.println(embeddingMatch1.score());            // 打印相似度得分
            System.out.println(embeddingMatch1.embedded().text());  // 打印命中的文本片段

            // ===== 检索用户 2 的数据 =====

            // 构造过滤条件：只保留 userId 为 "2" 的记录
            Filter onlyForUser2 = metadataKey("userId").isEqualTo("2");

            EmbeddingSearchRequest embeddingSearchRequest2 = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding) // 查询向量
                    .filter(onlyForUser2)           // 加上用户 2 的过滤条件
                    .build();

            // 执行搜索：在"用户 2 的数据"中查找最相关片段
            EmbeddingSearchResult<TextSegment> embeddingSearchResult2 = embeddingStore.search(embeddingSearchRequest2);
            EmbeddingMatch<TextSegment> embeddingMatch2 = embeddingSearchResult2.matches().get(0);

            System.out.println(embeddingMatch2.score());            // 打印相似度得分
            System.out.println(embeddingMatch2.embedded().text());  // 打印命中的文本片段

            // 停止容器
            postgreSQLContainer.stop();
        }
    }
}
