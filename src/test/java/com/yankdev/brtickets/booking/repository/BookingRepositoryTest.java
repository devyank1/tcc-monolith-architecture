package com.yankdev.brtickets.booking.repository;

import com.yankdev.brtickets.TestContainersConfiguration;
import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.booking.model.enums.BookingStatusEnum;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestContainersConfiguration.class)
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID firstUserId;
    private UUID secondUserId;

    @BeforeEach
    void setUp() {
        firstUserId = UUID.randomUUID();
        secondUserId = UUID.randomUUID();

        entityManager.persistAndFlush(booking(firstUserId, BookingStatusEnum.CONFIRMED, "250.00"));
        entityManager.persistAndFlush(booking(firstUserId, BookingStatusEnum.CANCELLED, "100.00"));
        entityManager.persistAndFlush(booking(secondUserId, BookingStatusEnum.PENDING, "75.50"));
    }

    private BookingModel booking(UUID userId, BookingStatusEnum status, String total) {
        BookingModel booking = new BookingModel();
        booking.setUserId(userId);
        booking.setStatus(status);
        booking.setTotalAmount(new BigDecimal(total));
        booking.setPaymentMethod(PaymentMethodEnum.PIX);
        booking.setCreatedAt(LocalDateTime.now());
        if (status == BookingStatusEnum.CONFIRMED) {
            booking.setConfirmedAt(LocalDateTime.now());
        }
        if (status == BookingStatusEnum.CANCELLED) {
            booking.setCancelledAt(LocalDateTime.now());
        }
        return booking;
    }

    @Test
    @DisplayName("findAllByUserId returns every booking of that user, whatever the status")
    void findsBookingsOfUser() {
        List<BookingModel> bookings = bookingRepository.findAllByUserId(firstUserId);

        assertThat(bookings).hasSize(2)
                .extracting(BookingModel::getStatus)
                .containsExactlyInAnyOrder(BookingStatusEnum.CONFIRMED, BookingStatusEnum.CANCELLED);
    }

    @Test
    @DisplayName("findAllByUserId never returns bookings of another user")
    void doesNotLeakOtherUsersBookings() {
        List<BookingModel> bookings = bookingRepository.findAllByUserId(secondUserId);

        assertThat(bookings).hasSize(1)
                .allMatch(booking -> booking.getUserId().equals(secondUserId));
    }

    @Test
    @DisplayName("findAllByUserId returns empty for a user with no booking")
    void findsNoBookingForUnknownUser() {
        assertThat(bookingRepository.findAllByUserId(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("the total amount keeps its decimal places")
    void persistsTotalAmount() {
        BookingModel stored = bookingRepository.findAllByUserId(secondUserId).get(0);

        assertThat(stored.getTotalAmount()).isEqualByComparingTo("75.50");
    }

    @Test
    @DisplayName("status and payment method are stored as strings")
    void persistsEnumsAsStrings() {
        BookingModel stored = bookingRepository.findAllByUserId(secondUserId).get(0);

        assertThat(stored.getStatus()).isEqualTo(BookingStatusEnum.PENDING);
        assertThat(stored.getPaymentMethod()).isEqualTo(PaymentMethodEnum.PIX);
    }

    @Test
    @DisplayName("a cancelled booking keeps its cancellation timestamp and no confirmation")
    void persistsTimestamps() {
        BookingModel cancelled = bookingRepository.findAllByUserId(firstUserId).stream()
                .filter(booking -> booking.getStatus() == BookingStatusEnum.CANCELLED)
                .findFirst()
                .orElseThrow();

        assertThat(cancelled.getCreatedAt()).isNotNull();
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getConfirmedAt()).isNull();
    }
}
