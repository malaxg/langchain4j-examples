package util;

import dev.langchain4j.agentic.scope.AgenticScope;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类：AgenticScope 美化打印器。
 * AgenticScope（智能体作用域）是 LangChain4j 中跨 Agent 共享的一块上下文区域，
 * 用于存储输入、中间结果和输出参数。本工具提供两个静态方法：
 * 1) printPretty：把 AgenticScope 的 state（状态）格式化成易读的 JSON 字符串，便于调试。
 * 2) printConversation：把一段对话文本按"User:"或"某 agent:"分行美化输出，便于观察多 Agent 调用链。
 */
public class AgenticScopePrinter {

    /**
     * 把 AgenticScope 的 state 美化打印为类 JSON 文本。
     * @param agenticScope 要打印的 AgenticScope（可为 null）
     * @param maxChars      单个值最多显示的字符数，超出部分截断
     * @return 美化后的字符串；若入参为 null 则返回 "null"
     */
    public static String printPretty(AgenticScope agenticScope, int maxChars) {
        if (agenticScope == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"memoryId\": \"").append(agenticScope.memoryId()).append("\",\n");
        sb.append("  \"state\": {\n");

        // 遍历并美化 state 中的每一项键值对
        Map<String, Object> state = agenticScope.state();
        if (state == null || state.isEmpty()) {
            sb.append("    // empty\n"); // state 为空时的占位注释
        } else {
            int count = 0;
            for (Map.Entry<String, Object> entry : state.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // 从第二项开始，每一项前加逗号分隔
                if (count > 0) {
                    sb.append(",\n");
                }

                sb.append("    \"").append(key).append("\": ");

                if (value == null) {
                    sb.append("null");
                } else {
                    String valueStr = value.toString();
                    if (valueStr.length() <= maxChars) {
                        // 转义引号/反斜杠/换行等，保证输出是合法 JSON
                        String escaped = valueStr.replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                                .replace("\t", "\\t");
                        sb.append("\"").append(escaped).append("\"");
                    } else {
                        // 超长则截断并加截断标记，避免控制台输出过载
                        String truncated = valueStr.substring(0, maxChars);
                        String escaped = truncated.replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                                .replace("\t", "\\t");
                        sb.append("\"").append(escaped).append(" [truncated...]\"");
                    }
                }
                count++;
            }
            sb.append("\n");
        }
        sb.append("  }\n");
        sb.append("}");

        return sb.toString();
    }

    /**
     * 把一段多 Agent 对话文本按角色（User / 各种 agent）分行美化输出。
     * 对话文本中通常是 "User: ..." 或 "HiringSupervisor agent: ..." 这类格式。
     * @param conversation 原始对话文本
     * @param maxChars     每行最多显示的字符数，超出截断
     * @return 美化后的多行字符串
     */
    public static String printConversation(String conversation, int maxChars) {
        if (conversation == null || conversation.isEmpty()) {
            return "(empty conversation)";
        }

        // 按 "User:" 或以 "某agent:" 结尾的行，把对话拆成若干段
        String[] parts = conversation.split("(?m)(?=^User:|^\\w+\\s+agent:)"); // <-- fixed
        StringBuilder sb = new StringBuilder();

        // 用于匹配 "xxx agent:" 前缀的正则
        Pattern agentPattern = Pattern.compile("^(\\w+)\\s+agent:(.*)$", Pattern.DOTALL);

        for (String part : parts) {
            if (part.trim().isEmpty()) continue;

            Matcher agentMatcher = agentPattern.matcher(part.trim());
            if (agentMatcher.matches()) {
                // 是某个 agent 的发言：记录 agent 类型与其内容（内容超长则截断）
                String agentType = agentMatcher.group(1);
                String content = agentMatcher.group(2).trim();

                sb.append(agentType).append(" agent:");
                if (!content.isEmpty()) {
                    if (content.length() > maxChars) {
                        sb.append(" ").append(content, 0, maxChars).append(" [truncated...]");
                    } else {
                        sb.append(" ").append(content);
                    }
                }
            } else if (part.startsWith("User:")) {
                // 是用户发言：去掉 "User:" 前缀后输出内容（超长则截断）
                String content = part.substring(5).trim();
                sb.append("User:");
                if (!content.isEmpty()) {
                    if (content.length() > maxChars) {
                        sb.append(" ").append(content, 0, maxChars).append(" [truncated...]");
                    } else {
                        sb.append(" ").append(content);
                    }
                }
            } else {
                // 其它普通文本段，直接输出（超长则截断）
                if (part.length() > maxChars) {
                    sb.append(part, 0, maxChars).append(" [truncated...]");
                } else {
                    sb.append(part);
                }
            }
            sb.append("\n\n");
        }

        return sb.toString().trim();
    }

}