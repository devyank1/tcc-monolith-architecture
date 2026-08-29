package com.yankdev.brtickets.ticket.repository;

import com.yankdev.brtickets.ticket.model.TicketModel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<TicketModel, UUID> {
    List<TicketModel> findAllByEvent_EventId(UUID eventId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TicketModel> findAllByTicketIdIn(List<UUID> ticketIds);
}
