package com.yankdev.brtickets.booking.service;

import com.yankdev.brtickets.booking.dto.BookingRequestDTO;
import com.yankdev.brtickets.booking.dto.BookingResponseDTO;
import com.yankdev.brtickets.booking.item.repository.BookingItemRepository;
import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.booking.model.enums.BookingStatusEnum;
import com.yankdev.brtickets.booking.repository.BookingRepository;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import com.yankdev.brtickets.shared.exception.AccessDeniedException;
import com.yankdev.brtickets.shared.exception.BookingNotFoundException;
import com.yankdev.brtickets.shared.exception.IllegalBookingCancellingException;
import com.yankdev.brtickets.shared.exception.IllegalTicketOnBookingException;
import com.yankdev.brtickets.shared.exception.UserNotFoundException;
import com.yankdev.brtickets.shared.security.AuthenticatedUserProvider;
import com.yankdev.brtickets.ticket.model.TicketModel;
import com.yankdev.brtickets.ticket.model.enums.TicketStatusEnum;
import com.yankdev.brtickets.ticket.repository.TicketRepository;
import com.yankdev.brtickets.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingItemRepository bookingItemRepository;
    @Mock
    private AuthenticatedUserProvider userProvider;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, ticketRepository, userRepository, bookingItemRepository, userProvider);
    }

    private TicketModel availableTicket(UUID ticketId, BigDecimal price) {
        TicketModel ticket = new TicketModel();
        ticket.setTicketId(ticketId);
        ticket.setPrice(price);
        ticket.setStatus(TicketStatusEnum.AVAILABLE);
        return ticket;
    }

    @Test
    void createBooking_throwsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        BookingRequestDTO request = new BookingRequestDTO();
        request.setTicketsId(List.of(UUID.randomUUID()));

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_throwsWhenTicketIdsDoNotMatchFoundTickets() {
        UUID userId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        BookingRequestDTO request = new BookingRequestDTO();
        request.setTicketsId(List.of(ticketId, UUID.randomUUID()));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(ticketRepository.findAllByTicketIdIn(request.getTicketsId()))
                .thenReturn(List.of(availableTicket(ticketId, BigDecimal.TEN)));

        assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                .isInstanceOf(IllegalTicketOnBookingException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_throwsWhenAnyTicketIsNotAvailable() {
        UUID userId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        BookingRequestDTO request = new BookingRequestDTO();
        request.setTicketsId(List.of(ticketId));

        TicketModel bookedTicket = availableTicket(ticketId, BigDecimal.TEN);
        bookedTicket.setStatus(TicketStatusEnum.BOOKED);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(ticketRepository.findAllByTicketIdIn(request.getTicketsId()))
                .thenReturn(List.of(bookedTicket));

        assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                .isInstanceOf(IllegalTicketOnBookingException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_usesLockedRepositoryMethodNotFindAllById() {
        UUID userId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        BookingRequestDTO request = new BookingRequestDTO();
        request.setTicketsId(List.of(ticketId));
        request.setPaymentMethod(PaymentMethodEnum.PIX);

        TicketModel ticket = availableTicket(ticketId, BigDecimal.TEN);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(ticketRepository.findAllByTicketIdIn(request.getTicketsId())).thenReturn(List.of(ticket));
        when(bookingRepository.save(any(BookingModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.createBooking(userId, request);

        verify(ticketRepository, times(1)).findAllByTicketIdIn(request.getTicketsId());
        verify(ticketRepository, never()).findAllById(any());
    }

    @Test
    void createBooking_success_computesTotalAndMarksTicketsBooked() {
        UUID userId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        BookingRequestDTO request = new BookingRequestDTO();
        request.setTicketsId(List.of(ticketId));
        request.setPaymentMethod(PaymentMethodEnum.PIX);

        TicketModel ticket = availableTicket(ticketId, BigDecimal.valueOf(150));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(ticketRepository.findAllByTicketIdIn(request.getTicketsId())).thenReturn(List.of(ticket));
        when(bookingRepository.save(any(BookingModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponseDTO response = bookingService.createBooking(userId, request);

        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(ticket.getStatus()).isEqualTo(TicketStatusEnum.BOOKED);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void findBookingById_throwsWhenCallerIsNotOwner() {
        UUID bookingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        BookingModel booking = new BookingModel();
        booking.setBookingId(bookingId);
        booking.setUserId(ownerId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userProvider.getCurrentUserId()).thenReturn(callerId);

        assertThatThrownBy(() -> bookingService.findBookingById(bookingId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findBookingById_returnsBookingWhenCallerIsOwner() {
        UUID bookingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        BookingModel booking = new BookingModel();
        booking.setBookingId(bookingId);
        booking.setUserId(ownerId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userProvider.getCurrentUserId()).thenReturn(ownerId);

        BookingResponseDTO response = bookingService.findBookingById(bookingId);

        assertThat(response.getBookingId()).isEqualTo(bookingId);
    }

    @Test
    void findBookingById_throwsWhenBookingDoesNotExist() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findBookingById(bookingId))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void cancelBooking_throwsWhenCallerIsNotOwner() {
        UUID bookingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        BookingModel booking = new BookingModel();
        booking.setBookingId(bookingId);
        booking.setUserId(ownerId);
        booking.setStatus(BookingStatusEnum.CONFIRMED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userProvider.getCurrentUserId()).thenReturn(callerId);

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                .isInstanceOf(AccessDeniedException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_throwsWhenAlreadyCancelled() {
        UUID bookingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        BookingModel booking = new BookingModel();
        booking.setBookingId(bookingId);
        booking.setUserId(ownerId);
        booking.setStatus(BookingStatusEnum.CANCELLED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userProvider.getCurrentUserId()).thenReturn(ownerId);

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                .isInstanceOf(IllegalBookingCancellingException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_success_freesTicketsAndMarksCancelled() {
        UUID bookingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        BookingModel booking = new BookingModel();
        booking.setBookingId(bookingId);
        booking.setUserId(ownerId);
        booking.setStatus(BookingStatusEnum.CONFIRMED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userProvider.getCurrentUserId()).thenReturn(ownerId);
        when(bookingItemRepository.findAllByBooking_BookingId(bookingId)).thenReturn(List.of());

        bookingService.cancelBooking(bookingId);

        assertThat(booking.getStatus()).isEqualTo(BookingStatusEnum.CANCELLED);
        assertThat(booking.getCancelledAt()).isNotNull();
        verify(bookingRepository).save(booking);
    }
}
