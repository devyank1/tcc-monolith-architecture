package com.yankdev.brtickets.ticket.dto;

import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.ticket.model.TicketModel;
import com.yankdev.brtickets.ticket.model.enums.TicketStatusEnum;
import com.yankdev.brtickets.ticket.model.enums.TicketTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TicketResponseDTO {

    private UUID ticketId;
    private UUID eventId;
    private String sector;
    private String row;
    private String seat;
    private BigDecimal price;
    private TicketStatusEnum status;
    private TicketTypeEnum type;
    private String qrCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
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

    public static TicketResponseDTO from(TicketModel ticket) {

        TicketResponseDTO dto = new TicketResponseDTO();

        dto.setTicketId(ticket.getTicketId());
        dto.setEventId(ticket.getEvent().getEventId());
        dto.setSector(ticket.getSector());
        dto.setRow(ticket.getRow());
        dto.setSeat(ticket.getSeat());
        dto.setPrice(ticket.getPrice());
        dto.setStatus(ticket.getStatus());
        dto.setType(ticket.getType());
        dto.setQrCode(ticket.getQrCode());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());

        return dto;
    }
}
