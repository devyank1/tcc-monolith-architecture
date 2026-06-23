package com.yankdev.brtickets.ticket.repository;

import com.yankdev.brtickets.ticket.model.TicketModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<TicketModel, UUID> {
    List<TicketModel> allTicketsByEvent(UUID eventId);
}
