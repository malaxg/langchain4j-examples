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
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.Arrays.asList;
import static shared.Utils.*;

public class _04_Advanced_RAG_with_Metadata_Example {

    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * <p>
     * LangChain4j 中高级 RAG 的说明：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>把文档来源及元数据（Metadata）注入到 LLM 的提示词中</b>。
     * 默认情况下，注入提示词的只有检索到的文本片段本身；
     * 通过自定义内容注入器（ContentInjector），可以把片段的元数据（比如来自哪个文件）也一起告诉 LLM。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        // 问 "What is the name of the file where cancellation policy is defined?"
        // （取消政策定义在哪个文件名里？）
        // 观察 "file_name"（文件名）这个元数据是如何被注入到提示词中的。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // 加载文档
        Document document = loadDocument(toPath(documentPath), new TextDocumentParser());

        // 创建嵌入模型
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // 创建内存向量存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 摄入器：切分 -> 向量化 -> 入库
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(300, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document); // 执行摄入

        // 创建内容检索器（默认配置）
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

        // 内容注入器（本示例核心技巧）：
        // 除了文本片段本身，还把每个片段的 "file_name"（文件名）和 "index"（在原文中的序号）元数据也注入提示词，
        // 这样 LLM 就能回答"信息来自哪个文件"之类的问题。
        ContentInjector contentInjector = DefaultContentInjector.builder()
                // .promptTemplate(...) // 也可以自定义提示词的格式
                .metadataKeysToInclude(asList("file_name", "index")) // 指定要注入的元数据键
                .build();

        // 把内容注入器装配进检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(contentInjector)
                .build();

        // 创建聊天模型（开启 logRequests(true) 以便在日志里观察实际发给 LLM 的请求内容）
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .logRequests(true)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
