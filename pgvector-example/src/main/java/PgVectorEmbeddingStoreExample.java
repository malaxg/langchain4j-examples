import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * 使用 pgvector 数据库作为向量存储（EmbeddingStore）的基础例子。
 * <p>
 * 角色/作用：演示如何把文本用嵌入模型（EmbeddingModel）转成向量，
 * 存入支持 pgvector 扩展的 PostgreSQL 数据库中，再根据相似度进行检索（向量搜索）。
 * 这是 RAG（检索增强生成）中"向量入库"环节的最小可运行示例。
 * </p>
 * 配置要点（面向初学者）：
 * <ul>
 *   <li>Testcontainers 会自动启动一个 pgvector 的 Docker 容器作为数据库</li>
 *   <li>PgVectorEmbeddingStore 需要表格名和向量维度等配置</li>
 *   <li>.dimension(...)：向量的维度，必须与嵌入模型输出的维度一致</li>
 * </ul>
 */
public class PgVectorEmbeddingStoreExample {

    /**
     * 程序入口：演示文本向量化 → 入库 → 相似度检索的完整流程。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {

        // 指定要启动的 Docker 镜像：pgvector（带向量扩展的 PostgreSQL 数据库）
        DockerImageName dockerImageName = DockerImageName.parse("pgvector/pgvector:pg16");
        // try-with-resources：程序结束时会自动关闭容器
        try (PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(dockerImageName)) {
            postgreSQLContainer.start(); // 启动并等待数据库就绪

            // 1) 创建嵌入模型：把文本转成向量。这里使用本地 ONNX 模型 AllMiniLmL6V2，无需联网
            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            // 2) 配置 pgvector 向量存储：把 Testcontainers 启动的数据库连接信息填入
            EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                    .host(postgreSQLContainer.getHost())               // 数据库主机地址
                    .port(postgreSQLContainer.getFirstMappedPort())    // 映射到宿主机的端口
                    .database(postgreSQLContainer.getDatabaseName())   // 数据库名
                    .user(postgreSQLContainer.getUsername())           // 用户名
                    .password(postgreSQLContainer.getPassword())       // 密码
                    .table("test")                                     // 要操作的数据库表名
                    .dimension(embeddingModel.dimension())             // 向量维度，与模型输出一致
                    .build();

            // 3) 向量入库：把两段文本转成向量并存入数据库
            TextSegment segment1 = TextSegment.from("我喜欢足球。");
            Embedding embedding1 = embeddingModel.embed(segment1).content(); // 文本 → 向量
            embeddingStore.add(embedding1, segment1);                        // 向量 + 文本 一起入库

            TextSegment segment2 = TextSegment.from("今天天气很好。");
            Embedding embedding2 = embeddingModel.embed(segment2).content();
            embeddingStore.add(embedding2, segment2);

            // 4) 构造查询向量：把用户问题也转成向量，用于相似度比较
            Embedding queryEmbedding = embeddingModel.embed("你最喜欢的运动是什么？").content();

            // 5) 发起检索请求：只取与查询向量最相似（得分最高）的 1 条结果
            EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)   // 查询向量
                    .maxResults(1)                    // 最多返回 1 条结果
                    .build();

            // 6) 执行检索，得到与问题最相关的文本片段列表
            List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(embeddingSearchRequest).matches();

            // 最相关（得分最高）的片段排在最前面
            EmbeddingMatch<TextSegment> embeddingMatch = relevant.get(0);

            // 打印相似度得分与文本，得分越高代表与查询越相关
            System.out.println(embeddingMatch.score()); // 0.8144288608390052（相似度，示意）
            System.out.println(embeddingMatch.embedded().text()); // 我喜欢足球。（最相关的片段）

            // 手动停止容器（可省略，try-with-resources 也会处理）
            postgreSQLContainer.stop();
        }
    }
}
