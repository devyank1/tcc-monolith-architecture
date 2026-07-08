package com.yankdev.brtickets.event.service;

import com.yankdev.brtickets.event.dto.EventRequestDTO;
import com.yankdev.brtickets.event.dto.EventResponseDTO;
import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
import com.yankdev.brtickets.event.repository.EventRepository;
import com.yankdev.brtickets.shared.exception.*;
import com.yankdev.brtickets.user.model.UserModel;

import com.yankdev.brtickets.user.repository.UserRepository;
import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.repository.VenueRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(VenueRepository venueRepository, EventRepository eventRepository, UserRepository userRepository) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public EventResponseDTO createEvent(EventRequestDTO request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UserModel user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found by email"));

        VenueModel venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new VenueNotFoundException("Venue not found."));

        if (!venue.isActive()) {
            throw new VenueIsNotActiveException("You cannot create an event in an inactive venue.");
        }

        if (!user.isActive()) {
            throw new UserIsNotActiveException("You cannot create an event with an inactive user.");
        }

        EventModel event = new EventModel();
        event.setVenue(venue);
        event.setCreatedBy(user);
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

    public List<EventResponseDTO> findEventByCity(String city) {

        List<EventModel> event = eventRepository.findAllByVenue_City(city);
        return event.stream()
                .map(EventResponseDTO::from)
                .toList();
    }

    public List<EventResponseDTO> findEventByDate(LocalDateTime start, LocalDateTime end) {

        List<EventModel> events = eventRepository.findAllByDateBetween(start, end);
        return events.stream()
                .map(EventResponseDTO::from)
                .toList();
    }

    public EventResponseDTO updateEvent(UUID eventId, EventRequestDTO request) {

        EventModel event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found."));

        if (request.getVenueId() != null) {
            VenueModel venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new VenueNotFoundException("Venue not found"));
            if (!venue.isActive()) {
                throw new VenueIsNotActiveException("You cannot update event in an inactive venue.");
            }
            event.setVenue(venue);
        }

        if (request.getName() != null) event.setName(request.getName());
        if (request.getDescription() != null ) event.setDescription(request.getDescription());
        if (request.getDate() != null) event.setDate(request.getDate());
        if (request.getEndDate() != null) event.setEndDate(request.getEndDate());
        if (request.getType() != null) event.setType(request.getType());
        if (request.getArtist() != null) event.setArtist(request.getArtist());
        if (request.getAgeRate() != null) event.setAgeRate(request.getAgeRate());
        if (request.getSalesStartAt() != null) event.setSalesStartAt(request.getSalesStartAt());
        if (request.getSalesEndAt() != null) event.setSalesEndAt(request.getSalesEndAt());

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

        if (deactivateEvent.getStatus() == EventStatusEnum.FINISHED) {
            throw new IllegalEventStateException("You cannot cancel a FINISHED event.");
        }

        if (deactivateEvent.getStatus() == EventStatusEnum.CANCELLED) {
            throw new IllegalEventStateException("This event is already cancelled.");
        }

        deactivateEvent.setStatus(EventStatusEnum.CANCELLED);
        eventRepository.save(deactivateEvent);
    }
}
