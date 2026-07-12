package com.yankdev.brtickets.booking.controller;

import com.yankdev.brtickets.booking.dto.BookingRequestDTO;
import com.yankdev.brtickets.booking.dto.BookingResponseDTO;
import com.yankdev.brtickets.booking.service.BookingService;
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

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Tag(name = "Create Booking", description = "Create a new booking")
    public ResponseEntity<BookingResponseDTO> createBooking(@RequestParam UUID userId, @RequestBody BookingRequestDTO request) {

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
    public ResponseEntity<List<BookingResponseDTO>> findAllBookingByUser(@RequestParam UUID userId) {

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
