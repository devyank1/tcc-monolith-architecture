package com.yankdev.brtickets.event.controller;

import com.yankdev.brtickets.event.dto.EventRequestDTO;
import com.yankdev.brtickets.event.dto.EventResponseDTO;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
import com.yankdev.brtickets.event.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<EventResponseDTO> createEvent(@RequestBody EventRequestDTO request, @RequestParam UUID adminId) {

        EventResponseDTO event = eventService.createEvent(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDTO>> findAllEvents() {

        List<EventResponseDTO> events = eventService.findAllEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping(params = "name")
    public ResponseEntity<List<EventResponseDTO>> findEventByName(@RequestParam String name) {

        List<EventResponseDTO> events = eventService.findEventByName(name);
        return ResponseEntity.ok(events);
    }

    @GetMapping(params = "type")
    public ResponseEntity<List<EventResponseDTO>> findEventByType(@RequestParam EventTypeEnum type) {

        List<EventResponseDTO> events = eventService.findEventByType(type);
        return ResponseEntity.ok(events);
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponseDTO> updateEvent(@PathVariable UUID eventId, @RequestBody EventRequestDTO request) {

        EventResponseDTO event = eventService.updateEvent(eventId,request);
        return ResponseEntity.ok(event);
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<EventResponseDTO> publishEvent(@PathVariable UUID eventId) {

        EventResponseDTO event = eventService.publishEvent(eventId);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {

        eventService.cancelEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
