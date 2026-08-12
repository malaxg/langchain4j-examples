package dev.langchain4j.example.mcp;

/**
 * Bot 接口：面向 AI 服务（AiServices）的声明式契约接口。
 * <p>
 * 它的角色是定义一个"聊天机器人"抽象：只要实现该方法，框架就会自动把
 * 聊天模型（ChatModel）和工具（Tool）注入进来，实现"智能"的对话行为。
 * 初学者可以把它理解成"告诉框架我想要一个能聊天的 Bot，它只有一个方法 chat(String prompt)"。
 * </p>
 */
public interface Bot {

    /**
     * 让聊天模型根据给定的提示词（prompt）生成回答。
     *
     * @param prompt 用户的输入提示词，即用户说的一句话
     * @return 模型生成的回答文本
     */
    String chat(String prompt);
}
