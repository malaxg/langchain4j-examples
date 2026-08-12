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
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;


public class _07_Advanced_RAG_Multiple_Retrievers_Example {


    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * <p>
     * LangChain4j 中高级 RAG 的说明：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>同时使用多个内容检索器（Multiple Content Retrievers）</b>。
     * 当数据分散在多个来源时，你可以为每个来源各建一个检索器，
     * 再用 {@link DefaultQueryRouter} 把每个查询同时路由到所有这些检索器，
     * 这样一次回答就能综合多个来源的信息。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // 创建嵌入模型
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // 创建第一个内容检索器：基于"使用条款"文档
        EmbeddingStore<TextSegment> embeddingStore1 =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);
        ContentRetriever contentRetriever1 = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore1)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 创建第二个内容检索器：基于"约翰·多伊传记"文档
        EmbeddingStore<TextSegment> embeddingStore2 =
                embed(toPath("documents/biography-of-john-doe.txt"), embeddingModel);
        ContentRetriever contentRetriever2 = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore2)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 创建查询路由器（本示例核心技巧）：
        // 使用 DefaultQueryRouter，会把每个查询同时路由到两个检索器，
        // 从而综合来自两份文档的信息。
        QueryRouter queryRouter = new DefaultQueryRouter(contentRetriever1, contentRetriever2);

        // 把查询路由器装配进检索增强器
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
