package domain;

import dev.langchain4j.model.output.structured.Description;

/**
 * 实体类（领域对象）：简历评审结果。
 *
 * <p>代表一个“评审 Agent”对某份简历的评估结果，包含两部分：
 * - score：0~1 之间的评分，表示候选人被邀请面试的可能性；
 * - feedback：对简历的详细反馈（好在哪、缺什么、需要改进、危险信号等）。
 */
public class CvReview {

    // 评分：0~1，表示你邀请该候选人参加面试的可能性有多大
    @Description("从 0 到 1 评分,表示你邀请该候选人参加面试的可能性")
    public double score;

    // 反馈：哪些做得好、哪些需要改进、缺什么技能、有哪些危险信号等
    @Description("对简历的反馈:哪些好,哪些需要改进,缺哪些技能,有哪些危险信号等")
    public String feedback;

    // 无参构造器：由于存在有参构造器，必须显式提供无参构造器以支持 JSON 反序列化
    public CvReview() {}

    // 有参构造器：便于代码直接创建评审对象（如教程示例中手动构造 CvReview）
    public CvReview(double score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }

    /**
     * 覆写 toString，方便打印查看评分与反馈。
     *
     * @return 可读的评审文本
     */
    @Override
    public String toString() {
        return "\nCvReview: " +
                " - score = " + score +
                "\n- feedback = \"" + feedback + "\"\n";
    }
}
