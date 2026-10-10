package com.yankdev.brtickets.event.repository;

import com.yankdev.brtickets.TestContainersConfiguration;
import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestContainersConfiguration.class)
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final LocalDateTime JANUARY = LocalDateTime.of(2027, 1, 15, 20, 0);
    private static final LocalDateTime JUNE = LocalDateTime.of(2027, 6, 15, 20, 0);
    private static final LocalDateTime DECEMBER = LocalDateTime.of(2027, 12, 15, 20, 0);

    private UserModel creator;
    private VenueModel saoPauloVenue;
    private VenueModel rioVenue;

    @BeforeEach
    void setUp() {
        creator = entityManager.persistAndFlush(user());
        saoPauloVenue = entityManager.persistAndFlush(venue("Allianz Parque", "Sao Paulo"));
        rioVenue = entityManager.persistAndFlush(venue("Maracana", "Rio de Janeiro"));

        entityManager.persistAndFlush(event("Rock in Rio", EventTypeEnum.FESTIVAL, rioVenue, JANUARY));
        entityManager.persistAndFlush(event("Rock in Rio", EventTypeEnum.FESTIVAL, rioVenue, JUNE));
        entityManager.persistAndFlush(event("Lollapalooza", EventTypeEnum.FESTIVAL, saoPauloVenue, JUNE));
        entityManager.persistAndFlush(event("Hamlet", EventTypeEnum.THEATER, saoPauloVenue, DECEMBER));
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

    private VenueModel venue(String name, String city) {
        VenueModel venue = new VenueModel();
        venue.setName(name);
        venue.setType(VenueEnum.ARENA);
        venue.setStreet("Some Street, 100");
        venue.setCity(city);
        venue.setState("SP");
        venue.setCapacity(40000);
        venue.setActive(true);
        return venue;
    }

    private EventModel event(String name, EventTypeEnum type, VenueModel venue, LocalDateTime date) {
        EventModel event = new EventModel();
        event.setVenue(venue);
        event.setCreatedBy(creator);
        event.setName(name);
        event.setDescription("Description of " + name);
        event.setDate(date);
        event.setEndDate(date.plusHours(5));
        event.setType(type);
        event.setArtist("Various artists");
        event.setStatus(EventStatusEnum.PUBLISHED);
        event.setAgeRate(16);
        event.setSalesStartAt(date.minusMonths(3));
        event.setSalesEndAt(date.minusDays(1));
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    @Test
    @DisplayName("findAllByName returns every event with that exact name")
    void findsEventsByName() {
        List<EventModel> events = eventRepository.findAllByName("Rock in Rio");

        assertThat(events).hasSize(2)
                .extracting(EventModel::getName)
                .containsOnly("Rock in Rio");
    }

    @Test
    @DisplayName("findAllByName is case sensitive and returns empty on no match")
    void findsNoEventByName() {
        assertThat(eventRepository.findAllByName("rock in rio")).isEmpty();
        assertThat(eventRepository.findAllByName("Unknown Event")).isEmpty();
    }

    @Test
    @DisplayName("findAllByType filters by the event type")
    void findsEventsByType() {
        assertThat(eventRepository.findAllByType(EventTypeEnum.FESTIVAL)).hasSize(3);
        assertThat(eventRepository.findAllByType(EventTypeEnum.THEATER))
                .hasSize(1)
                .extracting(EventModel::getName)
                .containsExactly("Hamlet");
    }

    @Test
    @DisplayName("findAllByType returns empty for a type with no event")
    void findsNoEventByType() {
        assertThat(eventRepository.findAllByType(EventTypeEnum.COMEDY)).isEmpty();
    }

    @Test
    @DisplayName("findAllByVenue_City joins the venue table to filter by city")
    void findsEventsByVenueCity() {
        List<EventModel> events = eventRepository.findAllByVenue_City("Sao Paulo");

        assertThat(events).hasSize(2)
                .extracting(EventModel::getName)
                .containsExactlyInAnyOrder("Lollapalooza", "Hamlet");
    }

    @Test
    @DisplayName("findAllByVenue_City returns empty for a city with no event")
    void findsNoEventByCity() {
        assertThat(eventRepository.findAllByVenue_City("Curitiba")).isEmpty();
    }

    @Test
    @DisplayName("findAllByDateBetween returns the events inside the range")
    void findsEventsInDateRange() {
        List<EventModel> events = eventRepository.findAllByDateBetween(
                LocalDateTime.of(2027, 5, 1, 0, 0),
                LocalDateTime.of(2027, 7, 1, 0, 0));

        assertThat(events).hasSize(2)
                .extracting(EventModel::getName)
                .containsExactlyInAnyOrder("Rock in Rio", "Lollapalooza");
    }

    @Test
    @DisplayName("findAllByDateBetween includes the boundaries of the range")
    void dateRangeIsInclusive() {
        List<EventModel> events = eventRepository.findAllByDateBetween(JUNE, DECEMBER);

        assertThat(events).hasSize(3)
                .extracting(EventModel::getDate)
                .contains(JUNE, DECEMBER);
    }

    @Test
    @DisplayName("findAllByDateBetween returns empty when no event falls in the range")
    void findsNoEventInDateRange() {
        assertThat(eventRepository.findAllByDateBetween(
                LocalDateTime.of(2030, 1, 1, 0, 0),
                LocalDateTime.of(2030, 12, 31, 0, 0))).isEmpty();
    }

    @Test
    @DisplayName("the venue and creator relations are stored and readable")
    void persistsRelations() {
        EventModel stored = eventRepository.findAllByName("Lollapalooza").get(0);

        assertThat(stored.getVenue().getVenueId()).isEqualTo(saoPauloVenue.getVenueId());
        assertThat(stored.getVenue().getCity()).isEqualTo("Sao Paulo");
        assertThat(stored.getCreatedBy().getUserId()).isEqualTo(creator.getUserId());
        assertThat(stored.getCreatedBy().getEmail()).isEqualTo("admin@brtickets.com");
    }
}
