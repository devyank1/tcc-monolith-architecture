package com.yankdev.brtickets.booking.item.repository;

import com.yankdev.brtickets.TestContainersConfiguration;
import com.yankdev.brtickets.booking.item.model.BookingItemModel;
import com.yankdev.brtickets.booking.model.BookingModel;
import com.yankdev.brtickets.booking.model.enums.BookingStatusEnum;
import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import com.yankdev.brtickets.ticket.model.TicketModel;
import com.yankdev.brtickets.ticket.model.enums.TicketStatusEnum;
import com.yankdev.brtickets.ticket.model.enums.TicketTypeEnum;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.model.enums.VenueEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestContainersConfiguration.class)
class BookingItemRepositoryTest {

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    private BookingModel booking;
    private BookingModel otherBooking;
    private TicketModel firstTicket;

    @BeforeEach
    void setUp() {
        UserModel creator = entityManager.persistAndFlush(user());
        VenueModel venue = entityManager.persistAndFlush(venue());
        EventModel event = entityManager.persistAndFlush(event(creator, venue));

        firstTicket = entityManager.persistAndFlush(ticket(event, "1", "150.50"));
        TicketModel secondTicket = entityManager.persistAndFlush(ticket(event, "2", "99.50"));
        TicketModel thirdTicket = entityManager.persistAndFlush(ticket(event, "3", "80.00"));

        booking = entityManager.persistAndFlush(booking("250.00"));
        otherBooking = entityManager.persistAndFlush(booking("80.00"));

        entityManager.persistAndFlush(item(booking, firstTicket));
        entityManager.persistAndFlush(item(booking, secondTicket));
        entityManager.persistAndFlush(item(otherBooking, thirdTicket));
    }

    private UserModel user() {
        UserModel user = new UserModel();
        user.setFirstName("Yan");
        user.setLastName("Carlos");
        user.setEmail("admin@brtickets.com");
        user.setPasswordHash("hashed-password");
        user.setCpf("12345678901");
        user.setBirthday(LocalDate.of(2000, 1, 15));
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private VenueModel venue() {
        VenueModel venue = new VenueModel();
        venue.setName("Allianz Parque");
        venue.setType(VenueEnum.ARENA);
        venue.setStreet("Some Street, 100");
        venue.setCity("Sao Paulo");
        venue.setState("SP");
        venue.setCapacity(40000);
        venue.setActive(true);
        return venue;
    }

    private EventModel event(UserModel creator, VenueModel venue) {
        EventModel event = new EventModel();
        event.setVenue(venue);
        event.setCreatedBy(creator);
        event.setName("Rock in Rio");
        event.setDate(LocalDateTime.now().plusMonths(2));
        event.setType(EventTypeEnum.FESTIVAL);
        event.setStatus(EventStatusEnum.PUBLISHED);
        event.setAgeRate(16);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private TicketModel ticket(EventModel event, String seat, String price) {
        TicketModel ticket = new TicketModel();
        ticket.setEvent(event);
        ticket.setSector("Pista Premium");
        ticket.setRow("A");
        ticket.setSeat(seat);
        ticket.setPrice(new BigDecimal(price));
        ticket.setStatus(TicketStatusEnum.BOOKED);
        ticket.setType(TicketTypeEnum.FULL_TICKET);
        ticket.setQrCode(UUID.randomUUID().toString());
        ticket.setCreatedAt(LocalDateTime.now());
        return ticket;
    }

    private BookingModel booking(String total) {
        BookingModel booking = new BookingModel();
        booking.setUserId(UUID.randomUUID());
        booking.setStatus(BookingStatusEnum.CONFIRMED);
        booking.setTotalAmount(new BigDecimal(total));
        booking.setPaymentMethod(PaymentMethodEnum.PIX);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setConfirmedAt(LocalDateTime.now());
        return booking;
    }

    private BookingItemModel item(BookingModel booking, TicketModel ticket) {
        BookingItemModel item = new BookingItemModel();
        item.setBooking(booking);
        item.setTicket(ticket);
        item.setUnitPrice(ticket.getPrice());
        return item;
    }

    @Test
    @DisplayName("findAllByBooking_BookingId returns every item of that booking")
    void findsItemsOfBooking() {
        List<BookingItemModel> items = bookingItemRepository.findAllByBooking_BookingId(booking.getBookingId());

        assertThat(items).hasSize(2)
                .extracting(BookingItemModel::getUnitPrice)
                .containsExactlyInAnyOrder(new BigDecimal("150.50"), new BigDecimal("99.50"));
    }

    @Test
    @DisplayName("findAllByBooking_BookingId does not leak items of another booking")
    void doesNotMixBookings() {
        List<BookingItemModel> items = bookingItemRepository.findAllByBooking_BookingId(otherBooking.getBookingId());

        assertThat(items).hasSize(1)
                .extracting(BookingItemModel::getUnitPrice)
                .containsExactly(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("findAllByBooking_BookingId returns empty for an unknown booking")
    void findsNoItemForUnknownBooking() {
        assertThat(bookingItemRepository.findAllByBooking_BookingId(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("each item keeps the ticket relation readable, which cancelBooking relies on")
    void keepsTicketRelation() {
        List<BookingItemModel> items = bookingItemRepository.findAllByBooking_BookingId(booking.getBookingId());

        assertThat(items)
                .extracting(item -> item.getTicket().getTicketId())
                .contains(firstTicket.getTicketId());
        assertThat(items)
                .allMatch(item -> item.getTicket().getStatus() == TicketStatusEnum.BOOKED);
    }

    @Test
    @DisplayName("each item keeps the booking relation readable")
    void keepsBookingRelation() {
        List<BookingItemModel> items = bookingItemRepository.findAllByBooking_BookingId(booking.getBookingId());

        assertThat(items)
                .allMatch(item -> item.getBooking().getBookingId().equals(booking.getBookingId()));
    }
}
