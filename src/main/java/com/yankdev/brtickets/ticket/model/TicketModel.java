package com.yankdev.brtickets.ticket.model;

import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.ticket.model.enums.TicketStatusEnum;
import com.yankdev.brtickets.ticket.model.enums.TicketTypeEnum;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class TicketModel {

    @Id
    @UuidGenerator
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private UUID ticketId;
    @JoinColumn(name = "eventId")
    @OneToOne(fetch = FetchType.LAZY)
    private EventModel event;
    @Column(nullable = false)
    private String sector;
    private String row;
    @Column(nullable = false)
    private String seat;
    @Column(nullable = false)
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    private TicketStatusEnum status;
    @Enumerated(EnumType.STRING)
    private TicketTypeEnum type;
    @Column(unique = true)
    private String qrCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public EventModel getEvent() {
        return event;
    }

    public void setEvent(EventModel event) {
        this.event = event;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public String getSeat() {
        return seat;
    }

    public void setSeat(String seat) {
        this.seat = seat;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public TicketStatusEnum getStatus() {
        return status;
    }

    public void setStatus(TicketStatusEnum status) {
        this.status = status;
    }

    public TicketTypeEnum getType() {
        return type;
    }

    public void setType(TicketTypeEnum type) {
        this.type = type;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
