package util.log;

/**
 * 工具类：日志级别管理。
 * 封装了本教程使用的四种日志级别（NONE / PRETTY / DEBUG / INFO）的切换逻辑。
 * PRETTY 模式会加载美化日志配置（logback-beautiful.xml），让 LLM 调用链看起来清晰整洁；
 * DEBUG 模式则输出完整的 HTTP 日志。同时这里还保存了打印内容的最大字符数限制。
 */
public class CustomLogging {
    
    // 当前日志级别，默认关闭所有日志
    private static LogLevels currentLevel = LogLevels.NONE;
    // 打印内容的最大字符数，超长会被截断
    private static int charLimit = 100;
    
    // 设置日志级别（使用默认字符数限制）
    public static void setLevel(LogLevels level) {
        currentLevel = level;
        configureLogging();
    }
    
    // 设置日志级别和自定义字符数限制
    public static void setLevel(LogLevels level, int charLimit) {
        currentLevel = level;
        CustomLogging.charLimit = charLimit;
        configureLogging();
    }
    
    // 获取当前日志级别
    public static LogLevels getLevel() {
        return currentLevel;
    }
    
    // 获取当前字符数限制
    public static int getCharLimit() {
        return charLimit;
    }
    
    // 根据当前级别设置 logback 配置文件路径
    private static void configureLogging() {
        System.setProperty("logback.statusListenerClass", "ch.qos.logback.core.status.NopStatusListener");
        
        switch (currentLevel) {
            case NONE:
                System.setProperty("logback.configurationFile", "log/logback-none.xml");
                break;
            case PRETTY:
                System.setProperty("logback.configurationFile", "log/logback-beautiful.xml");
                System.out.println("已启用美化日志 - 显示清晰的 Agent 对话"); // 原: Pretty logging enabled
                break;
            case DEBUG:
                System.setProperty("logback.configurationFile", "log/logback-full.xml");
                System.out.println("已启用调试日志 - 显示完整的 HTTP 日志"); // 原: Debug logging enabled
                break;
            case INFO:
                System.setProperty("logback.configurationFile", "log/logback-info.xml");
                System.out.println("已启用信息日志 - 显示基本信息"); // 原: Info logging enabled
                break;
        }
    }
    
    // 判断当前是否处于美化日志模式（供其它工具据此调整打印行为）
    public static boolean isPrettyLogging() {
        return currentLevel == LogLevels.PRETTY;
    }
    
    // 判断当前是否处于调试日志模式
    public static boolean isDebugLogging() {
        return currentLevel == LogLevels.DEBUG;
    }
}