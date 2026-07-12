package com.yankdev.brtickets.ticket.controller;

import com.yankdev.brtickets.ticket.dto.TicketRequestDTO;
import com.yankdev.brtickets.ticket.dto.TicketResponseDTO;
import com.yankdev.brtickets.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Tag(name = "Create Ticket", description = "Create a new ticket")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponseDTO> createTicket(@RequestBody TicketRequestDTO request) {

        TicketResponseDTO ticket = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping("/{ticketId}")
    @Tag(name = "Find Ticket By ID", description = "Retrieve details of a specific ticket")
    @SecurityRequirement(name = "bearerAuth")
        public ResponseEntity<TicketResponseDTO> findTicketById(@PathVariable UUID ticketId) {

        TicketResponseDTO ticket = ticketService.findTicketById(ticketId);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping
    @Tag(name = "Find All Tickets by Event", description = "Retrieve a list of all tickets for a specific event")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<TicketResponseDTO>> findAllTicketsByEvent(@RequestParam UUID eventId) {

        List<TicketResponseDTO> tickets = ticketService.findAllTicketsByEvent(eventId);
        return ResponseEntity.ok(tickets);
    }

    @PatchMapping("/{ticketId}")
    @Tag(name = "Update Ticket", description = "Update details of a specific ticket")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponseDTO> updateTicket(@PathVariable UUID ticketId, @RequestBody TicketRequestDTO request) {

        TicketResponseDTO ticket = ticketService.updateTicket(request, ticketId);
        return ResponseEntity.ok(ticket);
    }

    @DeleteMapping("/{ticketId}")
    @Tag(name = "Delete Ticket", description = "Deactivate a specific ticket")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTicket(@PathVariable UUID ticketId) {

        ticketService.deactivateTicket(ticketId);
        return ResponseEntity.noContent().build();
    }
}
