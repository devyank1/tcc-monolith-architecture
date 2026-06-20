package com.yankdev.brtickets.event.service;

import com.yankdev.brtickets.event.dto.EventRequestDTO;
import com.yankdev.brtickets.event.dto.EventResponseDTO;
import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.repository.EventRepository;
import com.yankdev.brtickets.shared.exception.EventNotFoundException;
import com.yankdev.brtickets.shared.exception.VenueIsNotActiveException;
import com.yankdev.brtickets.shared.exception.VenueNotFoundException;
import com.yankdev.brtickets.user.model.UserModel;

import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;

    public EventService(VenueRepository venueRepository, EventRepository eventRepository) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
    }

    //findAll (name, date and type), update, cancel, publish
    public EventResponseDTO createEvent(EventRequestDTO request, UserModel adminLogged) {

        VenueModel venue = venueRepository.findById(request.getVenue().getVenueId())
                .orElseThrow(() -> new VenueNotFoundException("Venue not found."));

        if (!venue.isActive()) {
            throw new VenueIsNotActiveException("You cannot create event in an inactive venue.");
        }

        EventModel event = new EventModel();
        event.setVenue(venue);
        event.setCreatedBy(adminLogged);
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setDate(request.getDate());
        event.setEndDate(request.getEndDate());
        event.setType(request.getType());
        event.setArtist(request.getArtist());
        event.setStatus(EventStatusEnum.DRAFT);
        event.setAgeRate(request.getAgeRate());
        event.setSalesStartAt(request.getSalesStartAt());
        event.setSalesEndAt(request.getSalesEndAt());

        EventModel newEvent = eventRepository.save(event);

        return EventResponseDTO.from(newEvent);
    }

    public EventResponseDTO findEventById(UUID eventId) {

        EventModel model = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found."));

        return EventResponseDTO.from(model);
    }

}
