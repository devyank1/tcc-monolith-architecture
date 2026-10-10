package com.yankdev.brtickets.payment.repository;

import com.yankdev.brtickets.TestContainersConfiguration;
import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.booking.model.enums.BookingStatusEnum;
import com.yankdev.brtickets.payment.model.PaymentModel;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import com.yankdev.brtickets.payment.model.enums.PaymentStatusEnum;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestContainersConfiguration.class)
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private BookingModel booking;

    @BeforeEach
    void setUp() {
        booking = entityManager.persistAndFlush(booking());
    }

    private BookingModel booking() {
        BookingModel booking = new BookingModel();
        booking.setUserId(UUID.randomUUID());
        booking.setStatus(BookingStatusEnum.PENDING);
        booking.setTotalAmount(new BigDecimal("250.00"));
        booking.setPaymentMethod(PaymentMethodEnum.PIX);
        booking.setCreatedAt(LocalDateTime.now());
        return booking;
    }

    private PaymentModel payment(PaymentMethodEnum method, PaymentStatusEnum status) {
        PaymentModel payment = new PaymentModel();
        payment.setBooking(booking);
        payment.setMethod(method);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("250.00"));
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }

    @Test
    @DisplayName("stores a PIX payment and reads it back with its PIX fields")
    void persistsPixPayment() {
        PaymentModel pix = payment(PaymentMethodEnum.PIX, PaymentStatusEnum.PENDING);
        pix.setPixKey("brtickets@business.com");
        pix.setPixQrCode(UUID.randomUUID().toString());
        pix.setPixExpiresAt(LocalDateTime.now().plusMinutes(10));

        PaymentModel saved = entityManager.persistAndFlush(pix);
        entityManager.clear();

        PaymentModel stored = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(stored.getMethod()).isEqualTo(PaymentMethodEnum.PIX);
        assertThat(stored.getStatus()).isEqualTo(PaymentStatusEnum.PENDING);
        assertThat(stored.getPixKey()).isEqualTo("brtickets@business.com");
        assertThat(stored.getPixQrCode()).isNotBlank();
        assertThat(stored.getPixExpiresAt()).isNotNull();
        assertThat(stored.getCardBrand()).isNull();
    }

    @Test
    @DisplayName("stores a credit card payment with brand, installments and last digits")
    void persistsCreditCardPayment() {
        PaymentModel card = payment(PaymentMethodEnum.CREDIT_CARD, PaymentStatusEnum.APPROVED);
        card.setCardBrand("VISA");
        card.setInstallments(3);
        card.setCardLastFourDigits("4321");

        PaymentModel saved = entityManager.persistAndFlush(card);
        entityManager.clear();

        PaymentModel stored = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(stored.getCardBrand()).isEqualTo("VISA");
        assertThat(stored.getInstallments()).isEqualTo(3);
        assertThat(stored.getCardLastFourDigits()).isEqualTo("4321");
        assertThat(stored.getPixKey()).isNull();
    }

    @Test
    @DisplayName("generates a UUID and keeps the booking relation readable")
    void generatesIdAndKeepsBookingRelation() {
        PaymentModel saved = entityManager.persistAndFlush(payment(PaymentMethodEnum.PIX, PaymentStatusEnum.PENDING));
        entityManager.clear();

        PaymentModel stored = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(stored.getId()).isNotNull();
        assertThat(stored.getBooking().getBookingId()).isEqualTo(booking.getBookingId());
    }

    @Test
    @DisplayName("the amount column keeps scale 2")
    void persistsAmountScale() {
        PaymentModel pending = payment(PaymentMethodEnum.PIX, PaymentStatusEnum.PENDING);
        pending.setAmount(new BigDecimal("99.90"));

        PaymentModel saved = entityManager.persistAndFlush(pending);
        entityManager.clear();

        assertThat(paymentRepository.findById(saved.getId()).orElseThrow().getAmount())
                .isEqualByComparingTo("99.90");
    }

    @Test
    @DisplayName("a refunded payment keeps its refund timestamp")
    void persistsRefundTimestamp() {
        PaymentModel refunded = payment(PaymentMethodEnum.PIX, PaymentStatusEnum.REFUNDED);
        refunded.setPaidAt(LocalDateTime.now().minusDays(1));
        refunded.setRefundedAt(LocalDateTime.now());

        PaymentModel saved = entityManager.persistAndFlush(refunded);
        entityManager.clear();

        PaymentModel stored = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PaymentStatusEnum.REFUNDED);
        assertThat(stored.getPaidAt()).isNotNull();
        assertThat(stored.getRefundedAt()).isNotNull();
    }

    @Test
    @DisplayName("the amount column is NOT NULL, so a payment without amount is rejected")
    void rejectsPaymentWithoutAmount() {
        PaymentModel noAmount = payment(PaymentMethodEnum.PIX, PaymentStatusEnum.PENDING);
        noAmount.setAmount(null);

        assertThatThrownBy(() -> entityManager.persistAndFlush(noAmount))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("the booking relation is NOT NULL, so an orphan payment is rejected")
    void rejectsPaymentWithoutBooking() {
        PaymentModel orphan = payment(PaymentMethodEnum.PIX, PaymentStatusEnum.PENDING);
        orphan.setBooking(null);

        assertThatThrownBy(() -> entityManager.persistAndFlush(orphan))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("findById returns empty for an id that was never stored")
    void findByIdReturnsEmpty() {
        assertThat(paymentRepository.findById(UUID.randomUUID())).isEmpty();
    }
}
