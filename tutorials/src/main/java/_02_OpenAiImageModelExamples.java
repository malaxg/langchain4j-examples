import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static dev.langchain4j.model.openai.OpenAiImageModelName.GPT_IMAGE_1;

/**
 * 教程第 2 课：使用 OpenAI 图像生成模型（ImageModel）。
 * <p>
 * 演示如何让模型根据一段文字描述（prompt）生成图片，并把生成的图片保存成本地 PNG 文件。
 * 使用的模型是 GPT_IMAGE_1（OpenAI 的新一代图像生成模型）。
 */
public class _02_OpenAiImageModelExamples {

    /**
     * 程序入口 main 方法。
     * 会生成一张图片并保存到当前工作目录下的 swiss-developers.png 文件。
     *
     * @param args 命令行参数，本示例未使用
     * @throws IOException 写文件失败时抛出（例如磁盘无权限或路径非法）
     */
    public static void main(String[] args) throws IOException {

        // 1. 构建图像生成模型：
        //    - apiKey:    指定 OpenAI 的 API Key
        //    - modelName: 使用 GPT_IMAGE_1 图像生成模型
        ImageModel model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_IMAGE_1)
                .build();

        // 2. 调用 generate() 把"文字描述"发给模型，让它据此生成图片。
        //    Response<Image> 是 LangChain4j 的通用响应包装类，content() 可取出真正的图片对象
        Response<Image> response = model.generate(
                "一位正在吃奶酪火锅的瑞士软件开发者，旁边还有一只鹦鹉和一杯咖啡");

        // 3. 新版 GPT 图像模型返回的是 Base64 编码的图片数据（而不是一个图片 URL）
        Image image = response.content();                                   // 取出图片对象
        byte[] bytes = Base64.getDecoder().decode(image.base64Data());      // 把 Base64 字符串解码成原始二进制字节
        Path path = Files.write(Path.of("swiss-developers.png"), bytes);    // 把字节写入文件，返回文件的路径

        // 4. 打印图片保存的完整路径，方便用户查看
        System.out.println("您的图片保存在：" + path.toAbsolutePath());
    }
}
