package com.yankdev.brtickets.payment.controller;

import com.yankdev.brtickets.payment.dto.PaymentRequestDTO;
import com.yankdev.brtickets.payment.dto.PaymentResponseDTO;
import com.yankdev.brtickets.payment.service.PaymentService;
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
    public ResponseEntity<PaymentResponseDTO> processPayment(@RequestParam UUID bookingId, @RequestBody PaymentRequestDTO request) {

        PaymentResponseDTO payment = paymentService.processPayment(bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Void> refundPayment(@PathVariable UUID paymentId) {

        paymentService.refundPayment(paymentId);
        return ResponseEntity.noContent().build();
    }
}
