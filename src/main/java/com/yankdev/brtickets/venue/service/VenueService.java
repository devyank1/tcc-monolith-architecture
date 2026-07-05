package com.yankdev.brtickets.venue.service;

import com.yankdev.brtickets.shared.exception.IllegalVenueCapacityException;
import com.yankdev.brtickets.shared.exception.VenueNotFoundException;
import com.yankdev.brtickets.venue.dto.VenueRequestDTO;
import com.yankdev.brtickets.venue.dto.VenueResponseDTO;
import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public VenueResponseDTO createVenue(VenueRequestDTO request) {

        if (request.getCapacity() < 1) {
            throw new IllegalVenueCapacityException("Venue needs to have at least 1 seat.");
        }

        VenueModel venue = new VenueModel();

        venue.setName(request.getName());
        venue.setDescription(request.getDescription());
        venue.setType(request.getType());
        venue.setStreet(request.getStreet());
        venue.setCity(request.getCity());
        venue.setState(request.getState());
        venue.setZipCode(request.getZipCode());
        venue.setCountry(request.getCountry());
        venue.setActive(request.isActive());
        venue.setCapacity(request.getCapacity());

        VenueModel newVenue = venueRepository.save(venue);

        return VenueResponseDTO.from(newVenue);

    }

    public VenueResponseDTO findVenueById(UUID venueId) {

        VenueModel venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found."));

        return VenueResponseDTO.from(venue);
    }

    public List<VenueResponseDTO> findAllActiveVenues(String city) {

        List<VenueModel> venues = venueRepository.findAllByCityAndIsActiveTrue(city);

        return venues.stream()
                .map(VenueResponseDTO::from)
                .toList();
    }

    public VenueResponseDTO updateVenue(UUID venueId, VenueRequestDTO request) {

        VenueModel venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found."));

        venue.setName(request.getName());
        venue.setDescription(request.getDescription());
        venue.setType(request.getType());
        venue.setStreet(request.getStreet());
        venue.setCity(request.getCity());
        venue.setState(request.getState());
        venue.setZipCode(request.getZipCode());
        venue.setCountry(request.getCountry());
        venue.setActive(request.isActive());
        venue.setCapacity(request.getCapacity());

        VenueModel updatedVenue = venueRepository.save(venue);

        return VenueResponseDTO.from(updatedVenue);
    }

    public void deactivateVenue(UUID venueId) {

        VenueModel venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found."));

        venue.setActive(false);
        venueRepository.save(venue);
    }
}
