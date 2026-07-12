package com.yankdev.brtickets.event.controller;

import com.yankdev.brtickets.event.dto.EventRequestDTO;
import com.yankdev.brtickets.event.dto.EventResponseDTO;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
import com.yankdev.brtickets.event.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Tag(name = "Create Event", description = "Create a new event")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponseDTO> createEvent(@RequestBody EventRequestDTO request) {

        EventResponseDTO event = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @GetMapping
    @Tag(name = "Get All Events", description = "Retrieve all events")
    public ResponseEntity<List<EventResponseDTO>> findAllEvents() {

        List<EventResponseDTO> events = eventService.findAllEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping(params = "name")
    @Tag(name = "Find Event by Name", description = "Retrieve events by name")
    public ResponseEntity<List<EventResponseDTO>> findEventByName(@RequestParam String name) {

        List<EventResponseDTO> events = eventService.findEventByName(name);
        return ResponseEntity.ok(events);
    }

    @GetMapping(params = "type")
    @Tag(name = "Find Event by Type", description = "Retrieve events by type")
    public ResponseEntity<List<EventResponseDTO>> findEventByType(@RequestParam EventTypeEnum type) {

        List<EventResponseDTO> events = eventService.findEventByType(type);
        return ResponseEntity.ok(events);
    }

    @GetMapping(params = "city")
    @Tag(name = "Find Event by City", description = "Retrieve events by city")
    public ResponseEntity<List<EventResponseDTO>> findEventByCity(@RequestParam String city) {

        List<EventResponseDTO> events = eventService.findEventByCity(city);
        return ResponseEntity.ok(events);
    }

    @GetMapping(params = "date")
    @Tag(name = "Find Event by Date", description = "Retrieve events by date")
    public ResponseEntity<List<EventResponseDTO>> findEventByDate(@RequestParam LocalDateTime start, @RequestParam LocalDateTime end) {

        List<EventResponseDTO> events = eventService.findEventByDate(start, end);
        return ResponseEntity.ok(events);
    }

    @PatchMapping("/{eventId}")
    @Tag(name = "Update Event", description = "Update details of a specific event")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponseDTO> updateEvent(@PathVariable UUID eventId, @RequestBody EventRequestDTO request) {

        EventResponseDTO event = eventService.updateEvent(eventId,request);
        return ResponseEntity.ok(event);
    }

    @PostMapping("/{eventId}/publish")
    @Tag(name = "Publish Event", description = "Publish a specific event")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponseDTO> publishEvent(@PathVariable UUID eventId) {

        EventResponseDTO event = eventService.publishEvent(eventId);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/{eventId}")
    @Tag(name = "Delete Event", description = "Cancel a specific event")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {

        eventService.cancelEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
