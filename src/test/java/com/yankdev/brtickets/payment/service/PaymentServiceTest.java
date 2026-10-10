package com.yankdev.brtickets.payment.service;

import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.booking.model.enums.BookingStatusEnum;
import com.yankdev.brtickets.booking.repository.BookingRepository;
import com.yankdev.brtickets.payment.dto.PaymentRequestDTO;
import com.yankdev.brtickets.payment.dto.PaymentResponseDTO;
import com.yankdev.brtickets.payment.model.PaymentModel;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import com.yankdev.brtickets.payment.model.enums.PaymentStatusEnum;
import com.yankdev.brtickets.payment.repository.PaymentRepository;
import com.yankdev.brtickets.shared.exception.AccessDeniedException;
import com.yankdev.brtickets.shared.exception.BookingNotFoundException;
import com.yankdev.brtickets.shared.exception.IllegalPaymentStatusException;
import com.yankdev.brtickets.shared.exception.IllegalPaymentStatusRefundException;
import com.yankdev.brtickets.shared.exception.PaymentNotFoundException;
import com.yankdev.brtickets.shared.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AuthenticatedUserProvider userProvider;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<PaymentModel> paymentCaptor;

    private UUID userId;
    private BookingModel booking;
    private PaymentRequestDTO request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        booking = new BookingModel();
        booking.setBookingId(UUID.randomUUID());
        booking.setUserId(userId);
        booking.setStatus(BookingStatusEnum.PENDING);
        booking.setTotalAmount(new BigDecimal("250.00"));

        request = new PaymentRequestDTO();
        request.setBookingId(booking.getBookingId());
        request.setMethod(PaymentMethodEnum.PIX);
        request.setGatewayTransactionId("tx-123456");
        request.setGatewayResponse("{\"status\":\"created\"}");
    }

    private PaymentModel payment(PaymentStatusEnum status) {
        PaymentModel payment = new PaymentModel();
        payment.setId(UUID.randomUUID());
        payment.setBooking(booking);
        payment.setMethod(PaymentMethodEnum.PIX);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("250.00"));
        payment.setCreatedAt(LocalDateTime.now().minusHours(1));
        return payment;
    }

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("creates the payment as PENDING for the booking owner")
        void createsPendingPayment() {
            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);
            when(paymentRepository.save(any(PaymentModel.class))).thenAnswer(call -> call.getArgument(0));

            PaymentResponseDTO response = paymentService.processPayment(booking.getBookingId(), request);

            assertThat(response.getBookingId()).isEqualTo(booking.getBookingId());
            assertThat(response.getStatus()).isEqualTo(PaymentStatusEnum.PENDING);
            assertThat(response.getMethod()).isEqualTo(PaymentMethodEnum.PIX);
            assertThat(response.getGatewayTransactionId()).isEqualTo("tx-123456");
            assertThat(response.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("fills the PIX fields when the method is PIX")
        void fillsPixFields() {
            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);
            when(paymentRepository.save(any(PaymentModel.class))).thenAnswer(call -> call.getArgument(0));

            paymentService.processPayment(booking.getBookingId(), request);

            verify(paymentRepository).save(paymentCaptor.capture());
            PaymentModel saved = paymentCaptor.getValue();
            assertThat(saved.getPixQrCode()).isNotBlank();
            assertThat(saved.getPixKey()).isEqualTo("brtickets@business.com");
            assertThat(saved.getPixExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(9));
            assertThat(saved.getCardBrand()).isNull();
            assertThat(saved.getInstallments()).isNull();
        }

        @Test
        @DisplayName("fills brand, installments and last digits when the method is CREDIT_CARD")
        void fillsCreditCardFields() {
            request.setMethod(PaymentMethodEnum.CREDIT_CARD);
            request.setCardBrand("VISA");
            request.setInstallments(3);
            request.setCardLastFourDigits("4321");

            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);
            when(paymentRepository.save(any(PaymentModel.class))).thenAnswer(call -> call.getArgument(0));

            paymentService.processPayment(booking.getBookingId(), request);

            verify(paymentRepository).save(paymentCaptor.capture());
            PaymentModel saved = paymentCaptor.getValue();
            assertThat(saved.getCardBrand()).isEqualTo("VISA");
            assertThat(saved.getInstallments()).isEqualTo(3);
            assertThat(saved.getCardLastFourDigits()).isEqualTo("4321");
            assertThat(saved.getPixQrCode()).isNull();
        }

        @Test
        @DisplayName("fills brand and last digits but no installments when the method is DEBIT_CARD")
        void fillsDebitCardFields() {
            request.setMethod(PaymentMethodEnum.DEBIT_CARD);
            request.setCardBrand("MASTERCARD");
            request.setInstallments(5);
            request.setCardLastFourDigits("8765");

            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);
            when(paymentRepository.save(any(PaymentModel.class))).thenAnswer(call -> call.getArgument(0));

            paymentService.processPayment(booking.getBookingId(), request);

            verify(paymentRepository).save(paymentCaptor.capture());
            PaymentModel saved = paymentCaptor.getValue();
            assertThat(saved.getCardBrand()).isEqualTo("MASTERCARD");
            assertThat(saved.getCardLastFourDigits()).isEqualTo("8765");
            assertThat(saved.getInstallments()).isNull();
            assertThat(saved.getPixQrCode()).isNull();
        }

        @Test
        @DisplayName("leaves card and PIX fields empty when the method is MONEY")
        void fillsNoSpecificFieldForMoney() {
            request.setMethod(PaymentMethodEnum.MONEY);

            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);
            when(paymentRepository.save(any(PaymentModel.class))).thenAnswer(call -> call.getArgument(0));

            paymentService.processPayment(booking.getBookingId(), request);

            verify(paymentRepository).save(paymentCaptor.capture());
            PaymentModel saved = paymentCaptor.getValue();
            assertThat(saved.getPixQrCode()).isNull();
            assertThat(saved.getPixKey()).isNull();
            assertThat(saved.getCardBrand()).isNull();
            assertThat(saved.getInstallments()).isNull();
        }

        @Test
        @DisplayName("denies paying a booking that belongs to another user")
        void deniesOtherUsersBooking() {
            booking.setUserId(UUID.randomUUID());
            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> paymentService.processPayment(booking.getBookingId(), request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You have not permission to process this payment");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the booking does not exist")
        void failsWhenBookingDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(bookingRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processPayment(unknownId, request))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessage("We cannot found your booking.");

            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {

        @Test
        @DisplayName("approves the payment and confirms the booking")
        void approvesPaymentAndConfirmsBooking() {
            PaymentModel pending = payment(PaymentStatusEnum.PENDING);
            when(paymentRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
            when(userProvider.getCurrentUserId()).thenReturn(userId);
            when(paymentRepository.save(any(PaymentModel.class))).thenAnswer(call -> call.getArgument(0));

            PaymentResponseDTO response = paymentService.confirmPayment(pending.getId());

            assertThat(response.getStatus()).isEqualTo(PaymentStatusEnum.APPROVED);
            assertThat(booking.getStatus()).isEqualTo(BookingStatusEnum.CONFIRMED);
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("refuses to confirm a payment that is not PENDING")
        void refusesNonPendingPayment() {
            PaymentModel approved = payment(PaymentStatusEnum.APPROVED);
            when(paymentRepository.findById(approved.getId())).thenReturn(Optional.of(approved));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> paymentService.confirmPayment(approved.getId()))
                    .isInstanceOf(IllegalPaymentStatusException.class)
                    .hasMessage("Your payment must be PENDING, because you didn't pay yet.");

            verify(paymentRepository, never()).save(any());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("denies confirming a payment of another user")
        void deniesOtherUsersPayment() {
            booking.setUserId(UUID.randomUUID());
            PaymentModel pending = payment(PaymentStatusEnum.PENDING);
            when(paymentRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> paymentService.confirmPayment(pending.getId()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You have not permission to confirm this payment");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the payment does not exist")
        void failsWhenPaymentDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(paymentRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.confirmPayment(unknownId))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessage("We could not find your payment");

            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        @DisplayName("refunds an approved payment and stamps refundedAt")
        void refundsApprovedPayment() {
            PaymentModel approved = payment(PaymentStatusEnum.APPROVED);
            when(paymentRepository.findById(approved.getId())).thenReturn(Optional.of(approved));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            paymentService.refundPayment(approved.getId());

            verify(paymentRepository).save(paymentCaptor.capture());
            PaymentModel saved = paymentCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(PaymentStatusEnum.REFUNDED);
            assertThat(saved.getRefundedAt()).isNotNull();
        }

        @Test
        @DisplayName("refuses to refund a payment that is already REFUNDED")
        void refusesAlreadyRefundedPayment() {
            PaymentModel refunded = payment(PaymentStatusEnum.REFUNDED);
            when(paymentRepository.findById(refunded.getId())).thenReturn(Optional.of(refunded));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> paymentService.refundPayment(refunded.getId()))
                    .isInstanceOf(IllegalPaymentStatusRefundException.class)
                    .hasMessage("Your payment has already refunded.");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("denies refunding a payment of another user")
        void deniesOtherUsersPayment() {
            booking.setUserId(UUID.randomUUID());
            PaymentModel approved = payment(PaymentStatusEnum.APPROVED);
            when(paymentRepository.findById(approved.getId())).thenReturn(Optional.of(approved));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> paymentService.refundPayment(approved.getId()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You have not permission to refund this payment");

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the payment does not exist")
        void failsWhenPaymentDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(paymentRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.refundPayment(unknownId))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessage("Payment not found");

            verify(paymentRepository, never()).save(any());
        }
    }
}
