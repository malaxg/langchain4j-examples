package _5_conditional_workflow;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;


import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

/**
 * 工具/提供者类：加载并构建一个小型 RAG 内容检索器 (ContentRetriever)。
 *
 * <p>用途：把公司的“内部规章(house_rules)”文档加载、分块、向量化后存入内存向量库，
 * 返回一个检索器，可挂到 Agent 上（见 _5a 中的 .contentRetriever(...)）。
 * 这样 LLM 回答问题时能依据检索到的内部规则，避免凭空猜测。
 *
 * <p>核心概念（RAG — 检索增强生成）：
 * - 文档加载 → 文档切分(DocumentSplitter) → 向量化(EmbeddingModel) → 存入向量库(EmbeddingStore)
 * - 查询时根据语义相似度从向量库检索相关片段，作为上下文提供给 LLM。
 */
public class RagProvider {

    /**
     * 构建并返回“公司内部规章”的内容检索器。
     *
     * @return EmbeddingStoreContentRetriever：基于内存向量库的内容检索器，
     *         查询时最多返回 2 条且相似度不低于 0.8 的片段。
     */
    public static ContentRetriever loadHouseRulesRetriever() {
        Document doc = loadDocument(toPath("documents/house_rules.txt")); // 1. 加载文档
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel(); // 2. 向量化模型（把文本转成向量）
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>(); // 3. 内存向量库（存储切块后的向量）

        // 4. 构建入库器：负责“切分 + 向量化 + 存入向量库”
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(200, 10)) // 文档切分：每块约 200 字符、重叠 10
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build();

        ingestor.ingest(List.of(doc)); // 5. 执行入库（加载文档进向量库）

        // 6. 返回检索器：查询时从向量库中找最相似的片段
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(2)   // 最多返回 2 条相关片段
                .minScore(0.8)   // 相似度阈值：低于 0.8 的片段不返回
                .build();
    }

    /**
     * 把 classpath 下的相对路径转换为文件系统中的绝对 Path。
     *
     * @param relativePath 相对路径（如 documents/house_rules.txt）
     * @return 对应的 Path
     */
    public static Path toPath(String relativePath) {
        try {
            URL fileUrl = Utils.class.getClassLoader().getResource(relativePath); // 通过类加载器定位资源
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
