package com.yankdev.brtickets.booking.item.model;

import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.ticket.model.TicketModel;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bookingItem")
public class BookingItemModel {

    @Id
    @UuidGenerator
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookingItemId")
    private UUID bookingItemId;
    @ManyToOne(fetch = FetchType.LAZY)
    private BookingModel booking;
    @ManyToOne(fetch = FetchType.LAZY)
    private TicketModel ticket;
    private BigDecimal unitPrice;

    public UUID getBookingItemId() {
        return bookingItemId;
    }

    public void setBookingItemId(UUID bookingItemId) {
        this.bookingItemId = bookingItemId;
    }

    public BookingModel getBooking() {
        return booking;
    }

    public void setBooking(BookingModel booking) {
        this.booking = booking;
    }

    public TicketModel getTicket() {
        return ticket;
    }

    public void setTicket(TicketModel ticket) {
        this.ticket = ticket;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
