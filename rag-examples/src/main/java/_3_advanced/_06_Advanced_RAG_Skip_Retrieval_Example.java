package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import shared.Assistant;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static shared.Utils.*;


public class _06_Advanced_RAG_Skip_Retrieval_Example {


    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * <p>
     * LangChain4j 中高级 RAG 的说明：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>按条件跳过检索（Skip Retrieval）</b>。
     * 有时候检索是不必要的。例如用户只是打了个招呼 "Hi"，
     * 这种情况下没必要去向量库里检索文档，直接基于 LLM 本身的通用知识回复即可。
     * <p>
     * 实现方式有很多种，最简单的是使用自定义的 {@link QueryRouter}。
     * 当需要跳过检索时，QueryRouter 返回空列表，表示该查询不会路由到任何 {@link ContentRetriever}。
     * <p>
     * 判断"是否需要检索"的方式可以多种多样：
     * - 基于规则（例如根据用户的权限、地理位置等）。
     * - 基于关键词（例如查询中是否包含某些特定词）。
     * - 基于语义相似度（参见本仓库中的 EmbeddingModelTextClassifierExample）。
     * - 用 LLM 来做判断。
     * <p>
     * 本示例采用最后一种：让 LLM 来判断用户查询是否需要进行检索。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        // 先输入 "Hi"
        // 注意：这个查询不会路由到任何检索器（因为 LLM 判断它与公司业务无关）。

        // 再问 "Can I cancel my reservation?"（我可以取消预订吗？）
        // 这个查询会被路由到我们的检索器。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // 创建嵌入模型
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        // 构建向量存储（embed 帮助方法：切分、向量化、入库）
        EmbeddingStore<TextSegment> embeddingStore =
                embed(toPath("documents/miles-of-smiles-terms-of-use.txt"), embeddingModel);

        // 创建内容检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        // 创建聊天模型（LLM），既用于"是否需要检索"的判断，也用于最终回答
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 创建自定义查询路由器（本示例核心技巧）：
        // 我们实现 QueryRouter 接口，先让 LLM 判断"这个问题是否与租车公司业务相关"，
        // 若回答含 "no" 就返回空列表（跳过检索），否则把问题路由到内容检索器。
        QueryRouter queryRouter = new QueryRouter() {

            // 提示词模板：让 LLM 判断查询是否与租车公司业务相关。
            // 注意：必须要求 LLM 用英文 token yes/no/maybe 回答，
            // 因为下方逻辑用 contains("no") 来判断，改成中文会被破坏。{{it}} 会被替换成实际的用户查询。
            private final PromptTemplate PROMPT_TEMPLATE = PromptTemplate.from(
                    "Is the following query related to the business of the car rental company? " + // 判断下面的查询是否与租车公司的业务相关
                            "Answer only with a single English word: 'yes', 'no' or 'maybe'. " + // 只用一个英文单词回答：yes、no 或 maybe
                            "Query: {{it}}" // 查询内容：{{it}}
            );

            @Override
            public Collection<ContentRetriever> route(Query query) {

                // 用模板生成提示词（把占位符 {{it}} 替换为用户查询文本）
                Prompt prompt = PROMPT_TEMPLATE.apply(query.text());

                // 把提示词作为用户消息发送给 LLM，得到判断结果
                AiMessage aiMessage = chatModel.chat(prompt.toUserMessage()).aiMessage();
                System.out.println("LLM decided: " + aiMessage.text()); // 打印 LLM 的决策结果

                if (aiMessage.text().toLowerCase().contains("no")) { // 如果 LLM 认为不相关（no）
                    return emptyList(); // 返回空列表 = 不路由到任何检索器 = 跳过检索
                }

                // 否则（yes / maybe），把查询路由到内容检索器去检索
                return singletonList(contentRetriever);
            }
        };

        // 把查询路由器装配进检索增强器
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
        List<TextSegment> segments = splitter.split(document); // 得到片段列表

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content(); // 向量化所有片段

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>(); // 内存向量存储
        embeddingStore.addAll(embeddings, segments); // 向量和片段一起入库
        return embeddingStore; // 返回向量存储
    }
}
