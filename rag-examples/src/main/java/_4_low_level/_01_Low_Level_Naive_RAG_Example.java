package _4_low_level;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.util.stream.Collectors.joining;
import static shared.Utils.OPENAI_API_KEY;
import static shared.Utils.toPath;

public class _01_Low_Level_Naive_RAG_Example {

    /**
     * 这个示例演示如何使用 <b>低层（底层）LangChain4j API</b> 来手动实现一个朴素 RAG。
     * 前面 _1_easy、_2_naive、_3_advanced 用的是高层 API（AI Services，由框架自动完成检索等步骤）；
     * 这里一步一步手写：加载文档、切分、向量化、入库、嵌入问题、相似度搜索、拼装提示词、调用 LLM，
     * 让你看到 RAG 的完整流程到底是怎么运作的。
     * 请对照其他包来理解高层 API 与低层 API 的区别。
     */

    public static void main(String[] args) {

        // 第一步：加载包含你想与模型"聊天"内容的文档。
        DocumentParser documentParser = new TextDocumentParser();
        Document document = loadDocument(toPath("example-files/story-about-happy-carrot.txt"), documentParser);

        // 第二步：把文档切分成片段。
        // 使用递归切分器，并传入 OpenAiTokenCountEstimator 作为 token 计数器，
        // 因为它基于 OpenAI 的分词规则来统计 token，切分更准确。
        DocumentSplitter splitter = DocumentSplitters.recursive(
                300, // 每块最大 300 个 token
                0, // 块与块之间不重叠
                new OpenAiTokenCountEstimator(GPT_4_O_MINI) // 用 OpenAI 分词器估算 token 数
        );
        List<TextSegment> segments = splitter.split(document);

        // 第三步：用嵌入模型把每个片段"向量化"（转成能表示语义的向量）。
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // 第四步：把向量和片段一起存进向量存储，供后续检索使用。
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);

        // 第五步：指定你要问的问题。
        String question = "Who is Charlie?";

        // 第六步：把问题也向量化（嵌入）。
        Embedding questionEmbedding = embeddingModel.embed(question).content();

        // 第七步：在向量存储中，按"语义相似度"找出与问题最相关的片段（低层检索）。
        // 下面几个参数你可以按自己的场景调优，找到一个合适的平衡点。
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding) // 用问题的向量去搜
                .maxResults(3) // 最多返回 3 条最相关结果
                .minScore(0.7) // 相似度得分不低于 0.7
                .build();
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = embeddingStore.search(embeddingSearchRequest).matches();

        // 第八步：为模型构造一个提示词，把"问题"和"检索到的相关内容"一起装入。
        // {{question}} 和 {{information}} 是占位符，下面会用实际值替换。
        PromptTemplate promptTemplate = PromptTemplate.from(
                "Answer the following question to the best of your ability:\n" // 请尽你所能回答下面的问题
                        + "\n"
                        + "Question:\n" // 问题
                        + "{{question}}\n"
                        + "\n"
                        + "Base your answer on the following information:\n" // 请基于以下信息来回答
                        + "{{information}}");

        // 把检索到的片段文本拼接起来（用空行分隔），作为提示词里的"信息"部分。
        String information = relevantEmbeddings.stream()
                .map(match -> match.embedded().text()) // 取出每个匹配片段的原文
                .collect(joining("\n\n"));

        // 准备替换占位符的变量：question=用户问题，information=拼接后的相关片段。
        Map<String, Object> variables = new HashMap<>();
        variables.put("question", question);
        variables.put("information", information);

        // 应用模板（把占位符替换成实际值），得到一个完整的提示词。
        Prompt prompt = promptTemplate.apply(variables);

        // 第九步：把提示词作为"用户消息"发送给 OpenAI 聊天模型。
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .timeout(Duration.ofSeconds(60)) // 设置请求超时时间 60 秒
                .build();
        AiMessage aiMessage = chatModel.chat(prompt.toUserMessage()).aiMessage();

        // 第十步：取出并打印模型的回答。
        String answer = aiMessage.text();
        System.out.println(answer); // Charlie is a cheerful carrot living in VeggieVille...（查理是一个住在蔬菜镇的快乐胡萝卜……）
    }
}
