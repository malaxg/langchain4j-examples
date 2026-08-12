package domain;

import dev.langchain4j.model.output.structured.Description;

/**
 * 实体类（领域对象）：简历(CV)的结构化表示。
 *
 * <p>当 Agent 的返回类型是 Java 对象时，LLM 会输出符合本类字段结构的 JSON，
 * 框架再反序列化为 Cv 实例。字段上的 @Description 用于提示 LLM 每个字段应填入什么内容。
 */
public class Cv {

    // 技能列表（用逗号拼接成一段字符串）
    @Description("候选人的技能,用逗号拼接")
    private String skills;

    // 职业经历
    @Description("候选人的职业经历")
    private String professionalExperience;

    // 教育背景/学业经历
    @Description("候选人的学业经历")
    private String studies;

    /**
     * 覆写 toString，方便打印时查看对象内容。
     *
     * @return 包含 skills/professionalExperience/studies 的可读字符串
     */
    @Override
    public String toString() {
        return "CV:\n" +
                "skills = \"" + skills + "\"\n" +
                "professionalExperience = \"" + professionalExperience + "\"\n" +
                "studies = \"" + studies + "\"\n";
    }
}
