package com.yankdev.brtickets.booking.service;

import com.yankdev.brtickets.booking.dto.BookingRequestDTO;
import com.yankdev.brtickets.booking.dto.BookingResponseDTO;
import com.yankdev.brtickets.booking.item.model.BookingItemModel;
import com.yankdev.brtickets.booking.item.repository.BookingItemRepository;
import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.booking.model.enums.BookingStatusEnum;
import com.yankdev.brtickets.booking.repository.BookingRepository;
import com.yankdev.brtickets.shared.exception.BookingNotFoundException;
import com.yankdev.brtickets.shared.exception.IllegalBookingCancellingException;
import com.yankdev.brtickets.shared.exception.IllegalTicketOnBookingException;
import com.yankdev.brtickets.shared.exception.UserNotFoundException;
import com.yankdev.brtickets.ticket.model.TicketModel;
import com.yankdev.brtickets.ticket.model.enums.TicketStatusEnum;
import com.yankdev.brtickets.ticket.repository.TicketRepository;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final BookingItemRepository bookingItemRepository;

    public BookingService(BookingRepository bookingRepository, TicketRepository ticketRepository, UserRepository userRepository, BookingItemRepository bookingItemRepository) {
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.bookingItemRepository = bookingItemRepository;
    }

    public BookingResponseDTO createBooking(UUID userId, BookingRequestDTO request) {

        UserModel user  = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found on this booking."));

        List<TicketModel> tickets = ticketRepository.findAllById(request.getTicketsId());

        if (tickets.size() != request.getTicketsId().size()) {
            throw new IllegalTicketOnBookingException("One or more tickets are wrong");
        }

        boolean hasUnavailable = tickets.stream()
                .anyMatch(ticket -> ticket.getStatus() != TicketStatusEnum.AVAILABLE);

        if (hasUnavailable) {
            throw new IllegalTicketOnBookingException("One or more tickets are not available.");
        }

        BigDecimal total = tickets.stream()
                .map(TicketModel::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BookingModel booking = new BookingModel();
        booking.setUser(user);
        booking.setStatus(BookingStatusEnum.CONFIRMED);
        booking.setTotalAmount(total);
        booking.setPaymentMethod(request.getPaymentMethod());
        booking.setCreatedAt(LocalDateTime.now(Clock.systemDefaultZone()));
        booking.setConfirmedAt(LocalDateTime.now(Clock.systemDefaultZone()));
        booking.setCancelledAt(null);

        BookingModel newBooking = bookingRepository.save(booking);

        for (TicketModel newTicket : tickets) {
            BookingItemModel bookingItem = new BookingItemModel();

            bookingItem.setBooking(newBooking);
            bookingItem.setTicket(newTicket);
            bookingItem.setUnitPrice(newTicket.getPrice());

            bookingItemRepository.save(bookingItem);
        }

        for (TicketModel ticket : tickets) {
            ticket.setStatus(TicketStatusEnum.BOOKED);

            ticketRepository.save(ticket);
        }

        return BookingResponseDTO.from(newBooking);
    }

    public BookingResponseDTO findBookingById(UUID bookingId) {

        BookingModel booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        return BookingResponseDTO.from(booking);
    }

    public List<BookingResponseDTO> findAllByUser(UUID userId) {

        List<BookingModel> bookings = bookingRepository.findAllByUser_UserId(userId);

        return bookings.stream()
                .map(BookingResponseDTO::from)
                .toList();
    }

    public void cancelBooking(UUID bookingId) {

        BookingModel booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        if (booking.getStatus() == BookingStatusEnum.CANCELLED) {
            throw new IllegalBookingCancellingException("You cannot cancel a CANCELLED booking");
        }

        booking.setStatus(BookingStatusEnum.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now(Clock.systemDefaultZone()));

        List<BookingItemModel> items = bookingItemRepository.findAllByBooking_BookingId(bookingId);

        for (BookingItemModel item: items) {

            TicketModel ticket = item.getTicket();
            ticket.setStatus(TicketStatusEnum.AVAILABLE);
            ticketRepository.save(ticket);

        }
        bookingRepository.save(booking);
    }
}
