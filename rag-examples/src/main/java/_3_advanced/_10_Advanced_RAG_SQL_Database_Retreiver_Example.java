package _3_advanced;

import _2_naive.Naive_RAG_Example;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.h2.jdbcx.JdbcDataSource;
import shared.Assistant;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static shared.Utils.*;


public class _10_Advanced_RAG_SQL_Database_Retreiver_Example {


    /**
     * 请先参考 {@link Naive_RAG_Example} 了解基础概念。
     * <p>
     * LangChain4j 中高级 RAG 的说明：https://github.com/langchain4j/langchain4j/pull/538
     * <p>
     * 这个示例教了什么 RAG 技巧：<b>用 SQL 数据库作为内容检索源（SQL Database Content Retriever）</b>。
     * 前面的示例都是从非结构化文本（文档/网页）里检索；
     * 这个示例展示如何把结构化数据（数据库表）接入 RAG：
     * 它会让 LLM 根据用户的自然语言问题生成 SQL，去查询数据库并把结果作为"检索到的内容"。
     * <p>
     * 警告！{@link SqlDatabaseContentRetriever} 虽然有趣，但使用起来很危险！
     * 切勿在生产环境中使用它！数据库用户必须只有非常受限的只读（READ-ONLY）权限！
     * 尽管生成的 SQL 有一定校验（确保是 SELECT 语句），但并不能保证它完全无害。请自行承担风险！
     * <p>
     * 本示例使用内存型 H2 数据库，包含 3 张表：customers（客户）、products（产品）和 orders（订单）。
     * 更多细节见 "resources/sql" 目录。
     * <p>
     * 本示例需要引入 "langchain4j-experimental-sql" 依赖。
     */

    public static void main(String[] args) {

        Assistant assistant = createAssistant();

        // 你可以问诸如 "How many customers do we have?"（我们有多少客户？）
        // 或 "What is our top seller?"（最畅销的产品是什么？）这样的问题。
        startConversationWith(assistant);
    }

    private static Assistant createAssistant() {

        // 创建数据源（内存 H2 数据库）
        DataSource dataSource = createDataSource();

        // 创建聊天模型（LLM）：既用于把自然语言问题转成 SQL，也用于最终回答
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .build();

        // 内容检索器（本示例核心技巧）：SqlDatabaseContentRetriever。
        // 它拿到用户的自然语言查询 -> 让聊天模型生成 SQL -> 查询数据源 -> 把查询结果当作检索到的内容。
        ContentRetriever contentRetriever = SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource) // 指定数据库
                .chatModel(chatModel) // 指定用于生成 SQL 的 LLM
                .build();

        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private static DataSource createDataSource() {

        // 创建内存 H2 数据源
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"); // H2 内存数据库 URL
        dataSource.setUser("sa"); // 用户名
        dataSource.setPassword("sa"); // 密码

        // 读取建表脚本并执行（创建 customers/products/orders 三张表）
        String createTablesScript = read("sql/create_tables.sql");
        execute(createTablesScript, dataSource);

        // 读取预填数据脚本并执行（往表里插入示例数据）
        String prefillTablesScript = read("sql/prefill_tables.sql");
        execute(prefillTablesScript, dataSource);

        return dataSource;
    }

    private static String read(String path) {
        try {
            return new String(Files.readAllBytes(toPath(path))); // 读取整个文件内容为字符串
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void execute(String sql, DataSource dataSource) {
        // 打开连接和语句，逐条执行以 ";" 分隔的 SQL 语句
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sqlStatement : sql.split(";")) { // 按分号切分成多条 SQL
                statement.execute(sqlStatement.trim()); // 去空格后执行每一条
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
