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
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.OPENAI_API_KEY;
import static shared.Utils.toPath;


public class _09_Advanced_RAG_Return_Sources_Example {


    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * <p>
     * LangChain4j 中高级 RAG 的说明：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>返回来源（Return Sources，即把检索到的内容一并返回）</b>。
     * 默认 {@link AiServices} 只返回模型的回答文本；把接口的返回类型改成
     * {@link Result}，就能同时拿到模型回答和本次检索到的来源内容，
     * 方便展示"这个答案依据了哪些资料"。
     */

    /**
     * 助手接口：返回 Result&lt;String&gt;，既包含回答文本，也包含检索来源。
     */
    interface Assistant {

        Result<String> answer(String query);
    }

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        Logger log = LoggerFactory.getLogger(shared.Assistant.class);
        try (Scanner scanner = new Scanner(System.in)) { // try-with-resources 自动关闭输入流
            while (true) { // 无限循环，直到输入 exit
                log.info("==================================================");
                log.info("用户: "); // 提示用户输入
                String userQuery = scanner.nextLine(); // 读取用户输入
                log.info("==================================================");

                if ("exit".equalsIgnoreCase(userQuery)) { // 输入 exit（不区分大小写）退出
                    break;
                }

                Result<String> result = assistant.answer(userQuery); // 得到回答 + 来源
                log.info("==================================================");
                log.info("助手: " + result.content()); // 打印模型回答（content() 是回答文本）

                log.info("来源: "); // 打印检索到的来源
                List<Content> sources = result.sources(); // 取出本次检索到的所有来源内容
                sources.forEach(content -> log.info(content.toString())); // 逐个打印
            }
        }
    }

    private static Assistant createAssistant() {

        // 创建基于向量存储的内容检索器
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 创建聊天模型（LLM）
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
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
