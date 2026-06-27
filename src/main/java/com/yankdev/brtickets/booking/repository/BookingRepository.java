package com.yankdev.brtickets.booking.repository;

import com.yankdev.brtickets.booking.model.BookingModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<BookingModel, UUID> {
    List<BookingModel> findAllByUser_UserId(UUID userId);
}
