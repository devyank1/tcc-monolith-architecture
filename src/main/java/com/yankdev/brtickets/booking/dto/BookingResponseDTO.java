package com.yankdev.brtickets.booking.dto;

import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.booking.model.enums.BookingStatusEnum;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import com.yankdev.brtickets.user.model.UserModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BookingResponseDTO {

    private UUID bookingId;
    private UserModel user;
    private BookingStatusEnum status;
    private BigDecimal totalAmount;
    private PaymentMethodEnum paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public BookingStatusEnum getStatus() {
        return status;
    }

    public void setStatus(BookingStatusEnum status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentMethodEnum getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethodEnum paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public static BookingResponseDTO from(BookingModel booking) {
        BookingResponseDTO dto = new BookingResponseDTO();

        booking.setBookingId(dto.getBookingId());
        booking.setUser(dto.getUser());
        booking.setStatus(dto.getStatus());
        booking.setTotalAmount(dto.getTotalAmount());
        booking.setPaymentMethod(dto.getPaymentMethod());
        booking.setCreatedAt(dto.getCreatedAt());
        booking.setConfirmedAt(dto.getConfirmedAt());
        booking.setCancelledAt(dto.getCancelledAt());

        return dto;
    }
}
