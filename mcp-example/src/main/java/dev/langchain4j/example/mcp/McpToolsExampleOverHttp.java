package dev.langchain4j.example.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;

import java.time.Duration;
import java.util.List;

/**
 * MCP（Model Context Protocol）通过 HTTP（SSE，服务端推送事件）方式接入外部工具的例子。
 * <p>
 * 这里的核心是"传输方式"：HttpMcpTransport 表示我们通过 HTTP 与一个远程的 MCP 服务器通信，
 * 由 MCP 服务器向外暴露一个 SSE 接口。LangChain4j 会从这个服务器拉取它提供的工具（Tool），
 * 并自动注册给模型使用。本例子使用官方 `server-everything` 服务器中的 add（两数相加）工具。
 * </p>
 * 配置要点（面向初学者）：
 * <ul>
 *   <li>.sseUrl(...)：SSE 推送事件的地址，即 MCP 服务器的入口</li>
 *   <li>.timeout(...)：与服务器交互的超时时间</li>
 *   <li>.logRequests/.logResponses：打印请求与响应日志，方便排查</li>
 * </ul>
 */
public class McpToolsExampleOverHttp {

    /**
     * 程序入口：演示如何把 MCP 服务器提供的工具接入聊天模型。
     *
     * 该例子使用 `server-everything` MCP 服务器来演示 MCP 协议的一些特性，
     * 特别是用到了它的 add（两数相加）工具。
     * <p>
     * 运行前需要先在 localhost:3001 上以 SSE 模式启动 `everything` 服务器，
     * 参考 https://github.com/modelcontextprotocol/servers/tree/main/src/everything
     * 并执行 `npm install` 和 `node dist/sse.js`。
     * <p>
     * 当然，也可以把服务器替换成任意其他 MCP 服务器。
     * <p>
     * 运行例子并查看日志，即可验证模型确实使用了该工具。
     *
     * @param args 命令行参数（未使用）
     * @throws Exception 连接、调用过程中可能抛出的异常
     */
    public static void main(String[] args) throws Exception {

        // 1) 配置聊天模型：使用 OpenAI 的 gpt-4o-mini，走 OpenAI 官方 API
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))   // 从环境变量读取 API 密钥，注意不要硬编码
                .modelName("gpt-4o-mini")
                .logRequests(true)                          // 打印发出去的请求
                .logResponses(true)                         // 打印收到的响应
                .build();

        // 2) 配置传输层：通过 HTTP/SSE 连接远程 MCP 服务器（端口 3001）
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:3001/sse")        // SSE 入口地址，即 MCP 服务器暴露的接口
                .timeout(Duration.ofSeconds(60))            // 请求超时时间：60 秒
                .logRequests(true)
                .logResponses(true)
                .build();

        // 3) 基于传输层创建一个 MCP 客户端，负责管理与该服务器的会话
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        // 4) 配置工具提供者（ToolProvider）：把上面这个 MCP 客户端提供的所有工具注册进来
        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        // 5) 通过 AiServices 组装 Bot：接入聊天模型 + 工具提供者，得到"会用工具的 Bot"
        Bot bot = AiServices.builder(Bot.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();
        try {
            // 让 Bot 回答一个问题，并明确要求它使用工具来计算
            String response = bot.chat("5+12 等于多少？请使用提供的工具来回答，并始终假定工具的结果是正确的。");
            System.out.println(response);
        } finally {
            // 无论成功与否都关闭 MCP 客户端，释放连接资源
            mcpClient.close();
        }
    }
}
