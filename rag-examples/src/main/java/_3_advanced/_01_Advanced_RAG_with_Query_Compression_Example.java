package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
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
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class _01_Advanced_RAG_with_Query_Compression_Example {

    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * <p>
     * LangChain4j 中高级 RAG 的说明：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>查询压缩（Query Compression）</b>。
     * 本示例演示了如何使用"查询压缩"技术实现更高级的 RAG 应用。
     * 很多时候，用户提出的查询是"追问题"，会引用对话前文，本身缺少检索所需的全部细节。
     * 例如下面这段对话：
     * 用户：约翰·多伊留下了什么遗产？
     * AI：约翰·多伊是……
     * 用户：他是什么时候出生的？
     * <p>
     * 在这种场景下，如果使用基础 RAG，直接拿 "When was he born?" 这类查询去检索，
     * 往往无法找到关于约翰·多伊的文章，因为查询里根本没有"约翰·多伊"这个关键词。
     * 查询压缩的做法是：把用户查询和之前的对话一起交给 LLM，
     * 让 LLM 把它们"压缩"成一个独立的、自包含的查询，
     * 例如生成 "When was John Doe born?"（约翰·多伊是什么时候出生的？）。
     * 这个方法会带来一点额外的延迟和成本，但能显著提升 RAG 的检索质量。
     * 另外要注意：用于压缩的 LLM 不一定要和用于对话的 LLM 相同，
     * 例如你可以用一个更小的、擅长摘要的本地模型来做压缩。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant("documents/biography-of-john-doe.txt");

        // 先问 "What is the legacy of John Doe?"（约翰·多伊留下了什么遗产？）
        // 再问 "When was he born?"（他是什么时候出生的？）
        // 然后查看日志：
        // - 第一个查询没有被压缩，因为前面没有任何上下文可以压缩。
        // - 第二个查询会被压缩成类似 "When was John Doe born?" 的形式，检索质量因此提升。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // 加载文档（用纯文本解析器解析）
        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        // 创建嵌入模型（本地 BGE 小模型，用于把文本转成向量）
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // 创建内存型向量存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 创建"摄入器"（EmbeddingStoreIngestor）：把 切分文档 -> 生成向量 -> 存入向量存储 三步封装为一个 API。
        // 这里使用递归切分器：每块约 300 个 token，重叠 0。
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // 执行摄入：文档被自动切分、向量化并存入向量存储
        ingestor.ingest(document);

        // 创建聊天模型（LLM），用于最终回答用户的对话
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 创建 CompressingQueryTransformer（查询压缩器）：
        // 它负责把"用户查询 + 之前的对话"交给 LLM，压缩成一个独立、自包含的查询，
        // 这能显著提升后续检索的质量（本示例的核心技巧）。
        QueryTransformer queryTransformer = new CompressingQueryTransformer(chatModel);

        // 创建内容检索器：根据（压缩后的）查询，从向量存储中检索最相关的片段
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2) // 每次检索 2 个最相关片段
                .minScore(0.6) // 要求相似度不低于 0.6
                .build();

        // RetrievalAugmentor（检索增强器）是 LangChain4j 中 RAG 流程的入口。
        // 你可以通过配置它来自定义 RAG 行为，把各个 RAG 组件装配在一起。
        // 在后续的示例中，我们会看到更多自定义方式。
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryTransformer(queryTransformer) // 装配查询压缩器
                .contentRetriever(contentRetriever) // 装配内容检索器
                .build();

        // 构建 AI Service，把检索增强器和聊天记忆装配进去
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
