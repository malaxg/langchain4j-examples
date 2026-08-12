package util.log;

/**
 * 枚举：日志级别。
 * - NONE：完全关闭日志
 * - PRETTY：美化日志（只显示精简的 Agent 对话摘要）
 * - DEBUG：完整调试日志（含全部 HTTP 请求/响应）
 * - INFO：基础信息日志
 * 这些枚举名会在源码中被硬编码引用，属于逻辑标识，故保留英文原名。
 */
public enum LogLevels {
    NONE,
    PRETTY,
    DEBUG,
    INFO
}