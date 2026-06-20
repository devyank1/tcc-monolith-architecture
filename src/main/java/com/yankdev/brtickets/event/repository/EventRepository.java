package com.yankdev.brtickets.event.repository;

import com.yankdev.brtickets.event.model.EventModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventModel, UUID> {
    List<EventModel> allEvents(String name);
    List<EventModel> allEventsByDate(LocalDateTime date);
    List<EventModel> allEventsByType(String type);
}
