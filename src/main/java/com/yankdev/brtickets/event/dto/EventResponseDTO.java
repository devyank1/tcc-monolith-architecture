package com.yankdev.brtickets.event.dto;

import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.venue.model.VenueModel;

import java.time.LocalDateTime;
import java.util.UUID;

public class EventResponseDTO {
    private UUID eventId;
    private VenueModel venue;
    private UserModel user;
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

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public VenueModel getVenue() {
        return venue;
    }

    public void setVenue(VenueModel venue) {
        this.venue = venue;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
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

    public static EventResponseDTO from(EventModel event) {

        EventResponseDTO dto = new EventResponseDTO();

        dto.setEventId(event.getEventId());
        dto.setVenue(event.getVenue());
        dto.setUser(event.getUser());
        dto.setName(event.getName());
        dto.setDescription(event.getDescription());
        dto.setDate(event.getDate());
        dto.setEndDate(event.getEndDate());
        dto.setType(event.getType());
        dto.setArtist(event.getArtist());
        dto.setStatus(event.getStatus());
        dto.setAgeRate(event.getAgeRate());
        dto.setSalesStartAt(event.getSalesStartAt());
        dto.setSalesEndAt(event.getSalesEndAt());

        return dto;
    }
}
