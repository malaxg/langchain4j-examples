import dev.langchain4j.agent.tool.*;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O;


/**
 * OpenAiFunctionCallingExamples：演示 OpenAI 的函数调用（Function Calling / Tool）能力。
 * 它允许让模型在需要的时候调用我们自己编写的 Java 方法，并把结果带回给模型生成最终回答，
 * 相当于给大模型“装上工具”。
 */
public class OpenAiFunctionCallingExamples {

    /**
     * 本示例演示如何以编程方式使用底层（Low-level）Tool API，
     * 例如 ToolSpecification（工具规范）、ToolExecutionRequest（工具执行请求）和 ToolExecutor（工具执行器）。
     * 该示例也被 LangChain4j 官方教程引用：https://docs.langchain4j.dev/tutorials/tools/#low-level-tool-api。
     * 但官方更推荐使用更高层的 API，参见：https://docs.langchain4j.dev/tutorials/tools/#high-level-tool-api
     * <p>
     * 整个过程分为 4 个步骤：
     * 1. 指定工具（WeatherTools）和用户问题（“明天伦敦的天气怎么样？”）
     * 2. 模型生成工具执行请求（模型自行决定调用哪些工具、传入什么参数）
     * 3. 用户执行工具，拿到工具返回结果（通过 ToolExecutor）
     * 4. 模型基于用户问题和工具结果，生成最终回答
     */
    static class Weather_Low_Level_Configuration {

        // 创建聊天模型（启用 strictTools：要求模型严格按 JSON Schema 输出工具参数）
        static ChatModel openAiModel = OpenAiChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_4_O)
                .strictTools(true) // 开启工具的结构化输出，参见：https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-tools
                .logRequests(true)
                .logResponses(true)
                .build();

        public static void main(String[] args) {

            // 第 1 步：用户指定工具和问题
            // 创建工具实例
            WeatherTools weatherTools = new WeatherTools();
            // 从工具类自动提取出工具规范（ToolSpecification）列表
            List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(weatherTools);
            // 构造用户问题
            List<ChatMessage> chatMessages = new ArrayList<>();
            UserMessage userMessage = userMessage("明天伦敦的天气怎么样？");
            chatMessages.add(userMessage);
            // 构造聊天请求：把消息和工具规范一起传进去
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(chatMessages)
                    .parameters(ChatRequestParameters.builder()
                            .toolSpecifications(toolSpecifications) // 告诉模型有哪些工具可用
                            .build())
                    .build();


            // 第 2 步：模型生成工具执行请求
            ChatResponse chatResponse = openAiModel.chat(chatRequest);
            AiMessage aiMessage = chatResponse.aiMessage();
            // 模型决定调用哪些工具（可能 0 个、1 个或多个）
            List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
            System.out.println("WeatherTools 中声明的 " + toolSpecifications.size() + " 个工具中，有 " + toolExecutionRequests.size() + " 个将被调用：");
            toolExecutionRequests.forEach(toolExecutionRequest -> {
                System.out.println("工具名称：" + toolExecutionRequest.name());
                System.out.println("工具参数：" + toolExecutionRequest.arguments());
            });
            // 把模型的这个“工具调用决定”追加到消息历史中
            chatMessages.add(aiMessage);


            // 第 3 步：用户（本地代码）执行工具，取得结果
            toolExecutionRequests.forEach(toolExecutionRequest -> {
                // 用默认执行器调用对应的方法
                ToolExecutor toolExecutor = new DefaultToolExecutor(weatherTools, toolExecutionRequest);
                System.out.println("现在开始执行工具 " + toolExecutionRequest.name());
                // 执行工具并传入一个执行 id（executionId，用于关联这次执行）
                String result = toolExecutor.execute(toolExecutionRequest, UUID.randomUUID().toString());
                // 把“工具执行结果”封装成消息，追加到消息历史
                ToolExecutionResultMessage toolExecutionResultMessages = ToolExecutionResultMessage.from(toolExecutionRequest, result);
                chatMessages.add(toolExecutionResultMessages);
            });


            // 第 4 步：模型基于问题和工具结果，生成最终回答
            ChatRequest chatRequest2 = ChatRequest.builder()
                    .messages(chatMessages) // 携带完整历史（含工具调用与结果）
                    .parameters(ChatRequestParameters.builder()
                            .toolSpecifications(toolSpecifications)
                            .build())
                    .build();
            ChatResponse finalChatResponse = openAiModel.chat(chatRequest2);
            // 打印最终回答（模型会综合工具结果生成一句话答案）
            System.out.println(finalChatResponse.aiMessage().text());
        }
    }

    // 天气工具类：里面每个加了 @Tool 注解的方法都会暴露给模型使用
    static class WeatherTools {

        // 获取指定城市明天的天气预报（city 是方法参数）
        @Tool("返回指定城市明天的天气预报")
        String getWeather(@P("需要返回天气预报的城市") String city) {
            return "明天 " + city + " 的天气是 25°C";
        }

        // 返回明天的日期
        @Tool("返回明天的日期")
        LocalDate getTomorrow() {
            return LocalDate.now().plusDays(1);
        }

        // 摄氏温度转华氏温度
        @Tool("把摄氏度转换成华氏度")
        double celsiusToFahrenheit(@P("需要转换成华氏度的摄氏度数值") double celsius) {
            return (celsius * 1.8) + 32;
        }

        // 注意：这个方法没有 @Tool 注解，因此不会被模型看到/调用
        String iAmNotATool() {
            return "我不是一个用 @Tool 注解标注的方法";
        }

    }
}
