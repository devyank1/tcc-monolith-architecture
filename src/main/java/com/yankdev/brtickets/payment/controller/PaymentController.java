package com.yankdev.brtickets.payment.controller;

import com.yankdev.brtickets.payment.dto.PaymentRequestDTO;
import com.yankdev.brtickets.payment.dto.PaymentResponseDTO;
import com.yankdev.brtickets.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Tag(name = "Process Payment", description = "Initiate a payment for a booking")
    public ResponseEntity<PaymentResponseDTO> processPayment(@RequestParam UUID bookingId, @RequestBody PaymentRequestDTO request) {

        PaymentResponseDTO payment = paymentService.processPayment(bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PostMapping("/{paymentId}/confirm")
    @Tag(name = "Confirm Payment", description = "Confirm a pending payment")
    public ResponseEntity<PaymentResponseDTO> confirmPayment(@PathVariable UUID paymentId) {

        PaymentResponseDTO payment = paymentService.confirmPayment(paymentId);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{paymentId}/refund")
    @Tag(name = "Refund Payment", description = "Initiate a refund for a completed payment")
    public ResponseEntity<Void> refundPayment(@PathVariable UUID paymentId) {

        paymentService.refundPayment(paymentId);
        return ResponseEntity.noContent().build();
    }
}
