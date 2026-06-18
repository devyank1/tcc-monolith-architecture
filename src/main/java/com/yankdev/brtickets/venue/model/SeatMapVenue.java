package com.yankdev.brtickets.venue.model;

import jakarta.persistence.Entity;

import java.util.List;

@Entity
public class SeatMapVenue {
    private List<SectorVenue> sectors;

    public List<SectorVenue> getSectors() {
        return sectors;
    }

    public void setSectors(List<SectorVenue> sectors) {
        this.sectors = sectors;
    }
}
