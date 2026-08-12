import dev.langchain4j.data.audio.Audio;
import dev.langchain4j.model.audio.AudioTranscriptionModel;
import dev.langchain4j.model.audio.AudioTranscriptionRequest;
import dev.langchain4j.model.audio.AudioTranscriptionResponse;
import dev.langchain4j.model.openai.OpenAiAudioTranscriptionModel;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.langchain4j.model.openai.OpenAiAudioTranscriptionModelName.WHISPER_1;

/**
 * OpenAiAudioModelExamples：演示 OpenAI 的音频转写（语音转文字）能力。
 * 使用 Whisper 模型把一段音频文件转成文字。
 */
public class OpenAiAudioModelExamples {

    public static void main(String[] args) {
        // 创建音频转写模型
        AudioTranscriptionModel model = OpenAiAudioTranscriptionModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                .modelName(WHISPER_1) // 使用 Whisper-1 语音模型
                .logRequests(true)    // 打印请求日志
                .logResponses(true)   // 打印响应日志
                .build();

        // 构造转写请求：需要提供音频内容（MIME 类型 + 二进制数据）
        AudioTranscriptionResponse response = model.transcribe(AudioTranscriptionRequest.builder()
                .audio(Audio.builder()
                        .mimeType("audio/wav") // MIME 类型（必填）
                        .binaryData(toBytes("audio.wav")) // 音频的二进制数据，从资源文件读取
                        .build())
                .build());

        // 打印转写出的文字
        System.out.println(response.text());
    }

    // 从 classpath 资源中读取文件，返回其字节数组
    private static byte[] toBytes(String fileName) {
        try {
            URL fileUrl = OpenAiAudioModelExamples.class.getResource(fileName); // 定位资源文件
            return Files.readAllBytes(Path.of(fileUrl.toURI())); // 读取全部字节
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
