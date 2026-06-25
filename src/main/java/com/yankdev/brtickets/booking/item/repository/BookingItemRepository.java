package com.yankdev.brtickets.booking.item.repository;

import com.yankdev.brtickets.booking.item.model.BookingItemModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItemModel, UUID> {
}
