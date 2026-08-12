package util.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类：日志解析器。
 * 负责从 LangChain4j 打印的 HTTP 请求/响应日志中提取 JSON 请求体，
 * 解析出其中的用户提问、模型回答、工具调用与工具结果，并以易读的摘要打印到控制台。
 * 注意：其中的 "user" / "assistant" / "tool" 等角色名、以及 "role" / "content" / "tool_calls"
 * 等 JSON 字段名是必须原样匹配的解析 token，不可翻译。
 */
public class LogParser {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 截断字符串：保留前后各一半，中间用省略标记替代。
     * @param input 待截断文本
     * @return 若超长则返回截断后的文本，否则原样返回
     */
    public static String truncateString(String input) {
        int maxChars = CustomLogging.getCharLimit();
        if (input == null || input.length() <= maxChars) {
            return input;
        }
        
        int firstHalf = maxChars / 2;
        int secondHalf = maxChars / 2;
        
        return input.substring(0, firstHalf) + "\n[... truncated ...]\n" + 
               input.substring(input.length() - secondHalf);
    }
    
    // 打印一条用户消息（截断后）
    public static void logUserMessage(String userMessage) {
        System.out.println("用户: " + truncateString(userMessage));
        System.out.println(); // 2 newlines for clear separation
        System.out.println();
    }
    
    // 打印一条模型回答（截断后）
    public static void logAssistantResponse(String response) {
        System.out.println("模型: " + truncateString(response));
        System.out.println(); // 2 newlines for clear separation
        System.out.println();
    }
    
    // 打印可用工具列表
    public static void logAvailableTools(String tools) {
        System.out.println("\t可用工具: " + tools);
        System.out.println(); // 2 newlines for clear separation
        System.out.println();
    }
    
    // 打印模型请求调用某个工具（含参数）
    public static void logToolCallRequest(String toolId, String toolName, String arguments) {
        System.out.println("模型请求调用工具: " + toolName + " (id: " + toolId + ")");
        System.out.println("  参数: " + truncateString(arguments));
        System.out.println(); // 2 newlines for clear separation
        System.out.println();
    }
    

    // 打印某个工具调用的返回结果
    public static void logToolCallResult(String toolId, String toolName, String result) {
        System.out.println("工具结果: " + toolName + " (id: " + toolId + ")");
        System.out.println("  结果: " + truncateString(result));
        System.out.println(); // 2 newlines for clear separation
        System.out.println();
    }
    
    /**
     * 解析 HTTP 请求日志：从 JSON 请求体中取出最新的消息（用户问题/工具结果/模型回复）来展示。
     * @param logMessage 原始的 HTTP 请求日志文本
     */
    public static void parseHttpRequest(String logMessage) {
        if (!logMessage.contains("HTTP request:") || !logMessage.contains("- body:")) {
            return;
        }
        
        try {
            String jsonBody = extractJsonFromLog(logMessage);
            if (jsonBody == null) return;
            
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode messages = root.get("messages");
            JsonNode tools = root.get("tools");
            
            if (messages == null || !messages.isArray()) return;
            
            // 取对话中最新的一条消息（即本轮新增内容）
            JsonNode lastMessage = messages.get(messages.size() - 1);
            if (lastMessage == null) return;
            
            String role = lastMessage.get("role").asText();
            
            if ("user".equals(role)) {
                // 新的一条用户提问
                String content = lastMessage.get("content").asText();
                if (content != null && !content.isEmpty()) {
                    logUserMessage(content);
                }
                
                // 当工具存在时，在用户消息之后顺便列出可用工具
                if (tools != null && tools.isArray() && tools.size() > 0) {
                    StringBuilder toolNames = new StringBuilder();
                    for (JsonNode tool : tools) {
                        if (toolNames.length() > 0) toolNames.append(", ");
                        toolNames.append(tool.get("function").get("name").asText());
                    }
                    logAvailableTools(toolNames.toString());
                }
            } else if ("tool".equals(role)) {
                // 新的一条工具执行结果
                String toolCallId = lastMessage.get("tool_call_id").asText();
                String content = lastMessage.get("content").asText();
                String toolName = extractToolNameFromHistory(messages, toolCallId);
                logToolCallResult(toolCallId, toolName, content);
            } else if ("assistant".equals(role)) {
                // 判断是否为最终回复（而非工具调用转发）
                JsonNode toolCalls = lastMessage.get("tool_calls");
                if (toolCalls == null || !toolCalls.isArray() || toolCalls.size() == 0) {
                    String content = lastMessage.get("content").asText();
                    if (content != null && !content.isEmpty()) {
                        logAssistantResponse(content);
                    }
                }
            }
            
        } catch (Exception e) {
            // 解析出错时静默忽略，不影响主流程
        }
    }
    

    // 根据 tool_call_id 在历史消息中反查对应的工具名（工具结果消息本身不含名字，需要向前查找）
    private static String extractToolNameFromHistory(JsonNode messages, String toolCallId) {
        for (JsonNode message : messages) {
            String role = message.get("role").asText();
            if ("assistant".equals(role)) {
                JsonNode toolCalls = message.get("tool_calls");
                if (toolCalls != null && toolCalls.isArray()) {
                    for (JsonNode toolCall : toolCalls) {
                        String id = toolCall.get("id").asText();
                        if (toolCallId.equals(id)) {
                            return toolCall.get("function").get("name").asText();
                        }
                    }
                }
            }
        }
        return "unknown";
    }

    /**
     * 解析 HTTP 响应日志：从返回 JSON 中提取模型的最终回答，
     * 或提取模型请求的工具调用（多个）。
     * @param logMessage 原始的 HTTP 响应日志文本
     */
    public static void parseHttpResponse(String logMessage) {
        if (!logMessage.contains("HTTP response:") || !logMessage.contains("- body:")) {
            return;
        }
        
        try {
            String jsonBody = extractJsonFromLog(logMessage);
            if (jsonBody == null) return;
            
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode choices = root.get("choices");
            
            if (choices == null || !choices.isArray() || choices.size() == 0) return;
            
            JsonNode message = choices.get(0).get("message");
            String content = message.get("content").asText();
            
            // 先检查是否存在工具调用
            JsonNode toolCalls = message.get("tool_calls");
            if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                // 这是模型请求调用工具，逐个展示
                for (JsonNode toolCall : toolCalls) {
                    String toolId = toolCall.get("id").asText();
                    String toolName = toolCall.get("function").get("name").asText();
                    String arguments = toolCall.get("function").get("arguments").asText();
                    logToolCallRequest(toolId, toolName, arguments);
                }
            } else if (content != null && !content.isEmpty()) {
                // 这是模型最终的文本回答（不带工具调用）
                logAssistantResponse(content);
            }
            
        } catch (Exception e) {
            // 解析出错时静默忽略
        }
    }
    
    // 从日志中裁剪出 "- body:" 之后到下一个空行之间的 JSON 内容
    private static String extractJsonFromLog(String logMessage) {
        // 用正则定位 "- body:" 后的 JSON 主体（直到双换行或结尾）
        Pattern pattern = Pattern.compile("- body:\\s*(.*?)(?=\\n\\n|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(logMessage);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }
    
}
