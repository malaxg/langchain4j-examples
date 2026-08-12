package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import shared.Assistant;

import java.nio.file.Path;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;


public class _08_Advanced_RAG_Web_Search_Example {


    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * <p>
     * LangChain4j 中高级 RAG 的说明：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>把搜索引擎（Web Search）作为额外的内容检索器</b>。
     * 向量检索只覆盖你本地入库的文档；当问题涉及私有文档之外的实时信息时，
     * 可以再挂一个网页搜索检索器（如 Tavily），
     * 并用 {@link DefaultQueryRouter} 把查询同时路由给"本地向量检索器"和"网页搜索检索器"。
     * <p>
     * 本示例需要引入 "langchain4j-web-search-engine-tavily" 依赖。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // 第一个检索器：基于本地向量存储的检索器
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        ContentRetriever embeddingStoreContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 第二个检索器：网页搜索检索器（本示例核心技巧）。
        // 先用 Tavily 搜索引擎（网页搜索 API）建一个 WebSearchEngine，
        // 再包一层 WebSearchContentRetriever，让它能作为 RAG 的检索器使用。
        WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
                .apiKey(System.getenv("TAVILY_API_KEY")) // 免费 Key：https://app.tavily.com/sign-in
                .build();

        ContentRetriever webSearchContentRetriever = WebSearchContentRetriever.builder()
                .webSearchEngine(webSearchEngine)
                .maxResults(3) // 每个查询取 3 条网页搜索结果
                .build();

        // 查询路由器：把每个查询同时路由给两个检索器（本地向量 + 网页搜索）
        QueryRouter queryRouter = new DefaultQueryRouter(embeddingStoreContentRetriever, webSearchContentRetriever);

        // 装配进检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
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

    private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
        DocumentParser documentParser = new TextDocumentParser(); // 文本解析器
        Document document = loadDocument(documentPath, documentParser); // 加载文档

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0); // 递归切分
        List<TextSegment> segments = splitter.split(document); // 得到片段列表

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content(); // 向量化

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>(); // 内存向量存储
        embeddingStore.addAll(embeddings, segments); // 入库
        return embeddingStore; // 返回向量存储
    }
}
