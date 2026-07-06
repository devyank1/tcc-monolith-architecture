package com.yankdev.brtickets.event.dto;

import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public class EventRequestDTO {
    private UUID venueId;
    private String name;
    private String description;
    private LocalDateTime date;
    private LocalDateTime endDate;
    private EventTypeEnum type;
    private String artist;
    private EventStatusEnum status;
    private Integer ageRate;
    private LocalDateTime salesStartAt;
    private LocalDateTime salesEndAt;

    public UUID getVenueId() {
        return venueId;
    }

    public void setVenueId(UUID venueId) {
        this.venueId = venueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public EventTypeEnum getType() {
        return type;
    }

    public void setType(EventTypeEnum type) {
        this.type = type;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public EventStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EventStatusEnum status) {
        this.status = status;
    }

    public Integer getAgeRate() {
        return ageRate;
    }

    public void setAgeRate(Integer ageRate) {
        this.ageRate = ageRate;
    }

    public LocalDateTime getSalesStartAt() {
        return salesStartAt;
    }

    public void setSalesStartAt(LocalDateTime salesStartAt) {
        this.salesStartAt = salesStartAt;
    }

    public LocalDateTime getSalesEndAt() {
        return salesEndAt;
    }

    public void setSalesEndAt(LocalDateTime salesEndAt) {
        this.salesEndAt = salesEndAt;
    }
}
