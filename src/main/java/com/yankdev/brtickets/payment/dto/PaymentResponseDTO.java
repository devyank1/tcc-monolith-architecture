package com.yankdev.brtickets.payment.dto;

import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.payment.model.PaymentModel;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import com.yankdev.brtickets.payment.model.enums.PaymentStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResponseDTO {
    private UUID id;
    private BookingModel booking;
    private PaymentMethodEnum method;
    private PaymentStatusEnum status;
    private BigDecimal amount;
    private String cardLastFourDigits;
    private Integer installments;
    private LocalDateTime pixExpiresAt;
    private String gatewayTransactionId;
    private String gatewayResponse;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getCardLastFourDigits() {
        return cardLastFourDigits;
    }

    public void setCardLastFourDigits(String cardLastFourDigits) {
        this.cardLastFourDigits = cardLastFourDigits;
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }

    public LocalDateTime getPixExpiresAt() {
        return pixExpiresAt;
    }

    public void setPixExpiresAt(LocalDateTime pixExpiresAt) {
        this.pixExpiresAt = pixExpiresAt;
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

    public static PaymentResponseDTO from(PaymentModel model) {

        PaymentResponseDTO dto = new PaymentResponseDTO();

        dto.setId(model.getId());
        dto.setBooking(model.getBooking());
        dto.setMethod(model.getMethod());
        dto.setStatus(model.getStatus());
        dto.setAmount(model.getAmount());
        dto.setCardLastFourDigits(model.getCardLastFourDigits());
        dto.setInstallments(model.getInstallments());
        dto.setPixExpiresAt(model.getPixExpiresAt());
        dto.setGatewayResponse(model.getGatewayResponse());
        dto.setGatewayTransactionId(model.getGatewayTransactionId());
        dto.setCreatedAt(model.getCreatedAt());
        dto.setPaidAt(model.getPaidAt());
        dto.setRefundedAt(model.getRefundedAt());

        return dto;
    }
}
