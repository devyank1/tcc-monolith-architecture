package com.yankdev.brtickets.ticket.controller;

import com.yankdev.brtickets.ticket.dto.TicketRequestDTO;
import com.yankdev.brtickets.ticket.dto.TicketResponseDTO;
import com.yankdev.brtickets.ticket.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@RequestBody TicketRequestDTO request) {

        TicketResponseDTO ticket = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDTO> findTicketById(@PathVariable UUID ticketId) {

        TicketResponseDTO ticket = ticketService.findTicketById(ticketId);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> findAllTicketsByEvent(@RequestParam UUID eventId) {

        List<TicketResponseDTO> tickets = ticketService.findAllTicketsByEvent(eventId);
        return ResponseEntity.ok(tickets);
    }

    @PutMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDTO> updateTicket(@PathVariable UUID ticketId, @RequestBody TicketRequestDTO request) {

        TicketResponseDTO ticket = ticketService.updateTicket(request, ticketId);
        return ResponseEntity.ok(ticket);
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(@PathVariable UUID ticketId) {

        ticketService.deactivateTicket(ticketId);
        return ResponseEntity.noContent().build();
    }

}
