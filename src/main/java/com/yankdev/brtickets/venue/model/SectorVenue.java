package com.yankdev.brtickets.venue.model;

import jakarta.persistence.Entity;

import java.util.List;

@Entity
public class SectorVenue {
    private String name;
    private String code;
    private String totalSeats;
    private List<RowVenue> rows;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(String totalSeats) {
        this.totalSeats = totalSeats;
    }

    public List<RowVenue> getRows() {
        return rows;
    }

    public void setRows(List<RowVenue> rows) {
        this.rows = rows;
    }
}
