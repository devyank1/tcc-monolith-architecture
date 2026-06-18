package com.yankdev.brtickets.venue.model;

import jakarta.persistence.Entity;

import java.util.List;

@Entity
public class RowVenue {
    private String label;
    private List<String> seats;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
    }
}
