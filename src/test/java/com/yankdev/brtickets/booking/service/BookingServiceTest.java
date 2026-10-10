package com.yankdev.brtickets.booking.service;

import com.yankdev.brtickets.booking.dto.BookingRequestDTO;
import com.yankdev.brtickets.booking.dto.BookingResponseDTO;
import com.yankdev.brtickets.booking.item.model.BookingItemModel;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private BookingService bookingService;

    @Captor
    private ArgumentCaptor<BookingModel> bookingCaptor;

    @Captor
    private ArgumentCaptor<TicketModel> ticketCaptor;

    private UUID userId;
    private TicketModel firstTicket;
    private TicketModel secondTicket;
    private BookingRequestDTO request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        firstTicket = ticket(new BigDecimal("150.50"), TicketStatusEnum.AVAILABLE);
        secondTicket = ticket(new BigDecimal("99.50"), TicketStatusEnum.AVAILABLE);

        request = new BookingRequestDTO();
        request.setTicketsId(List.of(firstTicket.getTicketId(), secondTicket.getTicketId()));
        request.setPaymentMethod(PaymentMethodEnum.PIX);
    }

    private TicketModel ticket(BigDecimal price, TicketStatusEnum status) {
        TicketModel ticket = new TicketModel();
        ticket.setTicketId(UUID.randomUUID());
        ticket.setPrice(price);
        ticket.setStatus(status);
        return ticket;
    }

    private BookingModel booking(UUID owner, BookingStatusEnum status) {
        BookingModel booking = new BookingModel();
        booking.setBookingId(UUID.randomUUID());
        booking.setUserId(owner);
        booking.setStatus(status);
        booking.setTotalAmount(new BigDecimal("250.00"));
        booking.setPaymentMethod(PaymentMethodEnum.PIX);
        return booking;
    }

    @Nested
    @DisplayName("createBooking")
    class CreateBooking {

        @Test
        @DisplayName("confirms the booking and sums the ticket prices")
        void createsBookingWithTotalAmount() {
            when(userRepository.existsById(userId)).thenReturn(true);
            when(ticketRepository.findAllByTicketIdIn(request.getTicketsId()))
                    .thenReturn(List.of(firstTicket, secondTicket));
            when(bookingRepository.save(any(BookingModel.class))).thenAnswer(call -> call.getArgument(0));

            BookingResponseDTO response = bookingService.createBooking(userId, request);

            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getStatus()).isEqualTo(BookingStatusEnum.CONFIRMED);
            assertThat(response.getTotalAmount()).isEqualByComparingTo("250.00");
            assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethodEnum.PIX);
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getConfirmedAt()).isNotNull();
            assertThat(response.getCancelledAt()).isNull();
        }

        @Test
        @DisplayName("marks every ticket of the booking as BOOKED")
        void marksTicketsAsBooked() {
            when(userRepository.existsById(userId)).thenReturn(true);
            when(ticketRepository.findAllByTicketIdIn(request.getTicketsId()))
                    .thenReturn(List.of(firstTicket, secondTicket));
            when(bookingRepository.save(any(BookingModel.class))).thenAnswer(call -> call.getArgument(0));

            bookingService.createBooking(userId, request);

            verify(ticketRepository, times(2)).save(ticketCaptor.capture());
            assertThat(ticketCaptor.getAllValues())
                    .extracting(TicketModel::getStatus)
                    .containsOnly(TicketStatusEnum.BOOKED);
        }

        @Test
        @DisplayName("creates one booking item per ticket, with its unit price")
        void createsOneItemPerTicket() {
            when(userRepository.existsById(userId)).thenReturn(true);
            when(ticketRepository.findAllByTicketIdIn(request.getTicketsId()))
                    .thenReturn(List.of(firstTicket, secondTicket));
            when(bookingRepository.save(any(BookingModel.class))).thenAnswer(call -> call.getArgument(0));

            bookingService.createBooking(userId, request);

            ArgumentCaptor<BookingItemModel> itemCaptor = ArgumentCaptor.forClass(BookingItemModel.class);
            verify(bookingItemRepository, times(2)).save(itemCaptor.capture());
            assertThat(itemCaptor.getAllValues())
                    .extracting(BookingItemModel::getUnitPrice)
                    .containsExactly(new BigDecimal("150.50"), new BigDecimal("99.50"));
        }

        @Test
        @DisplayName("fails when the user does not exist")
        void failsWhenUserDoesNotExist() {
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found on this booking");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when one of the requested tickets does not exist")
        void failsWhenTicketIsMissing() {
            when(userRepository.existsById(userId)).thenReturn(true);
            when(ticketRepository.findAllByTicketIdIn(request.getTicketsId()))
                    .thenReturn(List.of(firstTicket));

            assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                    .isInstanceOf(IllegalTicketOnBookingException.class)
                    .hasMessage("One or more tickets are wrong");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when one of the tickets is not AVAILABLE")
        void failsWhenTicketIsNotAvailable() {
            secondTicket.setStatus(TicketStatusEnum.BOOKED);
            when(userRepository.existsById(userId)).thenReturn(true);
            when(ticketRepository.findAllByTicketIdIn(request.getTicketsId()))
                    .thenReturn(List.of(firstTicket, secondTicket));

            assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                    .isInstanceOf(IllegalTicketOnBookingException.class)
                    .hasMessage("One or more tickets are not available.");

            verify(bookingRepository, never()).save(any());
            verify(bookingItemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findBookingById / findAllByUser")
    class Queries {

        @Test
        @DisplayName("returns the booking of the authenticated owner")
        void findsOwnBooking() {
            BookingModel booking = booking(userId, BookingStatusEnum.CONFIRMED);
            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            BookingResponseDTO response = bookingService.findBookingById(booking.getBookingId());

            assertThat(response.getBookingId()).isEqualTo(booking.getBookingId());
            assertThat(response.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("denies reading a booking that belongs to another user")
        void deniesOtherUsersBooking() {
            BookingModel booking = booking(UUID.randomUUID(), BookingStatusEnum.CONFIRMED);
            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> bookingService.findBookingById(booking.getBookingId()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You have not permission to find bookings");
        }

        @Test
        @DisplayName("fails when the booking does not exist")
        void failsWhenBookingDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(bookingRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.findBookingById(unknownId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessage("Booking not found");
        }

        @Test
        @DisplayName("returns every booking of a user")
        void findsAllBookingsByUser() {
            BookingModel first = booking(userId, BookingStatusEnum.CONFIRMED);
            BookingModel second = booking(userId, BookingStatusEnum.CANCELLED);
            when(bookingRepository.findAllByUserId(userId)).thenReturn(List.of(first, second));

            assertThat(bookingService.findAllByUser(userId)).hasSize(2)
                    .extracting(BookingResponseDTO::getStatus)
                    .containsExactly(BookingStatusEnum.CONFIRMED, BookingStatusEnum.CANCELLED);
        }

        @Test
        @DisplayName("returns an empty list when the user has no booking")
        void findsNoBookingForUser() {
            when(bookingRepository.findAllByUserId(userId)).thenReturn(List.of());

            assertThat(bookingService.findAllByUser(userId)).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("cancels the booking, stamps cancelledAt and releases the tickets")
        void cancelsBookingAndReleasesTickets() {
            BookingModel booking = booking(userId, BookingStatusEnum.CONFIRMED);
            firstTicket.setStatus(TicketStatusEnum.BOOKED);
            secondTicket.setStatus(TicketStatusEnum.BOOKED);

            BookingItemModel firstItem = new BookingItemModel();
            firstItem.setBooking(booking);
            firstItem.setTicket(firstTicket);
            BookingItemModel secondItem = new BookingItemModel();
            secondItem.setBooking(booking);
            secondItem.setTicket(secondTicket);

            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);
            when(bookingItemRepository.findAllByBooking_BookingId(booking.getBookingId()))
                    .thenReturn(List.of(firstItem, secondItem));

            bookingService.cancelBooking(booking.getBookingId());

            verify(bookingRepository).save(bookingCaptor.capture());
            assertThat(bookingCaptor.getValue().getStatus()).isEqualTo(BookingStatusEnum.CANCELLED);
            assertThat(bookingCaptor.getValue().getCancelledAt()).isNotNull();

            verify(ticketRepository, times(2)).save(ticketCaptor.capture());
            assertThat(ticketCaptor.getAllValues())
                    .extracting(TicketModel::getStatus)
                    .containsOnly(TicketStatusEnum.AVAILABLE);
        }

        @Test
        @DisplayName("denies cancelling a booking that belongs to another user")
        void deniesCancellingOtherUsersBooking() {
            BookingModel booking = booking(UUID.randomUUID(), BookingStatusEnum.CONFIRMED);
            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> bookingService.cancelBooking(booking.getBookingId()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You have not access to cancel this booking");

            verify(bookingRepository, never()).save(any());
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses to cancel a booking that is already CANCELLED")
        void refusesAlreadyCancelledBooking() {
            BookingModel booking = booking(userId, BookingStatusEnum.CANCELLED);
            when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
            when(userProvider.getCurrentUserId()).thenReturn(userId);

            assertThatThrownBy(() -> bookingService.cancelBooking(booking.getBookingId()))
                    .isInstanceOf(IllegalBookingCancellingException.class)
                    .hasMessage("You cannot cancel a CANCELLED booking");

            verify(bookingRepository, never()).save(any());
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the booking does not exist")
        void failsWhenBookingDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(bookingRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.cancelBooking(unknownId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessage("Booking not found.");

            verify(bookingRepository, never()).save(any());
        }
    }
}
