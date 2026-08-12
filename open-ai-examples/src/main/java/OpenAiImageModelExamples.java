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
 * OpenAiImageModelExamples：演示 OpenAI 的图像生成能力。
 * 传入一段文字描述，模型生成对应的图片；
 * 新版的 GPT 图像模型会把生成结果以 base64 编码的数据返回（而不是图片 URL）。
 */
public class OpenAiImageModelExamples {

    public static void main(String[] args) throws IOException {

        // 创建图像生成模型
        ImageModel model = OpenAiImageModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(GPT_IMAGE_1) // 使用 gpt-image-1 图像模型
                .quality("low")         // 生成质量：low / medium / high
                .build();

        // 根据文字提示生成图片，返回 Response<Image>
        Response<Image> response = model.generate("唐老鸭在纽约，卡通风格");

        // 新版 GPT 图像模型返回的是 base64 编码的数据（而不是图片 URL）
        Image image = response.content(); // 取出 Image 对象
        byte[] bytes = Base64.getDecoder().decode(image.base64Data()); // 把 base64 解码成字节数组
        Path path = Files.write(Path.of("donald-duck.png"), bytes); // 把字节写入当前目录的 png 文件

        // 打印生成图片的绝对路径
        System.out.println("唐老鸭图片已生成，路径：" + path.toAbsolutePath()); // :)
    }
}
