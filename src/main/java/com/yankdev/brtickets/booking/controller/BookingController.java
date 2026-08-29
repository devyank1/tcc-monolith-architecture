package com.yankdev.brtickets.booking.controller;

import com.yankdev.brtickets.booking.dto.BookingRequestDTO;
import com.yankdev.brtickets.booking.dto.BookingResponseDTO;
import com.yankdev.brtickets.booking.service.BookingService;
import com.yankdev.brtickets.shared.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public BookingController(BookingService bookingService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.bookingService = bookingService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping
    @Tag(name = "Create Booking", description = "Create a new booking")
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestBody BookingRequestDTO request) {

        UUID userId = authenticatedUserProvider.getCurrentUserId();
        BookingResponseDTO booking = bookingService.createBooking(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/{bookingId}")
    @Tag(name = "Get Booking", description = "Retrieve details of a specific booking")
    public ResponseEntity<BookingResponseDTO> findBookingById(@PathVariable UUID bookingId) {

        BookingResponseDTO booking = bookingService.findBookingById(bookingId);
        return ResponseEntity.ok(booking);
    }

    @GetMapping
    @Tag(name = "Find Bookings by User", description = "Retrieve all bookings for a specific user")
    public ResponseEntity<List<BookingResponseDTO>> findAllBookingByUser() {

        UUID userId = authenticatedUserProvider.getCurrentUserId();
        List<BookingResponseDTO> allBookings = bookingService.findAllByUser(userId);
        return ResponseEntity.ok(allBookings);
    }

    @DeleteMapping("/{bookingId}")
    @Tag(name = "Cancel Booking", description = "Cancel a specific booking")
    public ResponseEntity<Void> cancelBooking(@PathVariable UUID bookingId) {

        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }
}
