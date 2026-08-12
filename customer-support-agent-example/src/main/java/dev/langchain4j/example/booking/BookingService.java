package dev.langchain4j.example.booking;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 预订服务：模拟真实业务系统的数据访问层。
 *
 * <p>这里使用内存中的 Map 充当"数据库"，预置了一条演示预订数据。
 * 工具类 {@code BookingTools} 调用的正是本服务的方法。</p>
 */
@Component
public class BookingService {

    // 演示客户：John Doe（预置数据，与测试用例保持一致，保留英文原样）
    private static final Customer CUSTOMER = new Customer("John", "Doe");

    // 演示预订编号
    private static final String BOOKING_NUMBER = "MS-777";
    // 演示预订：2025-12-13 至 2025-12-31，属于 John Doe
    private static final Booking BOOKING = new Booking(
            BOOKING_NUMBER,
            LocalDate.of(2025, 12, 13),
            LocalDate.of(2025, 12, 31),
            CUSTOMER
    );

    // 模拟"数据库表"：预订编号 -> 预订记录
    private static final Map<String, Booking> BOOKINGS = new HashMap<>() {{
        put(BOOKING_NUMBER, BOOKING);
    }};

    /**
     * 查询预订详情。
     *
     * @param bookingNumber   预订编号
     * @param customerName    客户名（名）
     * @param customerSurname 客户姓
     * @return 预订详情
     * @throws BookingNotFoundException 预订不存在或客户信息不匹配时抛出
     */
    public Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname) {
        // 先校验预订存在且属于该客户
        ensureExists(bookingNumber, customerName, customerSurname);

        // 模拟数据库查询
        return BOOKINGS.get(bookingNumber);
    }

    /**
     * 取消预订。
     *
     * @param bookingNumber   预订编号
     * @param customerName    客户名（名）
     * @param customerSurname 客户姓
     * @throws BookingNotFoundException 预订不存在或客户信息不匹配时抛出
     */
    public void cancelBooking(String bookingNumber, String customerName, String customerSurname) {
        // 先校验预订存在且属于该客户
        ensureExists(bookingNumber, customerName, customerSurname);

        // 模拟取消预订（从"数据库"中移除记录）
        BOOKINGS.remove(bookingNumber);
    }

    /**
     * 校验预订是否存在，且预订人信息与传入的客户信息一致。
     *
     * @param bookingNumber   预订编号
     * @param customerName    客户名（名）
     * @param customerSurname 客户姓
     * @throws BookingNotFoundException 预订不存在，或姓名/姓氏不匹配时抛出
     */
    private void ensureExists(String bookingNumber, String customerName, String customerSurname) {
        // 模拟数据库查询

        Booking booking = BOOKINGS.get(bookingNumber);
        if (booking == null) {
            throw new BookingNotFoundException(bookingNumber);
        }

        Customer customer = booking.customer();
        // 校验客户名
        if (!customer.name().equals(customerName)) {
            throw new BookingNotFoundException(bookingNumber);
        }
        // 校验客户姓
        if (!customer.surname().equals(customerSurname)) {
            throw new BookingNotFoundException(bookingNumber);
        }
    }
}
