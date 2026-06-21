package com.yankdev.brtickets.event.service;

import com.yankdev.brtickets.event.dto.EventRequestDTO;
import com.yankdev.brtickets.event.dto.EventResponseDTO;
import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
import com.yankdev.brtickets.event.repository.EventRepository;
import com.yankdev.brtickets.shared.exception.EventNotFoundException;
import com.yankdev.brtickets.shared.exception.IllegalEventStateException;
import com.yankdev.brtickets.shared.exception.VenueIsNotActiveException;
import com.yankdev.brtickets.shared.exception.VenueNotFoundException;
import com.yankdev.brtickets.user.model.UserModel;

import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;

    public EventService(VenueRepository venueRepository, EventRepository eventRepository) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
    }

    //publish
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
        event.setCreatedAt(LocalDateTime.now());

        EventModel newEvent = eventRepository.save(event);

        return EventResponseDTO.from(newEvent);
    }

    public List<EventResponseDTO> findAllEvents() {

        List<EventModel> model = eventRepository.findAll();

        return model.stream()
                .map(EventResponseDTO::from)
                .toList();
    }

    public List<EventResponseDTO> findEventByName(String name) {

        List<EventModel> model = eventRepository.findAllByName(name);

        return model.stream()
                .map(EventResponseDTO::from)
                .toList();
    }

    public List<EventResponseDTO> findEventByType(EventTypeEnum type) {

        List<EventModel> model = eventRepository.findAllByType(type);

        return model.stream()
                .map(EventResponseDTO::from)
                .toList();
    }

    public EventResponseDTO updateEvent(UUID eventId, EventRequestDTO request) {

        EventModel event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found."));

        event.setVenue(request.getVenue());
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setDate(request.getDate());
        event.setEndDate(request.getEndDate());
        event.setType(request.getType());
        event.setArtist(request.getArtist());
        event.setAgeRate(request.getAgeRate());
        event.setSalesStartAt(request.getSalesStartAt());
        event.setSalesEndAt(request.getSalesEndAt());

        EventModel eventUpdated = eventRepository.save(event);

        return EventResponseDTO.from(eventUpdated);
    }

    public EventResponseDTO publishEvent(UUID eventId) {

        EventModel event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found."));

        if (event.getStatus() != EventStatusEnum.DRAFT) {
            throw new IllegalEventStateException("Event state is wrong.");
        } else {
            event.setStatus(EventStatusEnum.PUBLISHED);
        }

        EventModel newPublishedEvent = eventRepository.save(event);

        return EventResponseDTO.from(newPublishedEvent);
    }

    public void cancelEvent(UUID eventId) {

        EventModel deactivateEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found."));

        if (deactivateEvent.getStatus() == EventStatusEnum.CANCELLED) {
            throw new IllegalEventStateException("You cannot cancel a CANCELLED event.");
        }

        deactivateEvent.setStatus(EventStatusEnum.CANCELLED);
        eventRepository.save(deactivateEvent);
    }
}
