package com.yankdev.brtickets.ticket.service;

import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.repository.EventRepository;
import com.yankdev.brtickets.shared.exception.EventNotFoundException;
import com.yankdev.brtickets.shared.exception.TicketNotFoundException;
import com.yankdev.brtickets.ticket.dto.TicketRequestDTO;
import com.yankdev.brtickets.ticket.dto.TicketResponseDTO;
import com.yankdev.brtickets.ticket.model.TicketModel;
import com.yankdev.brtickets.ticket.model.enums.TicketStatusEnum;
import com.yankdev.brtickets.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    public TicketService(TicketRepository ticketRepository, EventRepository eventRepository) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }

    public TicketResponseDTO createTicket(TicketRequestDTO request) {

        TicketModel ticket = new TicketModel();

        EventModel event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EventNotFoundException("Event not found."));
        ticket.setEvent(event);

        ticket.setSector(request.getSector());
        ticket.setRow(request.getRow());
        ticket.setSeat(request.getSeat());
        ticket.setPrice(request.getPrice());
        ticket.setStatus(TicketStatusEnum.AVAILABLE);
        ticket.setType(request.getType());
        ticket.setQrCode(UUID.randomUUID().toString());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(null);

        TicketModel newTicket = ticketRepository.save(ticket);

        return TicketResponseDTO.from(newTicket);
    }

    public TicketResponseDTO findTicketById(UUID ticketId) {

        TicketModel ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found by ID."));

        return TicketResponseDTO.from(ticket);
    }

    public List<TicketResponseDTO> findAllTicketsByEvent(UUID eventId) {

        List<TicketModel> allTickets = ticketRepository.findAllByEvent_EventId(eventId);

        return allTickets.stream()
                .map(TicketResponseDTO::from)
                .toList();
    }

    public TicketResponseDTO updateTicket(TicketRequestDTO request, UUID ticketId) {

        TicketModel ticket  = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found."));

        if (request.getSector() != null) ticket.setSector(request.getSector());
        if (request.getRow() != null) ticket.setRow(request.getRow());
        if (request.getSeat() != null) ticket.setSeat(request.getSeat());
        if (request.getUpdatedAt() != null) ticket.setUpdatedAt(LocalDateTime.now());

        TicketModel updatedTicket = ticketRepository.save(ticket);
        return TicketResponseDTO.from(updatedTicket);

    }

    public void deactivateTicket(UUID ticketId) {

        TicketModel ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));

        ticket.setStatus(TicketStatusEnum.CANCELLED);
        ticketRepository.save(ticket);
    }
}
