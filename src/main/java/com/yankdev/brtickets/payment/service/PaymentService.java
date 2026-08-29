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
import com.yankdev.brtickets.shared.exception.*;
import com.yankdev.brtickets.shared.security.AuthenticatedUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final AuthenticatedUserProvider userProvider;


    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository, AuthenticatedUserProvider userProvider) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userProvider = userProvider;
    }

    public PaymentResponseDTO processPayment(UUID bookingId, PaymentRequestDTO request) {

        BookingModel booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("We cannot found your booking."));

        if (!booking.getUserId().equals(userProvider.getCurrentUserId())) {
            throw new AccessDeniedException("You have not permission to process this payment");
        }

        PaymentModel payment = new PaymentModel();

        payment.setBooking(booking);
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatusEnum.PENDING);
        payment.setGatewayTransactionId(request.getGatewayTransactionId());
        payment.setGatewayResponse(request.getGatewayResponse());
        payment.setPaidAt(LocalDateTime.now());

        if (PaymentMethodEnum.PIX == request.getMethod()) {
            payment.setPixExpiresAt(LocalDateTime.now().plusMinutes(10));
            payment.setPixQrCode(UUID.randomUUID().toString());
            payment.setPixKey("brtickets@business.com");
        }

        if (PaymentMethodEnum.CREDIT_CARD == request.getMethod()) {
            payment.setCardBrand(request.getCardBrand());
            payment.setInstallments(request.getInstallments());
            payment.setCardLastFourDigits(request.getCardLastFourDigits());
        }

        if (PaymentMethodEnum.DEBIT_CARD == request.getMethod()) {
            payment.setCardBrand(request.getCardBrand());
            payment.setCardLastFourDigits(request.getCardLastFourDigits());
        }

        PaymentModel newPayment = paymentRepository.save(payment);

        return PaymentResponseDTO.from(newPayment);
    }

    @Transactional
    public PaymentResponseDTO confirmPayment(UUID paymentId) {

        PaymentModel payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("We could not find your payment"));

        if (!payment.getBooking().getUserId().equals(userProvider.getCurrentUserId())) {
            throw new AccessDeniedException("You have not permission to confirm this payment");
        }

        if (payment.getStatus() != PaymentStatusEnum.PENDING) {
            throw new IllegalPaymentStatusException("Your payment must be PENDING, because you didn't pay yet.");
        }

        payment.setStatus(PaymentStatusEnum.APPROVED);
        payment.getBooking().setStatus(BookingStatusEnum.CONFIRMED);

        bookingRepository.save(payment.getBooking());
        PaymentModel paid = paymentRepository.save(payment);

        return PaymentResponseDTO.from(paid);
    }

    public void refundPayment(UUID paymentId) {

        PaymentModel payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.getBooking().getUserId().equals(userProvider.getCurrentUserId())) {
            throw new AccessDeniedException("You have not permission to refund this payment");
        }

        if (PaymentStatusEnum.REFUNDED == payment.getStatus()) {
            throw new IllegalPaymentStatusRefundException("Your payment has already refunded.");
        }

        payment.setStatus(PaymentStatusEnum.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        paymentRepository.save(payment);
    }
}
