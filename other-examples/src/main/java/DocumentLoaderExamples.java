import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.*;

public class DocumentLoaderExamples {

    private static final Logger log = LoggerFactory.getLogger(DocumentLoaderExamples.class);

    /**
     * 程序入口主方法：依次演示 5 种不同的文档加载方式。
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {
        loadSingleDocument();             // 加载单个文档
        loadMultipleDocuments();          // 一次性加载目录下的多个文档
        loadMultipleDocumentsWithGlob();  // 用 glob 通配符过滤后加载多个文档
        loadMultipleDocumentsRecursively(); // 递归加载目录下所有子目录中的文档
        loadUsingParserFromSPI();         // 通过 SPI 自动发现解析器来加载文档
    }

    /**
     * 加载单个文档示例。
     * <p>
     * 使用 ApacheTikaDocumentParser 解析 PDF 等文件，
     * 提取出其中包含的纯文本内容。
     */
    private static void loadSingleDocument() {
        Path documentPath = toPath("example-files/story-about-happy-carrot.pdf");
        log.info("正在加载单个文档: {}", documentPath);
        // loadDocument：加载指定路径的单个文档，这里显式指定使用 ApacheTika 解析器
        Document document = loadDocument(documentPath, new ApacheTikaDocumentParser());
        log(document);
        log.info("");
    }

    /**
     * 加载目录下的多个文档示例。
     * <p>
     * loadDocuments 会扫描指定目录，把其中可解析的文件全部加载成 Document。
     */
    private static void loadMultipleDocuments() {
        Path directoryPath = toPath("example-files/");
        log.info("正在从以下目录加载多个文档: {}", directoryPath);
        List<Document> documents = loadDocuments(directoryPath, new ApacheTikaDocumentParser());
        documents.forEach(DocumentLoaderExamples::log);
        log.info("");
    }

    /**
     * 使用 glob 通配符过滤后加载多个文档示例。
     * <p>
     * 通过 PathMatcher（这里匹配所有 .txt 文件）只加载符合规则的文件。
     */
    private static void loadMultipleDocumentsWithGlob() {
        Path directoryPath = toPath("example-files/");
        // 构造一个文件路径匹配器，匹配规则为 glob:*.txt（即只匹配 .txt 结尾的文件）
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.txt");
        log.info("正在从以下目录加载 *.txt 文档: {}", directoryPath);
        List<Document> documents = loadDocuments(directoryPath, pathMatcher, new ApacheTikaDocumentParser());
        documents.forEach(DocumentLoaderExamples::log);
        log.info("");
    }

    /**
     * 递归加载多个文档示例。
     * <p>
     * loadDocumentsRecursively 会连同子目录一起递归扫描，
     * 把目录树里所有可解析的文件都加载成 Document。
     */
    private static void loadMultipleDocumentsRecursively() {
        Path directoryPath = toPath("example-files/");
        log.info("正在从以下目录递归加载多个文档: {}", directoryPath);
        List<Document> documents = loadDocumentsRecursively(directoryPath, new ApacheTikaDocumentParser());
        documents.forEach(DocumentLoaderExamples::log);
        log.info("");
    }

    /**
     * 通过 SPI 自动发现解析器来加载文档示例。
     * <p>
     * 这里没有显式传入解析器，而是依赖 classpath 中的 SPI 配置自动发现可用的解析器。
     */
    private static void loadUsingParserFromSPI() {
        Path documentPath = toPath("example-files/story-about-happy-carrot.pdf");
        log.info("正在通过 SPI 导入的解析器加载文档: {}", documentPath);
        Document document = loadDocument(documentPath); // 这里不指定解析器，解析器由 SPI 机制自动发现
        log(document);
        log.info("");
    }

    /**
     * 打印单个文档的关键信息（文件名 + 正文前 50 个字符），方便观察加载结果。
     *
     * @param document 待打印的文档对象
     */
    private static void log(Document document) {
        log.info("{}: {} ...", document.metadata().getString("file_name"), document.text().trim().substring(0, 50));
    }

    /**
     * 把 classpath 资源中的相对路径转换成文件系统的绝对路径 {@link Path}。
     * <p>
     * 通过 {@link Class#getResource} 定位资源，再把资源 URL 转为本地文件路径。
     *
     * @param fileName 资源文件名（相对路径）
     * @return 对应的文件系统路径
     */
    private static Path toPath(String fileName) {
        try {
            URL fileUrl = DocumentLoaderExamples.class.getResource(fileName);
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
