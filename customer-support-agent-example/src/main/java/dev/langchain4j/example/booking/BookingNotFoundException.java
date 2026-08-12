package dev.langchain4j.example.booking;

/**
 * 预订不存在的自定义异常。
 *
 * <p>当按预订编号查不到预订，或客户信息与预订人不匹配时抛出。</p>
 */
public class BookingNotFoundException extends RuntimeException {

    /**
     * 构造异常对象。
     *
     * @param bookingNumber 不存在的预订编号
     */
    public BookingNotFoundException(String bookingNumber) {
        // 异常消息保留英文：工具调用出错时该消息会回传给大模型，供其生成合适的客服话术
        super("Booking " + bookingNumber + " not found");
    }
}
