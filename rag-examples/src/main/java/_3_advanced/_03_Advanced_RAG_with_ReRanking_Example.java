package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class _03_Advanced_RAG_with_ReRanking_Example {

    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * <p>
     * LangChain4j 中高级 RAG 的说明：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>重排序（Re-Ranking）</b>。
     * <p>
     * 通常，检索器（{@link ContentRetriever}）检索回来的结果并不全都与用户查询真正相关。
     * 因为在第一阶段检索时，面对海量数据，我们往往倾向使用更快、更便宜的模型，
     * 代价就是检索质量可能较低。
     * 如果把不相关的信息发给 LLM，既浪费成本，最坏情况下还会导致幻觉（hallucination）。
     * 所以，在第二阶段，我们可以用更先进的模型（例如 Cohere Rerank）对第一阶段的结果进行重排序，
     * 并过滤掉不相关的结果。
     * <p>
     * 本示例需要引入 "langchain4j-cohere" 依赖。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        // 先输入 "Hi"。观察第一阶段检索到的所有片段是如何被重排模型过滤掉的。
        // 再问 "Can I cancel my reservation?"，观察除一个片段外其他片段是如何被过滤掉的。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // 加载文档（用纯文本解析器解析）
        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        // 创建嵌入模型（本地 BGE 小模型）
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // 创建内存型向量存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 摄入器：自动完成 切分 -> 向量化 -> 入库
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document); // 执行摄入

        // 第一阶段检索器：先"粗召回"，多取一些结果（maxResults=5），
        // 为第二阶段的重排序留下足够的候选。
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5) // 多取一些结果（先多召回，再精排）
                .build();

        // 注册并获取 Cohere 的免费 API Key，请访问：https://dashboard.cohere.com/welcome/register
        ScoringModel scoringModel = CohereScoringModel.builder()
                .apiKey(System.getenv("COHERE_API_KEY")) // 从环境变量 COHERE_API_KEY 读取 Key
                .modelName("rerank-multilingual-v3.0") // 使用 Cohere 的多语言重排模型
                .build();

        // 内容聚合器（本示例的核心技巧）：对第一阶段检索到的结果进行重排序，
        // 只把与用户查询真正相关（得分 >= 0.8）的片段交给 LLM，过滤掉不相关的内容。
        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                .minScore(0.8) // 只保留真正相关的片段
                .build();

        // 把"第一阶段检索器 + 重排聚合器"装配进检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentAggregator(contentAggregator)
                .build();

        // 创建聊天模型（LLM），用于最终回答
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(model)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
