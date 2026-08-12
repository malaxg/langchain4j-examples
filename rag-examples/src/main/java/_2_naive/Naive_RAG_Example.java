package _2_naive;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;

public class Naive_RAG_Example {

    /**
     * 这个示例演示了如何实现一个"朴素 RAG"（Retrieval-Augmented Generation，检索增强生成）应用。
     * 这里的"朴素"意味着我们不会使用任何高级的 RAG 技巧。
     * 在每次与 LLM（大语言模型）的交互中，我们会：
     * 1. 直接采用用户的查询，不做任何加工。
     * 2. 使用嵌入模型（Embedding Model）把查询向量化。
     * 3. 用查询的向量在向量存储中检索（存储中保存着你文档的各个小片段），
     *    找出与查询最相关的 X 个片段。
     * 4. 把找到的片段拼接到用户的查询后面。
     * 5. 把拼接后的输入（用户查询 + 相关片段）一起发送给 LLM。
     * 6. 并期待：
     *    - 用户的查询表述良好，包含了检索所需的全部信息。
     *    - 找到的片段确实与用户的查询相关。
     */

    public static void main(String[] args) {

        // 创建一个"知道"我们文档内容的助手（AI Service）。
        // 这里加载的是虚构租车公司 "Miles of Smiles" 的使用条款文档。
        Assistant assistant = createAssistant("documents/miles-of-smiles-terms-of-use.txt");

        // 现在开始与助手对话，可以像下面这样提问：
        // - 我可以取消预订吗？
        // - 我出了事故，需要额外付费吗？
        startConversationWith(assistant);
    }

    private static Assistant createAssistant(String documentPath) {

        // 第一步：创建一个聊天模型，也就是 LLM，由它来回答我们的问题。
        // 本例使用 OpenAI 的 gpt-4o-mini，你也可以选择其他受支持的模型。
        // LangChain4j 目前支持 10 多个主流 LLM 提供商。
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();


        // 接下来，加载一个我们想要用于 RAG 的文档。
        // 我们使用虚构的租车公司 "Miles of Smiles" 的使用条款。
        // 本例只导入了一个文档，但实际上你可以加载任意多个。
        // LangChain4j 内置支持从多种来源加载文档：
        // 文件系统、URL、Amazon S3、Azure Blob Storage、GitHub、腾讯云 COS。
        // 另外，LangChain4j 还支持解析多种文档类型：文本、PDF、Word、Excel、PPT。
        // 当然，你也可以从其他来源手动导入数据。
        DocumentParser documentParser = new TextDocumentParser();
        Document document = loadDocument(toPath(documentPath), documentParser);


        // 现在我们需要把文档切分成更小的片段，也叫"块"（chunk）。
        // 这样做的目的是：面对用户问题时，只把相关的片段发送给 LLM，
        // 而不必把整篇文档都发给它。例如，如果用户询问取消预订的政策，
        // 我们就只找出并发送与取消相关的那些片段。
        // 一个好的起点是使用"递归"文档切分器：它首先尝试按段落切分；
        // 如果某个段落太大、无法装进一个片段，就递归地先按换行符切，
        // 再按句子切，最后（如有必要）按单词切，确保每一块文本都能放进一个片段。
        // 这里每块最多 300 个 token，相邻块之间重叠 0 个 token。
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);


        // 现在我们需要把这些片段"嵌入"（embed，也叫"向量化"）。
        // 向量化是做相似度检索的前提——把文本转成能表示语义的数值向量。
        // 本例使用一个本地的进程内嵌入模型，你也可以选择其他任何受支持的模型。
        // LangChain4j 目前支持 10 多个流行的嵌入模型提供商。
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();


        // 接着，我们把向量存进向量存储（也叫"向量数据库"）。
        // 在每次与 LLM 交互时，都会用这个存储来检索相关片段。
        // 为简单起见，本例使用内存型向量存储，你也可以选择任何受支持的存储。
        // LangChain4j 目前支持 15 多个流行的向量存储。
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        // 我们也可以使用 EmbeddingStoreIngestor 来把上面的手工步骤
        // （切分、向量化、入库）隐藏在一个更简单的 API 后面。
        // 关于 EmbeddingStoreIngestor 的用法，见 _01_Advanced_RAG_with_Query_Compression_Example。


        // 内容检索器（ContentRetriever）负责根据用户查询检索相关内容。
        // 目前它能够检索文本片段，未来还会扩展支持图像、音频等多模态内容。
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2) // 每次交互只检索最相关的 2 个片段
                .minScore(0.5) // 只检索与用户查询有一定相似度（得分 >= 0.5）的片段
                .build();


        // （可选）我们可以使用聊天记忆（chat memory），
        // 这样就能与 LLM 进行多轮对话，让它记住之前的交互内容。
        // 目前 LangChain4j 提供两种聊天记忆实现：
        // MessageWindowChatMemory（按消息条数）和 TokenWindowChatMemory（按 token 数量）。
        // 这里设置最多记住 10 条消息。
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);


        // 最后一步：构建我们的 AI Service，
        // 把上面创建好的各个组件（聊天模型、检索器、聊天记忆）配置进去。
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }
}
