package com.yankdev.brtickets.venue.model.converter;

import com.yankdev.brtickets.venue.model.SeatMapVenue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.ObjectMapper;

@Converter
public class SeatMapConverter implements AttributeConverter<SeatMapVenue, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Override
    public String convertToDatabaseColumn(SeatMapVenue seatMap) {
        if (seatMap == null) return null;

        try {
            return MAPPER.writeValueAsString(seatMap);

        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao serializar o SeatMap", e);
        }
    }

    @Override
    public SeatMapVenue convertToEntityAttribute(String json) {
        if (json == null) return null;

        try {
            return MAPPER.readValue(json, SeatMapVenue.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao desserializar o SeatMap", e);
        }
    }
}
