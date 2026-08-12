package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.builder.sql.LanguageModelSqlFilterBuilder;
import dev.langchain4j.store.embedding.filter.builder.sql.TableDefinition;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import shared.Assistant;
import shared.Utils;

import java.util.function.Function;

import static dev.langchain4j.data.document.Metadata.metadata;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import static org.assertj.core.api.Assertions.assertThat;

class _05_Advanced_RAG_with_Metadata_Filtering_Examples {

    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * 关于元数据过滤的更多信息：https://github.com/langchain4j/langchain4j/pull/610
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>元数据过滤（Metadata Filtering）</b>。
     * 在向量检索时，除了按语义相似度，还可以用元数据（如 动物种类、用户ID、电影类型）来过滤候选片段，
     * 缩小检索范围，让结果更精准。本示例展示三种方式：
     * 1. 静态过滤（Static Filter）：固定不变的过滤条件。
     * 2. 动态过滤（Dynamic Filter）：根据运行时参数（如当前用户）动态生成的过滤条件。
     * 3. 由 LLM 生成过滤条件（LLM-generated Filter）：把元数据描述成 SQL 表，让 LLM 根据自然语言查询生成过滤条件。
     */

    // 共享的聊天模型（LLM）：分别用于多轮对话
    ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey(Utils.OPENAI_API_KEY)
            .modelName(GPT_4_O_MINI)
            .build();

    // 共享的嵌入模型（把文本向量化）
    EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

    @Test
    void Static_Metadata_Filter_Example() {

        // 准备数据（given）：创建两个片段并各自带元数据。
        // 元数据键都是 "animal"，值分别是 "dog"（狗）和 "bird"（鸟）。
        TextSegment dogsSegment = TextSegment.from("Article about dogs ...", metadata("animal", "dog"));
        TextSegment birdsSegment = TextSegment.from("Article about birds ...", metadata("animal", "bird"));

        // 把两个片段向量化后存入向量存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModel.embed(dogsSegment).content(), dogsSegment);
        embeddingStore.add(embeddingModel.embed(birdsSegment).content(), birdsSegment);
        // 现在 embeddingStore 里同时包含关于狗和鸟的片段

        // 创建一个静态过滤条件：animal 等于 "dog"（只检索关于狗的片段）
        Filter onlyDogs = metadataKey("animal").isEqualTo("dog");

        // 把过滤条件配置到检索器里（静态过滤，本示例核心技巧）
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .filter(onlyDogs) // 指定静态过滤条件，把检索范围限制为只关于狗的片段
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // 触发（when）：让助手回答
        String answer = assistant.answer("Which animal?");

        // 断言（then）：回答里应提到 dog，不应提到 bird
        assertThat(answer)
                .containsIgnoringCase("dog")
                .doesNotContainIgnoringCase("bird");
    }


    /**
     * 个性化助手接口：每个用户有独立的聊天记忆。
     * 通过 @MemoryId 注解为用户指定记忆 ID；
     * 这个 ID 也会出现在 Query 的元数据里，供动态过滤使用。
     */
    interface PersonalizedAssistant {

        // chatMessage 是用户消息文本；userId 标记当前用户（每个用户有独立记忆）
        String chat(@MemoryId String userId, @dev.langchain4j.service.UserMessage String userMessage);
    }

