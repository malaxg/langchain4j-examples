import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static java.time.Duration.ofSeconds;

/**
 * 教程第 6 课：少样本提示（Few-Shot）。
 * <p>
 * "少样本"指在正式提问之前，先在历史消息里放进几个"输入→理想输出"的示例，
 * 让模型模仿示例的格式和风格来回答。这比只靠口头要求更能教会模型"该怎么回"。
 * <p>
 * 本课模拟一个"客服机器人"场景：预先给模型几个"用户抱怨/夸奖"的示例，
 * 每个示例都约定输出格式为：
 *   Action: 客服应执行的内部动作
 *   Reply:  回复给用户的话
 * 最后把一条"真实的用户抱怨"追加进去，让模型按同样的格式给出处理方案。
 */
public class _06_FewShot {

    /**
     * 程序入口 main 方法。
     *
     * @param args 命令行参数，本示例未使用
     */
    public static void main(String[] args) {

        // 构建流式聊天模型，并设置较长的超时时间（100 秒）
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O_MINI)
                .timeout(ofSeconds(100))
                .build();

        // 创建一个消息列表，用来存放"少样本"示例（用户消息 + AI 示例回复成对出现）
        List<ChatMessage> fewShotHistory = new ArrayList<>();

        // 示例 1：正面反馈。
        // 模型学到的规则：遇到夸奖时，动作是"把内容转给正面反馈存储"，回复要热情感谢
        fewShotHistory.add(UserMessage.from(
                "我非常喜欢这次更新！界面很友好，新功能也太棒了！"));
        fewShotHistory.add(AiMessage.from(
                "Action: forward input to positive feedback storage\nReply: 非常感谢您的热情反馈！我们已把您的留言转达给产品开发团队，他们一定会非常高兴听到这些。希望您继续享受我们的产品。"));

        // 示例 2：负面反馈（安卓设备更新后频繁崩溃）。
        // 模型学到的规则：遇到 bug 类抱怨时，动作是"开新工单"，回复要诚恳致歉并承诺跟进
        fewShotHistory.add(UserMessage
                .from("这次更新之后，我的安卓设备经常崩溃。"));
        fewShotHistory.add(AiMessage.from(
                "Action: open new ticket - crash after update Android\nReply: 非常抱歉给您带来困扰。我们已把问题报告给开发团队，会尽快处理。修复完成后我们会发邮件通知您，随时欢迎您继续联系我们。"));

        // 示例 3：又一个正面反馈（夸奖应用方便日常任务）。
        fewShotHistory.add(UserMessage
                .from("你们的应用让我的日常任务轻松太多了！给团队点赞！"));
        fewShotHistory.add(AiMessage.from(
                "Action: forward input to positive feedback storage\nReply: 非常感谢您的暖心评价！很高兴我们的应用让您的日常工作更轻松。您的反馈已同步给团队，希望您继续享受我们的应用！"));

        // 示例 4：又一个负面反馈（新功能造成数据丢失）。
        fewShotHistory.add(UserMessage
                .from("这个新功能没有按预期工作，还导致了数据丢失。"));
        fewShotHistory.add(AiMessage.from(
                "Action: open new ticket - data loss by new feature\nReply: 非常抱歉给您带来不便。您的反馈对我们至关重要，我们已把问题上报给技术团队并优先处理。进展我们会及时同步，问题解决后第一时间通知您。感谢您的耐心与支持。"));

        // 5. 加入"真实的用户消息"：这条没有对应的示例回复，模型需模仿上面 4 组示例的格式来回答
        UserMessage customerComplaint = UserMessage
                .from("你们的应用怎么这么慢？快想想办法吧！");
        fewShotHistory.add(customerComplaint);

        // 打印真实用户消息，然后开始流式对话
        System.out.println("[用户]: " + customerComplaint.singleText());
        System.out.print("[AI]: ");

        // 用 CompletableFuture 桥接异步回调与同步等待
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();

        // 把整个消息列表（含少样本示例 + 真实用户消息）发给模型
        model.chat(fewShotHistory, new StreamingChatResponseHandler() {

            /**
             * 每生成一小段就打印出来（不换行，打字机效果）。
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.print(partialResponse);
            }

            /**
             * 生成完成时：让 future 正常完成。
             */
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                futureChatResponse.complete(completeResponse);
            }

            /**
             * 出错时：让 future 以异常结束。
             */
            @Override
            public void onError(Throwable error) {
                futureChatResponse.completeExceptionally(error);
            }
        });

        // 阻塞等待流式生成完成
        futureChatResponse.join();

        // 在真实业务中，接下来的流程应该是：
        // 1. 把回复内容（Reply）发给用户；
        // 2. 根据动作（Action）在后台执行相应的处理，例如开工单、存入反馈库等
    }
}
