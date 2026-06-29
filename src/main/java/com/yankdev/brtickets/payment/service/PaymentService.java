package com.yankdev.brtickets.payment.service;

import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.booking.repository.BookingRepository;
import com.yankdev.brtickets.payment.dto.PaymentRequestDTO;
import com.yankdev.brtickets.payment.dto.PaymentResponseDTO;
import com.yankdev.brtickets.payment.model.PaymentModel;
import com.yankdev.brtickets.payment.model.enums.PaymentStatusEnum;
import com.yankdev.brtickets.payment.repository.PaymentRepository;
import com.yankdev.brtickets.shared.exception.BookingNotFoundException;
import com.yankdev.brtickets.shared.exception.IllegalPaymentStatusRefundException;
import com.yankdev.brtickets.shared.exception.PaymentNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    public PaymentResponseDTO processPayment(UUID bookingId, PaymentRequestDTO request) {

        BookingModel booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("We cannot found your booking."));

        PaymentModel payment = new PaymentModel();

        payment.setBooking(booking);
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatusEnum.PENDING);
        payment.setAmount(booking.getTotalAmount());
        payment.setGatewayTransactionId(request.getGatewayTransactionId());
        payment.setGatewayResponse(request.getGatewayResponse());

        PaymentModel newPayment = paymentRepository.save(payment);

        return PaymentResponseDTO.from(newPayment);
    }

    public void refundPayment(UUID paymentId) {

        PaymentModel payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (PaymentStatusEnum.REFUNDED == payment.getStatus()) {
            throw new IllegalPaymentStatusRefundException("Your payment has already refunded.");
        }

        payment.setStatus(PaymentStatusEnum.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        paymentRepository.save(payment);
    }
}