    @Test
    void Dynamic_Metadata_Filter_Example() {

        // 准备数据（given）：
        // 用户1 的喜好（元数据 userId=1）
        TextSegment user1Info = TextSegment.from("My favorite color is green", metadata("userId", "1"));
        // 用户2 的喜好（元数据 userId=2）
        TextSegment user2Info = TextSegment.from("My favorite color is red", metadata("userId", "2"));

        // 存入向量存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModel.embed(user1Info).content(), user1Info);
        embeddingStore.add(embeddingModel.embed(user2Info).content(), user2Info);
        // 现在向量存储里同时包含用户1 和用户2 的信息

        // 动态过滤条件（本示例核心技巧）：
        // 输入是当前的 Query，输出是一个 Filter。
        // 这里根据"当前对话的记忆 ID"（即 userId）来过滤，只检索属于当前用户的信息。
        Function<Query, Filter> filterByUserId =
                (query) -> metadataKey("userId").isEqualTo(query.metadata().chatMemoryId().toString());

        // 把动态过滤函数配置到检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                // 指定动态过滤：把检索范围限制为只属于当前用户（@MemoryId 指定的用户）的片段
                .dynamicFilter(filterByUserId)
                .build();

        // 构建个性化助手（带记忆 ID）
        PersonalizedAssistant personalizedAssistant = AiServices.builder(PersonalizedAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // 触发（when）：以用户1 的身份提问
        String answer1 = personalizedAssistant.chat("1", "Which color would be best for a dress?");

        // 断言（then）：只检索到用户1 的信息（绿色），不应出现用户2 的红色
        assertThat(answer1)
                .containsIgnoringCase("green")
                .doesNotContainIgnoringCase("red");

        // 触发（when）：以用户2 的身份提问
        String answer2 = personalizedAssistant.chat("2", "Which color would be best for a dress?");

        // 断言（then）：只检索到用户2 的信息（红色），不应出现用户1 的绿色
        assertThat(answer2)
                .containsIgnoringCase("red")
                .doesNotContainIgnoringCase("green");
    }

    @Test
    void LLM_generated_Metadata_Filter_Example() {

        // 准备数据（given）：三部电影的片段，各有 genre（电影类型）和 year（上映年份）元数据
        TextSegment forrestGump = TextSegment.from("Forrest Gump", metadata("genre", "drama").put("year", 1994));
        TextSegment groundhogDay = TextSegment.from("Groundhog Day", metadata("genre", "comedy").put("year", 1993));
        TextSegment dieHard = TextSegment.from("Die Hard", metadata("genre", "action").put("year", 1998));

        // 把元数据描述成一张 SQL 表的列（本示例核心技巧）：
        // LanguageModelSqlFilterBuilder 需要知道元数据的"结构"，就像描述数据库表一样。
        // 这样 LLM 就能根据自然语言查询生成对应的过滤条件。
        TableDefinition tableDefinition = TableDefinition.builder()
                .name("movies") // 表名（逻辑上的，仅用于描述）
                .addColumn("genre", "VARCHAR", "one of: [comedy, drama, action]") // genre 列：取值之一是 喜剧/剧情/动作
                .addColumn("year", "INT") // year 列：整数
                .build();

        // 创建"由 LLM 生成 SQL 过滤条件"的构建器，
        // 它会把用户查询 + 表结构描述交给 LLM，让 LLM 生成一个 Filter。
        LanguageModelSqlFilterBuilder sqlFilterBuilder = new LanguageModelSqlFilterBuilder(chatModel, tableDefinition);

        // 把三部电影片段存入向量存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.add(embeddingModel.embed(forrestGump).content(), forrestGump);
        embeddingStore.add(embeddingModel.embed(groundhogDay).content(), groundhogDay);
        embeddingStore.add(embeddingModel.embed(dieHard).content(), dieHard);

        // 把"由 LLM 动态生成过滤条件"配置到检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .dynamicFilter(query -> sqlFilterBuilder.build(query)) // LLM 会根据查询动态生成过滤条件
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .build();

        // 触发（when）：请助手推荐一部 90 年代的剧情片（drama）。
        // 注意：forrestGump 是 1994 年、drama 类型，应当被检索到。
        String answer = assistant.answer("Recommend me a good drama from 90s");

        // 断言（then）：应推荐 Forrest Gump（剧情片、90年代），而不应推荐喜剧（Groundhog Day）和动作片（Die Hard）
        assertThat(answer)
                .containsIgnoringCase("Forrest Gump")
                .doesNotContainIgnoringCase("Groundhog Day")
                .doesNotContainIgnoringCase("Die Hard");
    }
}
