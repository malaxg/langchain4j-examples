package dev.langchain4j.example.booking;

import java.time.LocalDate;

/**
 * 预订信息（使用 Java record 定义不可变数据类）。
 *
 * @param bookingNumber    预订编号（唯一标识一次预订）
 * @param bookingBeginDate 预订开始日期
 * @param bookingEndDate   预订结束日期
 * @param customer         预订人客户信息
 */
public record Booking(
        String bookingNumber,
        LocalDate bookingBeginDate,
        LocalDate bookingEndDate,
        Customer customer) {
}
