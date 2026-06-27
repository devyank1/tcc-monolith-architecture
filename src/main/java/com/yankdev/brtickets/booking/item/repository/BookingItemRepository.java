package com.yankdev.brtickets.booking.item.repository;

import com.yankdev.brtickets.booking.item.model.BookingItemModel;
import com.yankdev.brtickets.booking.model.BookingModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItemModel, UUID> {
    List<BookingModel> findAllByBooking_BookingId(UUID bookingId);
}
