package dev.langchain4j.example.booking;

/**
 * 客户信息（使用 Java record 定义不可变数据类）。
 *
 * @param name    客户名（名）
 * @param surname 客户姓
 */
public record Customer(String name, String surname) {
}
