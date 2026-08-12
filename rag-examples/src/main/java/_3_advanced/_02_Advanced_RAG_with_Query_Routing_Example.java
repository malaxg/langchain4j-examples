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
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class _02_Advanced_RAG_with_Query_Routing_Example {

    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * <p>
     * LangChain4j 中高级 RAG 的说明：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>查询路由（Query Routing）</b>。
     * <p>
     * 现实中的私有数据往往分散在多个来源、多种格式中，
     * 例如 Confluence 里的公司内部文档、Git 仓库里的项目代码、
     * 存有用户数据的关系型数据库、售卖产品的搜索引擎等等。
     * 当 RAG 流程使用多个数据源时，你通常会有多个 {@link EmbeddingStore} 或 {@link ContentRetriever}。
     * 虽然你可以把每个用户查询都路由到所有 {@link ContentRetriever}，
     * 但这种做法既低效，有时还会适得其反。
     * <p>
     * "查询路由"正是解决这个问题的方法：把查询定向到最合适的（一个或多个）{@link ContentRetriever}。
     * 路由可以用多种方式实现：
     * - 基于规则（例如根据用户的权限、地理位置等）。
     * - 基于关键词（例如查询中包含 X1、X2、X3 就路由到 {@link ContentRetriever} X 等）。
     * - 基于语义相似度（参见本仓库中的 EmbeddingModelTextClassifierExample）。
     * - 用 LLM 来做路由决策。
     * <p>
     * 对于第 1、2、3 种场景，你可以实现一个自定义的 {@link QueryRouter}。
     * 第 4 种场景，本示例会演示如何使用 {@link LanguageModelQueryRouter}。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        // 先问 "What is the legacy of John Doe?"（约翰·多伊留下了什么遗产？）
        // 再问 "Can I cancel my reservation?"（我可以取消预订吗？）
        // 然后观察日志，看这两个查询是如何被路由到不同的检索器的。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // 创建嵌入模型（本地 BGE 模型，把文本向量化）
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // 为"传记"文档单独创建一个向量存储（embed 帮助方法负责：切分、向量化、入库）
        EmbeddingStore<TextSegment> biographyEmbeddingStore =
                embed(toPath("documents/biography-of-john-doe.txt"), embeddingModel);
        // 从该向量存储创建第一个内容检索器
        ContentRetriever biographyContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(biographyEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 同样，为"使用条款"文档单独创建一个向量存储
        EmbeddingStore<TextSegment> termsOfUseEmbeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);
        // 创建第二个内容检索器
        ContentRetriever termsOfUseContentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(termsOfUseEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 创建聊天模型（LLM），既用于路由决策，也用于最终回答
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 创建查询路由器（本示例的核心技巧）。
        // 每个检索器配一段文字描述；LanguageModelQueryRouter 会把"查询 + 这些描述"一起发给 LLM，
        // 由 LLM 决定把当前查询路由到哪个（或哪些）检索器。
        // 注意：描述文字用英文，是为了让 LLM 在路由时更容易匹配到对应的检索器。
        Map<ContentRetriever, String> retrieverToDescription = new HashMap<>();
        retrieverToDescription.put(biographyContentRetriever, "biography of John Doe"); // 描述：约翰·多伊的传记
        retrieverToDescription.put(termsOfUseContentRetriever, "terms of use of car rental company"); // 描述：租车公司的使用条款
        QueryRouter queryRouter = new LanguageModelQueryRouter(chatModel, retrieverToDescription);

        // 把查询路由器装配进检索增强器（这里没有显式设置 contentRetriever，
        // 因为具体用哪个检索器完全由 QueryRouter 决定）
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private static EmbeddingStore<TextSegment> embed(Path documentPath, EmbeddingModel embeddingModel) {
        DocumentParser documentParser = new TextDocumentParser(); // 文本解析器
        Document document = loadDocument(documentPath, documentParser); // 加载文档

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0); // 递归切分：每块 300 token，重叠 0
        List<TextSegment> segments = splitter.split(document); // 得到文档片段列表

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content(); // 把所有片段向量化

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>(); // 内存向量存储
        embeddingStore.addAll(embeddings, segments); // 把向量和片段一起存入存储
        return embeddingStore; // 返回向量存储
    }
}
