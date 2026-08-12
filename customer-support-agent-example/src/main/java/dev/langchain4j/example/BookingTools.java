package dev.langchain4j.example;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.example.booking.Booking;
import dev.langchain4j.example.booking.BookingService;
import org.springframework.stereotype.Component;

/**
 * 客服 Agent 使用的"工具"类。
 *
 * <p>所谓工具（Tool），就是 Agent 可以主动调用的函数：当大模型判断需要查询或取消预订时，
 * 它会根据方法名和参数描述自动选择并调用这里的方法，然后把结果返回给大模型。</p>
 *
 * <p>用 {@code @Tool} 注解标记的 public 方法会被 LangChain4j 识别为工具。
 * 大模型通过"函数调用（Function Calling）"机制与这些方法协作。</p>
 */
@Component
public class BookingTools {

    // 依赖注入预订服务，真正执行业务逻辑
    private final BookingService bookingService;

    /**
     * 构造器注入预订服务。
     *
     * @param bookingService 预订服务（Spring 自动注入）
     */
    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * 工具：查询预订详情。
     *
     * <p>大模型在回答"我的预订什么时候开始"这类问题时，会调用本方法获取预订信息。</p>
     *
     * @param bookingNumber   预订编号
     * @param customerName    客户名（名）
     * @param customerSurname 客户姓
     * @return 预订信息；若预订不存在或客户信息不匹配，抛出 {@code BookingNotFoundException}
     */
    @Tool
    public Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname) {
        return bookingService.getBookingDetails(bookingNumber, customerName, customerSurname);
    }

    /**
     * 工具：取消预订。
     *
     * <p>大模型在确认用户要取消预订后调用本方法。</p>
     *
     * @param bookingNumber   预订编号
     * @param customerName    客户名（名）
     * @param customerSurname 客户姓
     */
    @Tool
    public void cancelBooking(String bookingNumber, String customerName, String customerSurname) {
        bookingService.cancelBooking(bookingNumber, customerName, customerSurname);
    }
}
