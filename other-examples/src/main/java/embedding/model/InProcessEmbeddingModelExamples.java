package embedding.model;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.store.embedding.CosineSimilarity;

import java.io.IOException;

/**
 * 演示在 Java 进程内（In-Process）离线进行文本向量化的两种方式。
 * <p>
 * 1. 使用官方预打包（Pre-Packaged）的本机模型 all-MiniLM-L6-v2，
 *    无需联网即可生成 Embedding；
 * 2. 使用自定义的自定义模型（Custom），从本地加载 ONNX 模型文件
 *    并指定 PoolingMode（池化方式）来生成向量。
 */
public class InProcessEmbeddingModelExamples {

    /**
     * 示例：使用预打包的进程内 Embedding 模型。
     * <p>
     * 演示完全在 Java 进程内、不联网地完成文本向量化。
     */
    static class Pre_Packaged_In_Process_Embedding_Model_Example {

        public static void main(String[] args) throws IOException {

            String text = "让我们来演示，embedding 可以在 Java 进程内完全离线完成。";

            // 需要引入 "langchain4j-embeddings-all-minilm-l6-v2" 依赖（Maven/Gradle），详见 pom.xml
            // 创建本地（进程内）Embedding 模型：模型文件会随依赖一起提供，无需联网
            EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            // 对文本进行向量化，取出 Embedding 向量内容
            Embedding inProcessEmbedding = embeddingModel.embed(text).content();
            System.out.println(inProcessEmbedding);

            // 取消注释，可把结果与 HuggingFace 云端生成的 Embedding 对比
            // EmbeddingModel huggingFaceEmbeddingModel = HuggingFaceEmbeddingModel.builder()
            //        .accessToken(System.getenv("HF_API_KEY"))
            //        .modelId("sentence-transformers/all-MiniLM-L6-v2")
            //        .build();

            //Embedding huggingFaceEmbedding = huggingFaceEmbeddingModel.embed(text).content();

            //System.out.println(CosineSimilarity.between(inProcessEmbedding, huggingFaceEmbedding));
            // 1.000000001963221 <- 这说明本地离线 all-MiniLM-L6-v2 模型生成的向量，
            // 与调用 HuggingFace API 生成的结果几乎完全一致。
        }
    }

    /**
     * 示例：使用自定义的进程内 Embedding 模型。
     * <p>
     * 从本地加载你自行下载的 ONNX 模型文件，并通过 PoolingMode 指定池化方式。
     * 用于演示如何接入其他（非官方预打包）模型。
     */
    static class Custom_In_Process_Embedding_Model_Example {

        public static void main(String[] args) throws IOException {

            // 你可以使用 Hugging Face 上的许多 Embedding 模型。
            // https://huggingface.co/Xenova 仓库里有很多已转换为 ONNX 格式的流行模型。

            // 例如 https://huggingface.co/Xenova/multilingual-e5-large
            // 进入 "Files and versions"：https://huggingface.co/Xenova/multilingual-e5-large/tree/main
            // 下载 "tokenizer.json"：https://huggingface.co/Xenova/multilingual-e5-large/resolve/main/tokenizer.json?download=true
            // 进入 "onnx" 目录：https://huggingface.co/Xenova/multilingual-e5-large/tree/main/onnx
            // 下载 "model_quantized.onnx"：https://huggingface.co/Xenova/multilingual-e5-large/resolve/main/onnx/model_quantized.onnx?download=true
            // 前往原始模型仓库：https://huggingface.co/intfloat/multilingual-e5-large
            // 进入 "Files and versions"：https://huggingface.co/intfloat/multilingual-e5-large/tree/main
            // 进入 "1_Pooling"：https://huggingface.co/intfloat/multilingual-e5-large/tree/main/1_Pooling
            // 查看 "config.json"：https://huggingface.co/intfloat/multilingual-e5-large/blob/main/1_Pooling/config.json
            // 注意其中 "pooling_mode_mean_tokens": true，这说明我们需要使用 PoolingMode.MEAN（均值池化）

            // 你也可以参考这份指南，把任意其他模型转换为 ONNX 格式：https://huggingface.co/docs/optimum/exporters/onnx/usage_guides/export_a_model

            // 需要引入 "langchain4j-embeddings" 依赖（Maven/Gradle），详见 pom.xml
            // 用本地 ONNX 模型文件构建自定义 Embedding 模型：
            // 第一个参数：ONNX 模型文件路径；第二个参数：对应的 tokenizer.json 路径；第三个参数：池化方式 MEAN
            EmbeddingModel custom = new OnnxEmbeddingModel(
                    "C:\\dev\\repo\\langchain4j-embeddings\\langchain4j-embeddings-all-minilm-l6-v2\\target\\classes\\ololo\\all-minilm-l6-v2.onnx",
                    "C:\\dev\\repo\\langchain4j-embeddings\\langchain4j-embeddings-all-minilm-l6-v2\\target\\classes\\all-minilm-l6-v2-tokenizer.json",
                    PoolingMode.MEAN
            );

            // 官方预打包版本（用于和自定义版本对比，验证两者结果是否一致）
            AllMiniLmL6V2EmbeddingModel packaged = new AllMiniLmL6V2EmbeddingModel();

            String englishText = "你好，最近怎么样？";

            // 分别用自定义模型和预打包模型对同一文本向量化
            Embedding customEmbedding = custom.embed(englishText).content();
            Embedding packagedEmbedding = packaged.embed(englishText).content();

            // 计算两个向量之间的余弦相似度（越接近 1 表示越一致）
            System.out.println(CosineSimilarity.between(customEmbedding, packagedEmbedding));
        }
    }
}
