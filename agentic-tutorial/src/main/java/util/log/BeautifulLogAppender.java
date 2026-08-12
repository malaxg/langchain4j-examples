package util.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * 工具类：Logback 自定义 Appender（日志输出器）。
 * 用于拦截并美化 LLM 的 HTTP 请求/响应日志：把原始冗长的 HTTP 日志交给 LogParser 解析成易读的
 * "用户问题 / 模型回答 / 工具调用" 等摘要；同时过滤掉一堆无关的噪音日志（okhttp、fusion、logback 自身配置等）。
 * 该 Appender 会在 logback-beautiful.xml（PRETTY 模式）中注册。
 */
public class BeautifulLogAppender extends AppenderBase<ILoggingEvent> {
    
    // 本次实现的缓冲字段暂未使用，保留字段以兼容结构（批量收集 HTTP 请求/响应时可用）
    private static final StringBuilder httpRequestBuffer = new StringBuilder();
    private static final StringBuilder httpResponseBuffer = new StringBuilder();
    private static boolean inHttpRequest = false;
    private static boolean inHttpResponse = false;
    
    // 每条日志事件都会进入这里
    @Override
    protected void append(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        String loggerName = event.getLoggerName();
        
        // 处理来自 HTTP 客户端的日志：解析请求/响应体
        if (loggerName.contains("LoggingHttpClient")) {
            if (message.contains("HTTP request:")) {
                LogParser.parseHttpRequest(message);
            } else if (message.contains("HTTP response:")) {
                LogParser.parseHttpResponse(message);
            }
            return;
        }
        
        // 过滤掉已知的噪音日志源，避免污染控制台输出
        if (loggerName.contains("okhttp3") ||
            loggerName.contains("com.fasterxml.jackson") ||
            loggerName.contains("ai.djl") ||
            loggerName.contains("org.apache.tika") ||
            loggerName.contains("ch.qos.logback") ||
            loggerName.contains("ch.qos.logback.classic.LoggerContext") ||
            loggerName.contains("ch.qos.logback.classic.util.ContextInitializer") ||
            loggerName.contains("ch.qos.logback.core.model.processor") ||
            loggerName.contains("ch.qos.logback.classic.joran") ||
            loggerName.contains("ch.qos.logback.classic.util.DefaultJoranConfigurator") ||
            loggerName.contains("ch.qos.logback.classic.util.SerializedModelConfigurator") ||
            loggerName.contains("ch.qos.logback.classic.util.ContextInitializer") ||
            message.contains("logback-classic version") ||
            message.contains("No custom configurators were discovered") ||
            message.contains("Trying to configure with") ||
            message.contains("Constructed configurator") ||
            message.contains("Could NOT find resource") ||
            message.contains("Found resource") ||
            message.contains("Processing appender") ||
            message.contains("About to instantiate appender") ||
            message.contains("Ignoring unknown property") ||
            message.contains("Setting level of") ||
            message.contains("Attaching appender") ||
            message.contains("End of configuration") ||
            message.contains("Registering current configuration") ||
            message.contains("call lasted") ||
            message.contains("ExecutionStatus")) {
            return;
        }
        
        // 其余日志一律原样输出，便于调试时看到完整信息
        System.out.println("UNFILTERED LOG: [" + event.getLevel() + "] [" + loggerName + "] " + message);
    }
}

