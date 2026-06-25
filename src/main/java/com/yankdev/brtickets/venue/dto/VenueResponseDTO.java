package com.yankdev.brtickets.venue.dto;

import com.yankdev.brtickets.venue.model.SeatMapVenue;
import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.model.enums.VenueEnum;

import java.util.UUID;

public class VenueResponseDTO {
    private UUID venueId;
    private String name;
    private String description;
    private VenueEnum type;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Integer capacity;
    private boolean isActive;
    private SeatMapVenue seatMap;

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

    public VenueEnum getType() {
        return type;
    }

    public void setType(VenueEnum type) {
        this.type = type;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public SeatMapVenue getSeatMap() {
        return seatMap;
    }

    public void setSeatMap(SeatMapVenue seatMap) {
        this.seatMap = seatMap;
    }

    public static VenueResponseDTO from(VenueModel model) {
        VenueResponseDTO dto = new VenueResponseDTO();

        dto.setVenueId(model.getVenueId());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        dto.setType(model.getType());
        dto.setStreet(model.getStreet());
        dto.setCity(model.getCity());
        dto.setState(model.getState());
        dto.setZipCode(model.getZipCode());
        dto.setCountry(model.getCountry());
        dto.setCapacity(model.getCapacity());
        dto.setActive(model.isActive());
        dto.setSeatMap(model.getSeatMap());

        return dto;
    }

}
