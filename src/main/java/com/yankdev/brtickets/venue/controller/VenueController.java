package com.yankdev.brtickets.venue.controller;

import com.yankdev.brtickets.venue.dto.VenueRequestDTO;
import com.yankdev.brtickets.venue.dto.VenueResponseDTO;
import com.yankdev.brtickets.venue.service.VenueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping()
    public ResponseEntity<VenueResponseDTO> createVenue(@RequestBody VenueRequestDTO request) {

        VenueResponseDTO venue = venueService.createVenue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(venue);
    }

    @GetMapping("/{venueId}")
    public ResponseEntity<VenueResponseDTO> findVenueById(@PathVariable UUID venueId) {

        VenueResponseDTO venue = venueService.findVenueById(venueId);
        return ResponseEntity.ok(venue);
    }

    @GetMapping
    public ResponseEntity<List<VenueResponseDTO>> findAllVenues(@RequestParam String city) {

        List<VenueResponseDTO> venues = venueService.findAllActiveVenues(city);
        return ResponseEntity.ok(venues);
    }

    @PatchMapping("/{venueId}")
    public ResponseEntity<VenueResponseDTO> updateVenue(@PathVariable UUID venueId, @RequestBody VenueRequestDTO request) {

        VenueResponseDTO venue = venueService.updateVenue(venueId, request);
        return ResponseEntity.ok(venue);
    }

    @DeleteMapping("/{venueId}")
    public ResponseEntity<Void> deleteVenue(@PathVariable UUID venueId) {

        venueService.deactivateVenue(venueId);
        return ResponseEntity.noContent().build();
    }
}
