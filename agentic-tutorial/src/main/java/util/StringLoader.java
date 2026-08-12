package util;

import java.io.IOException;
import java.io.InputStream;

/**
 * 工具类：从 classpath 资源目录加载文本文件的内容。
 * 示例中大量使用 *.txt 资源文件（如简历、职位描述等）作为 Agent 的输入数据，
 * 本类把这些资源文件整体读取为字符串，供工作流使用。
 */
public class StringLoader {
    
    /**
     * 使用本类（StringLoader）自己的类加载器，从资源路径加载文本内容。
     * @param resourcePath 资源路径，如 "/documents/user_life_story.txt"
     * @return 文件内容的字符串
     * @throws IOException 资源不存在或读取失败时抛出
     */
    public static String loadFromResource(String resourcePath) throws IOException {
        try (InputStream inputStream = StringLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes());
        }
    }
    
    /**
     * 使用指定类的类加载器，从资源路径加载文本内容。
     * @param clazz        用于定位资源的类（便于加载与该类同包/同目录的资源）
     * @param resourcePath 资源路径
     * @return 文件内容的字符串
     * @throws IOException 资源不存在或读取失败时抛出
     */
    public static String loadFromResource(Class<?> clazz, String resourcePath) throws IOException {
        try (InputStream inputStream = clazz.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath + " for class: " + clazz.getName());
            }
            return new String(inputStream.readAllBytes());
        }
    }
}