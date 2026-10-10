package com.yankdev.brtickets.ticket.repository;

import com.yankdev.brtickets.TestContainersConfiguration;
import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
import com.yankdev.brtickets.ticket.model.TicketModel;
import com.yankdev.brtickets.ticket.model.enums.TicketStatusEnum;
import com.yankdev.brtickets.ticket.model.enums.TicketTypeEnum;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.model.enums.VenueEnum;
import org.hibernate.exception.ConstraintViolationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestContainersConfiguration.class)
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TestEntityManager entityManager;

    private EventModel firstEvent;
    private EventModel secondEvent;
    private TicketModel firstTicket;
    private TicketModel secondTicket;
    private TicketModel otherEventTicket;

    @BeforeEach
    void setUp() {
        UserModel creator = entityManager.persistAndFlush(user());
        VenueModel venue = entityManager.persistAndFlush(venue());

        firstEvent = entityManager.persistAndFlush(event("Rock in Rio", creator, venue));
        secondEvent = entityManager.persistAndFlush(event("Lollapalooza", creator, venue));

        firstTicket = entityManager.persistAndFlush(
                ticket(firstEvent, "A", "1", "150.50", TicketStatusEnum.AVAILABLE));
        secondTicket = entityManager.persistAndFlush(
                ticket(firstEvent, "A", "2", "99.50", TicketStatusEnum.BOOKED));
        otherEventTicket = entityManager.persistAndFlush(
                ticket(secondEvent, "B", "10", "300.00", TicketStatusEnum.AVAILABLE));
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

    private EventModel event(String name, UserModel creator, VenueModel venue) {
        EventModel event = new EventModel();
        event.setVenue(venue);
        event.setCreatedBy(creator);
        event.setName(name);
        event.setDate(LocalDateTime.now().plusMonths(2));
        event.setType(EventTypeEnum.FESTIVAL);
        event.setStatus(EventStatusEnum.PUBLISHED);
        event.setAgeRate(16);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private TicketModel ticket(EventModel event, String row, String seat, String price, TicketStatusEnum status) {
        TicketModel ticket = new TicketModel();
        ticket.setEvent(event);
        ticket.setSector("Pista Premium");
        ticket.setRow(row);
        ticket.setSeat(seat);
        ticket.setPrice(new BigDecimal(price));
        ticket.setStatus(status);
        ticket.setType(TicketTypeEnum.FULL_TICKET);
        ticket.setQrCode(UUID.randomUUID().toString());
        ticket.setCreatedAt(LocalDateTime.now());
        return ticket;
    }

    @Test
    @DisplayName("findAllByEvent_EventId returns every ticket of that event")
    void findsTicketsByEvent() {
        List<TicketModel> tickets = ticketRepository.findAllByEvent_EventId(firstEvent.getEventId());

        assertThat(tickets).hasSize(2)
                .extracting(TicketModel::getSeat)
                .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    @DisplayName("findAllByEvent_EventId does not leak tickets of another event")
    void doesNotMixEvents() {
        List<TicketModel> tickets = ticketRepository.findAllByEvent_EventId(secondEvent.getEventId());

        assertThat(tickets).hasSize(1)
                .extracting(TicketModel::getSeat)
                .containsExactly("10");
    }

    @Test
    @DisplayName("findAllByEvent_EventId returns empty for an unknown event")
    void findsNoTicketForUnknownEvent() {
        assertThat(ticketRepository.findAllByEvent_EventId(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("findAllByTicketIdIn returns exactly the requested tickets")
    void findsTicketsByIdList() {
        List<TicketModel> tickets = ticketRepository.findAllByTicketIdIn(
                List.of(firstTicket.getTicketId(), otherEventTicket.getTicketId()));

        assertThat(tickets).hasSize(2)
                .extracting(TicketModel::getTicketId)
                .containsExactlyInAnyOrder(firstTicket.getTicketId(), otherEventTicket.getTicketId());
    }

    @Test
    @DisplayName("findAllByTicketIdIn returns fewer rows when an id does not exist")
    void returnsFewerRowsForUnknownId() {
        List<TicketModel> tickets = ticketRepository.findAllByTicketIdIn(
                List.of(firstTicket.getTicketId(), UUID.randomUUID()));

        assertThat(tickets).hasSize(1);
    }

    @Test
    @DisplayName("findAllByTicketIdIn returns empty for an empty id list")
    void returnsEmptyForEmptyIdList() {
        assertThat(ticketRepository.findAllByTicketIdIn(List.of())).isEmpty();
    }

    @Test
    @DisplayName("the ticket status and type are stored as strings")
    void persistsEnumsAsStrings() {
        TicketModel stored = ticketRepository.findById(secondTicket.getTicketId()).orElseThrow();

        assertThat(stored.getStatus()).isEqualTo(TicketStatusEnum.BOOKED);
        assertThat(stored.getType()).isEqualTo(TicketTypeEnum.FULL_TICKET);
    }

    @Test
    @DisplayName("the price keeps its two decimal places")
    void persistsPriceScale() {
        TicketModel stored = ticketRepository.findById(firstTicket.getTicketId()).orElseThrow();

        assertThat(stored.getPrice()).isEqualByComparingTo("150.50");
    }

    @Test
    @DisplayName("the database rejects a duplicated QR code")
    void rejectsDuplicatedQrCode() {
        TicketModel duplicate = ticket(firstEvent, "C", "3", "50.00", TicketStatusEnum.AVAILABLE);
        duplicate.setQrCode(firstTicket.getQrCode());

        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
