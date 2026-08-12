package _1_easy;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class Easy_RAG_Example {

    // 聊天模型（LLM）：整个应用共享一个，负责回答用户的问题。
    // 本例使用 OpenAI 的 gpt-4o-mini。
    private static final ChatModel CHAT_MODEL = OpenAiChatModel.builder()
            .apiKey(OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .build();

    /**
     * 这个示例演示了如何实现一个 "Easy RAG"（检索增强生成）应用。
     * 这里的 "Easy" 意味着我们不会深入讲解文档解析、切分、向量化等底层细节，
     * 所有 "魔法" 都被隐藏在 "langchain4j-easy-rag" 模块中。
     * <p>
     * 如果你想了解如何在没有 "Easy RAG" 魔法的情况下亲手实现 RAG，
     * 请参考 {@link Naive_RAG_Example}（朴素 RAG 示例）。
     */

    public static void main(String[] args) {

        // 第一步：加载我们想要用于 RAG 的文档。
        // loadDocuments 会从文件系统中加载所有名字匹配 "*.txt" 的文档。
        List<Document> documents = loadDocuments(toPath("documents/"), glob("*.txt"));

        // 第二步：创建一个能访问我们文档的助手（AI Service）。
        // AiServices.builder 会用代理/反射自动为 Assistant 接口生成实现。
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(CHAT_MODEL) // 让它使用 OpenAI 的 LLM 来回答问题
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // 让它记住最近 10 条消息
                .contentRetriever(createContentRetriever(documents)) // 让它能访问到我们的文档
                .build();

        // 最后：与助手开始对话。你可以像下面这样提问：
        // - 我可以取消预订吗？
        // - 我出了事故，需要额外付费吗？
        startConversationWith(assistant);
    }

    private static ContentRetriever createContentRetriever(List<Document> documents) {

        // 创建一个空的"内存向量存储"，用来存放文档及其向量表示（Embedding）。
        // InMemoryEmbeddingStore 表示数据只保存在内存中（进程退出后丢失），适合学习和演示。
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 把文档"摄入"（ingest）到向量存储中。
        // 底层会自动完成：切分文档 -> 把片段向量化 -> 把向量和片段存入存储。
        // 这一系列"魔法"现在可以先忽略细节，Easy RAG 模块已经帮我们做好了。
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        // 最后，从向量存储创建一个内容检索器（ContentRetriever）。
        // 它的作用：收到用户问题后，在向量存储中找出与问题最相关的文档片段。
        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }
}
