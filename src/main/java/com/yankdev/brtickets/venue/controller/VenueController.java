package com.yankdev.brtickets.venue.controller;

import com.yankdev.brtickets.venue.dto.VenueRequestDTO;
import com.yankdev.brtickets.venue.dto.VenueResponseDTO;
import com.yankdev.brtickets.venue.service.VenueService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Tag(name = "Create Venue", description = "Creation of a new venue")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VenueResponseDTO> createVenue(@RequestBody VenueRequestDTO request) {

        VenueResponseDTO venue = venueService.createVenue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(venue);
    }

    @GetMapping("/{venueId}")
    @Tag(name = "Find Venues By ID", description = "Retrieve details of a specific venue")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VenueResponseDTO> findVenueById(@PathVariable UUID venueId) {

        VenueResponseDTO venue = venueService.findVenueById(venueId);
        return ResponseEntity.ok(venue);
    }

    @GetMapping
    @Tag(name = "Find All Venues", description = "Retrieve a list of all active venues in a specific city")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<VenueResponseDTO>> findAllVenues(@RequestParam String city) {

        List<VenueResponseDTO> venues = venueService.findAllActiveVenues(city);
        return ResponseEntity.ok(venues);
    }

    @PatchMapping("/{venueId}")
    @Tag(name = "Update Venue", description = "Update details of a specific venue")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VenueResponseDTO> updateVenue(@PathVariable UUID venueId, @RequestBody VenueRequestDTO request) {

        VenueResponseDTO venue = venueService.updateVenue(venueId, request);
        return ResponseEntity.ok(venue);
    }

    @DeleteMapping("/{venueId}")
    @Tag(name = "Delete Venue", description = "Deactivate a specific venue")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVenue(@PathVariable UUID venueId) {

        venueService.deactivateVenue(venueId);
        return ResponseEntity.noContent().build();
    }
}
