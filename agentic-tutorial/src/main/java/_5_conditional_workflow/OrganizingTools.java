package _5_conditional_workflow;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 工具类（Tool 提供者）：组织中与面试相关的可调用工具集。
 *
 * <p>这些方法通过 @Tool 注解注册为“工具(tool)”，
 * 供 Agent 在工作流中被 LLM 按需调用（如查时间、建日程、发邮件、更新状态）。
 * 这里都是”模拟(dummy)“实现，仅用于演示工具如何被声明与调用，
 * 实际项目中应替换为真实业务逻辑。
 *
 * <p>核心概念：@Tool 让方法变成 LLM 可直接调用的“函数”；
 * @P 用来描述每个参数的语义，帮助 LLM 正确传参（参数名不翻译，值描述可翻译）。
 */
public class OrganizingTools {

    /**
     * 获取当前日期。
     *
     * @Tool 声明这是一个可被 Agent 调用的工具（无工具描述，使用默认名）。
     * @return 当前日期
     */
    @Tool
    public Date getCurrentDate(){
        return new Date();
    }

    /**
     * 根据职位描述 ID，找出需要出席现场面试的相关人员的邮箱和姓名。
     *
     * @Tool("...") 中的字符串是该工具对 LLM 的功能描述。
     * @P 描述参数 jobDescriptionId 的语义。
     *
     * @param jobDescriptionId 职位描述 ID
     * @return 相关人员的“姓名: 邮箱”列表
     */
    @Tool("根据给定的职位描述 ID,找出需要出席现场面试的相关人员的邮箱地址和姓名")
    public List<String> getInvolvedEmployeesForInterview(@P("职位描述 ID") String jobDescriptionId){
        // 演示用模拟实现
        return new ArrayList<>(List.of(
                "Anna Bolena: hiring.manager@company.com",
                "Chris Durue: near.colleague@company.com",
                "Esther Finnigan: vp@company.com"));
    }

    /**
     * 根据邮箱地址为相关人员创建日程条目。
     *
     * @Tool(...) 工具功能描述。
     *
     * @param emailAddress 员工邮箱地址列表
     * @param topic 会议主题
     * @param start 开始日期时间（格式 yyyy-mm-dd hh:mm）
     * @param end 结束日期时间（格式 yyyy-mm-dd hh:mm）
     */
    @Tool("根据邮箱地址为员工创建日程条目")
    public void createCalendarEntry(@P("员工邮箱地址列表") List<String> emailAddress, @P("会议主题") String topic, @P("开始日期时间,格式 yyyy-mm-dd hh:mm") String start, @P("结束日期时间,格式 yyyy-mm-dd hh:mm") String end){
        // 演示用模拟实现
        System.out.println("*** 已创建日程条目 CALENDAR ENTRY CREATED ***");
        System.out.println("主题 Topic: " + topic);
        System.out.println("开始 Start: " + start);
        System.out.println("结束 End: " + end);
    }

    /**
     * 发送一封邮件。
     *
     * @Tool 无字符串参数的版本使用默认描述；@P 描述各参数语义。
     *
     * @param to 收件人邮箱地址列表
     * @param cc 抄送邮箱地址列表
     * @param subject 邮件主题
     * @param body 邮件正文
     * @return 模拟的邮件 ID（供后续逻辑使用）
     */
    @Tool
    public int sendEmail(@P("收件人邮箱地址列表") List<String> to, @P("抄送邮箱地址列表") List<String> cc, @P("邮件主题") String subject, @P("正文") String body){
        // 演示用模拟实现
        System.out.println("*** 已发送邮件 EMAIL SENT ***");
        System.out.println("收件人 To: " + to);
        System.out.println("抄送 Cc: " + cc);
        System.out.println("主题 Subject: " + subject);
        System.out.println("正文 Body: " + body);
        return 1234; // 模拟邮件 ID
    }

    /**
     * 更新候选人申请状态。
     *
     * @Tool 默认描述；@P 描述各参数语义。
     *
     * @param jobDescriptionId 职位描述 ID
     * @param candidateName 候选人姓名（名+姓）
     * @param newStatus 新的申请状态
     */
    @Tool
    public void updateApplicationStatus(@P("职位描述 ID") String jobDescriptionId, @P("候选人姓名(名+姓)") String candidateName, @P("新的申请状态") String newStatus){
        // 演示用模拟实现
        System.out.println("*** 已更新申请状态 APPLICATION STATUS UPDATED ***");
        System.out.println("职位描述 ID: " + jobDescriptionId);
        System.out.println("候选人姓名: " + candidateName);
        System.out.println("新状态: " + newStatus);
    }
}
