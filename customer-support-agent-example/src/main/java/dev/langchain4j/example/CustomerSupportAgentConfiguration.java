package dev.langchain4j.example;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * 客服 Agent 的 Spring 配置类。
 *
 * <p>这里集中定义了构建 Agent 所需的各个组件（Bean）：</p>
 * <ul>
 *     <li><b>ChatMemoryProvider</b>：对话记忆，让 Agent 记住历史对话（按会话ID隔离）；</li>
 *     <li><b>EmbeddingModel</b>：文本嵌入模型，把文本转换为向量；</li>
 *     <li><b>EmbeddingStore</b>：向量存储（RAG 的知识库），存有公司条款文档的向量；</li>
 *     <li><b>ContentRetriever</b>：检索器，根据用户问题从向量库检索相关资料（RAG）；</li>
 *     <li><b>TokenCountEstimator</b>：Token 数量估算器，用于计算记忆窗口的容量。</li>
 * </ul>
 *
 * <p>这些 Bean 会被 LangChain4j 的 {@code @AiService} 自动拾取并注入到 Agent 中。</p>
 */
@Configuration
public class CustomerSupportAgentConfiguration {

    /**
     * 配置"对话记忆提供者"。
     *
     * <p>每个会话（由 memoryId 标识）都有独立的记忆窗口，Agent 因此能记住上下文。
     * 这里使用"基于 Token 数量"的记忆窗口：当历史对话超过 5000 个 Token 时，
     * 最早的内容会被自动丢弃。</p>
     *
     * @param tokenizer Token 计数估算器，用于计算历史消息占用的 Token 数
     * @return 记忆提供者：根据 memoryId 返回对应的记忆实例
     */
    @Bean
    ChatMemoryProvider chatMemoryProvider(TokenCountEstimator tokenizer) {
        // memoryId -> 为每个会话单独创建一块记忆，实现多会话隔离
        return memoryId -> TokenWindowChatMemory.builder()
                .id(memoryId)                 // 给记忆设置会话ID
                .maxTokens(5000, tokenizer)   // 记忆窗口最多保留 5000 个 Token
                .build();
    }

    /**
     * 配置"文本嵌入模型"。
     *
     * <p>嵌入模型的作用：把一段文字转换成一组向量（数字序列），
     * 便于在向量库中进行相似度检索（RAG 的基础）。</p>
     *
     * <p>AllMiniLmL6V2 是本地小模型，无需联网、无需付费，
     * 精度一般但对本演示已足够。</p>
     *
     * @return 本地运行的嵌入模型实例
     */
    @Bean
    EmbeddingModel embeddingModel() {
        // 不是最好的嵌入模型，但对本演示来说已经够用
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * 配置"向量存储"（RAG 的知识库），并把公司条款文档灌入其中。
     *
     * <p>RAG 思路：先把知识文档切块、向量化后存入向量库；
     * 之后用户提问时，检索器从中找出最相关的片段交给大模型参考。</p>
     *
     * <p>真实的项目中，向量库通常已经预先灌满业务数据；这里为了演示，
     * 我们在启动时动态完成"加载文档 → 切块 → 向量化 → 入库"的完整流程。</p>
     *
     * @param embeddingModel 嵌入模型，用于把文本片段转成向量
     * @param resourceLoader 资源加载器，用于读取 classpath 下的文档
     * @param tokenizer      Token 计数估算器，用于按 Token 数切块
     * @return 灌好数据的向量存储
     * @throws IOException 文档读取失败时抛出
     */
    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel, ResourceLoader resourceLoader, TokenCountEstimator tokenizer) throws IOException {

        // 通常你已有的向量库里已经装满了自己的数据。
        // 但为了演示，我们在这里：

        // 1. 创建一个内存型向量库（重启后数据丢失，仅适合演示）
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 2. 加载示例文档（"Miles of Smiles" 租车公司的使用条款）
        //    RAG 知识来源：miles-of-smiles-terms-of-use.txt（文件名保持不变）
        Resource resource = resourceLoader.getResource("classpath:miles-of-smiles-terms-of-use.txt");
        Document document = loadDocument(resource.getFile().toPath(), new TextDocumentParser());

        // 3. 把文档切分成每段 100 个 Token 的文本片段
        // 4. 把每个片段转换成向量（嵌入）
        // 5. 把向量存入向量库
        // 以上步骤可以手动完成，但我们用 EmbeddingStoreIngestor 来自动化处理：
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 0, tokenizer);
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)  // 切分文档
                .embeddingModel(embeddingModel)      // 向量化
                .embeddingStore(embeddingStore)      // 入库
                .build();
        ingestor.ingest(document);  // 执行"切块 → 向量化 → 入库"

        return embeddingStore;
    }

    /**
     * 配置"内容检索器"（RAG 的核心组件）。
     *
     * <p>用户提问时，检索器会把问题向量化，然后到向量库中找出最相似的内容片段，
     * 作为参考资料一并交给大模型，让回答"有据可依"。</p>
     *
     * @param embeddingStore 向量库（知识库）
     * @param embeddingModel 嵌入模型，用于把查询问题转成向量
     * @return 内容检索器实例
     */
    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {

        // 你需要根据实际情况调整这两个参数，找到最佳效果，
        // 这取决于多种因素，例如：
        // - 数据的特性
        // - 使用的嵌入模型
        int maxResults = 1;    // 每次最多返回 1 条最相关的资料
        double minScore = 0.6; // 相似度低于 0.6 的资料不予返回

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)   // 指定知识库
                .embeddingModel(embeddingModel)   // 指定用于查询向量化的模型
                .maxResults(maxResults)           // 返回条数上限
                .minScore(minScore)               // 相似度阈值
                .build();
    }

    /**
     * 配置"Token 计数估算器"。
     *
     * <p>用于估算一段文本（如对话历史）会占用多少 Token，
     * 从而决定记忆窗口能保留多少内容。这里使用 OpenAI 的估算算法，
     * 与配置文件中指定的模型（gpt-4o-mini）保持一致。</p>
     *
     * @return OpenAI 风格的 Token 计数估算器
     */
    @Bean
    TokenCountEstimator tokenCountEstimator() {
        return new OpenAiTokenCountEstimator(GPT_4_O_MINI);
    }
}
