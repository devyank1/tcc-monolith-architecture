package com.yankdev.brtickets.payment.dto;

import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import com.yankdev.brtickets.payment.model.enums.PaymentStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentRequestDTO {
    private BookingModel booking;
    private PaymentMethodEnum method;
    private PaymentStatusEnum status;
    private BigDecimal amount;
    private String gatewayTransactionId;
    private String gatewayResponse;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    public BookingModel getBooking() {
        return booking;
    }

    public void setBooking(BookingModel booking) {
        this.booking = booking;
    }

    public PaymentMethodEnum getMethod() {
        return method;
    }

    public void setMethod(PaymentMethodEnum method) {
        this.method = method;
    }

    public PaymentStatusEnum getStatus() {
        return status;
    }

    public void setStatus(PaymentStatusEnum status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}
