package dev.langchain4j.example.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;

import java.io.File;
import java.util.List;

/**
 * MCP 通过 stdio（标准输入/输出）方式接入外部工具的例子。
 * <p>
 * 与 HTTP 例子不同，stdio 传输方式会启动一个本地子进程（这里是 npm 启动的
 * `server-filesystem` 服务器），并通过该进程的标准输入/输出来通信。它适合与本地的
 * MCP 服务器进程交互，例如允许 LLM 操作本机文件系统。
 * </p>
 * 配置要点（面向初学者）：
 * <ul>
 *   <li>.command(...)：指定要启动的子进程命令（这里是 npm exec 启动 filesystem 服务器）</li>
 *   <li>命令里的最后一个参数：允许该服务器访问的目录（这里是 src/main/resources）</li>
 *   <li>.logEvents(true)：打印通信相关事件日志，方便排查</li>
 * </ul>
 */
public class McpToolsExampleOverStdio {

    // 我们让 AI 读取这个文件的文本内容
    public static final String FILE_TO_BE_READ = "src/main/resources/file.txt";

    /**
     * 程序入口：演示通过 stdio 把本地 filesystem 工具接入模型，让 LLM 与本机文件系统交互。
     *
     * 该例子使用 `server-filesystem` MCP 服务器演示如何让 LLM 与本地文件系统交互。
     * <p>
     * 运行本例子需要机器上安装有 npm，因为它是通过 npm 以子进程方式启动
     * `server-filesystem` 的：`npm exec @modelcontextprotocol/server-filesystem@0.6.2`。
     * <p>
     * 当然，也可以把服务器替换成任意其他 MCP 服务器。
     * <p>
     * 与服务器的通信直接通过 stdin/stdout（标准输入/输出）进行。
     * <p>
     * 重要：执行时请确保工作目录等于项目根目录（`langchain4j-examples/mcp-example`），
     * 否则程序将找不到要读取的文件。如果在其他目录下工作，请调整 main 方法中
     * StdioMcpTransport.Builder() 里的路径。
     *
     * @param args 命令行参数（未使用）
     * @throws Exception 启动子进程、通信过程中可能抛出的异常
     */
    public static void main(String[] args) throws Exception {

        // 1) 配置聊天模型：使用 OpenAI 的 gpt-4o-mini（这里把日志打印注释掉了，保持干净输出）
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))   // 从环境变量读取 API 密钥
                .modelName("gpt-4o-mini")
//                .logRequests(true)
//                .logResponses(true)
                .build();

        // 2) 配置传输层：通过 stdio 启动本地 npm 子进程作为 MCP 服务器
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of("/usr/bin/npm", "exec",
                        "@modelcontextprotocol/server-filesystem@0.6.2",
                        // 允许该服务器交互的目录：指向本项目的资源目录，并用绝对路径
                        new File("src/main/resources").getAbsolutePath()
                ))
                .logEvents(true)
                .build();

        // 3) 基于传输层创建 MCP 客户端，管理与该子进程的会话
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        // 4) 配置工具提供者：把该 MCP 客户端（文件系统服务器）提供的工具注册进来
        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        // 5) 通过 AiServices 组装 Bot：接入聊天模型 + 文件系统工具
        Bot bot = AiServices.builder(Bot.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();

        try {
            // 让 Bot 读取指定文件的完整路径内容
            File file = new File(FILE_TO_BE_READ);
            String response = bot.chat("读取文件 " + file.getAbsolutePath() + " 的文本内容");
            System.out.println("响应内容: " + response);
        } finally {
            // 无论成功与否都关闭 MCP 客户端，终止子进程
            mcpClient.close();
        }
    }
}
