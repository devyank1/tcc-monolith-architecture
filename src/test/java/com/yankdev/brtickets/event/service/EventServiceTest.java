package com.yankdev.brtickets.event.service;

import com.yankdev.brtickets.event.dto.EventRequestDTO;
import com.yankdev.brtickets.event.dto.EventResponseDTO;
import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
import com.yankdev.brtickets.event.repository.EventRepository;
import com.yankdev.brtickets.shared.exception.EventNotFoundException;
import com.yankdev.brtickets.shared.exception.IllegalEventStateException;
import com.yankdev.brtickets.shared.exception.UserIsNotActiveException;
import com.yankdev.brtickets.shared.exception.UserNotFoundException;
import com.yankdev.brtickets.shared.exception.VenueIsNotActiveException;
import com.yankdev.brtickets.shared.exception.VenueNotFoundException;
import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import com.yankdev.brtickets.user.repository.UserRepository;
import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.model.enums.VenueEnum;
import com.yankdev.brtickets.venue.repository.VenueRepository;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final String LOGGED_EMAIL = "admin@brtickets.com";

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    @Captor
    private ArgumentCaptor<EventModel> eventCaptor;

    private UserModel loggedUser;
    private VenueModel venue;
    private EventRequestDTO request;

    @BeforeEach
    void setUp() {
        loggedUser = new UserModel();
        loggedUser.setUserId(UUID.randomUUID());
        loggedUser.setEmail(LOGGED_EMAIL);
        loggedUser.setRole(UserRole.ADMIN);
        loggedUser.setActive(true);

        venue = new VenueModel();
        venue.setVenueId(UUID.randomUUID());
        venue.setName("Allianz Parque");
        venue.setType(VenueEnum.ARENA);
        venue.setCity("Sao Paulo");
        venue.setCapacity(43000);
        venue.setActive(true);

        request = new EventRequestDTO();
        request.setVenueId(venue.getVenueId());
        request.setName("Rock in Rio");
        request.setDescription("Music festival");
        request.setDate(LocalDateTime.now().plusMonths(2));
        request.setEndDate(LocalDateTime.now().plusMonths(2).plusHours(8));
        request.setType(EventTypeEnum.FESTIVAL);
        request.setArtist("Various artists");
        request.setAgeRate(16);
        request.setSalesStartAt(LocalDateTime.now().plusDays(1));
        request.setSalesEndAt(LocalDateTime.now().plusMonths(2).minusDays(1));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(email, "password"));
    }

    private EventModel existingEvent(EventStatusEnum status) {
        EventModel event = new EventModel();
        event.setEventId(UUID.randomUUID());
        event.setVenue(venue);
        event.setCreatedBy(loggedUser);
        event.setName("Old Event Name");
        event.setDescription("Old description");
        event.setDate(LocalDateTime.now().plusMonths(1));
        event.setEndDate(LocalDateTime.now().plusMonths(1).plusHours(4));
        event.setType(EventTypeEnum.SHOW);
        event.setArtist("Old Artist");
        event.setStatus(status);
        event.setAgeRate(18);
        event.setSalesStartAt(LocalDateTime.now());
        event.setSalesEndAt(LocalDateTime.now().plusMonths(1).minusDays(1));
        event.setCreatedAt(LocalDateTime.now().minusDays(10));
        return event;
    }

    @Nested
    @DisplayName("createEvent")
    class CreateEvent {

        @Test
        @DisplayName("creates the event as DRAFT for the authenticated user")
        void createsEventAsDraft() {
            authenticate(LOGGED_EMAIL);
            when(userRepository.findByEmail(LOGGED_EMAIL)).thenReturn(Optional.of(loggedUser));
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));
            when(eventRepository.save(any(EventModel.class))).thenAnswer(call -> call.getArgument(0));

            EventResponseDTO response = eventService.createEvent(request);

            assertThat(response.getName()).isEqualTo("Rock in Rio");
            assertThat(response.getStatus()).isEqualTo(EventStatusEnum.DRAFT);
            assertThat(response.getVenueId()).isEqualTo(venue.getVenueId());
            assertThat(response.getUserId()).isEqualTo(loggedUser.getUserId());
            assertThat(response.getType()).isEqualTo(EventTypeEnum.FESTIVAL);
            assertThat(response.getAgeRate()).isEqualTo(16);
        }

        @Test
        @DisplayName("stamps createdAt on the saved event")
        void stampsCreatedAt() {
            authenticate(LOGGED_EMAIL);
            when(userRepository.findByEmail(LOGGED_EMAIL)).thenReturn(Optional.of(loggedUser));
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));
            when(eventRepository.save(any(EventModel.class))).thenAnswer(call -> call.getArgument(0));

            eventService.createEvent(request);

            verify(eventRepository).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getCreatedAt())
                    .isNotNull()
                    .isAfter(LocalDateTime.now().minusMinutes(1));
        }

        @Test
        @DisplayName("fails when the authenticated email has no user")
        void failsWhenLoggedUserDoesNotExist() {
            authenticate("ghost@brtickets.com");
            when(userRepository.findByEmail("ghost@brtickets.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.createEvent(request))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found by email");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the venue does not exist")
        void failsWhenVenueDoesNotExist() {
            authenticate(LOGGED_EMAIL);
            when(userRepository.findByEmail(LOGGED_EMAIL)).thenReturn(Optional.of(loggedUser));
            when(venueRepository.findById(request.getVenueId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.createEvent(request))
                    .isInstanceOf(VenueNotFoundException.class)
                    .hasMessage("Venue not found.");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses an inactive venue")
        void refusesInactiveVenue() {
            authenticate(LOGGED_EMAIL);
            venue.setActive(false);
            when(userRepository.findByEmail(LOGGED_EMAIL)).thenReturn(Optional.of(loggedUser));
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));

            assertThatThrownBy(() -> eventService.createEvent(request))
                    .isInstanceOf(VenueIsNotActiveException.class)
                    .hasMessage("You cannot create an event in an inactive venue.");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses an inactive user")
        void refusesInactiveUser() {
            authenticate(LOGGED_EMAIL);
            loggedUser.setActive(false);
            when(userRepository.findByEmail(LOGGED_EMAIL)).thenReturn(Optional.of(loggedUser));
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));

            assertThatThrownBy(() -> eventService.createEvent(request))
                    .isInstanceOf(UserIsNotActiveException.class)
                    .hasMessage("You cannot create an event with an inactive user.");

            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("queries")
    class Queries {

        @Test
        @DisplayName("findEventById returns the event")
        void findsEventById() {
            EventModel event = existingEvent(EventStatusEnum.PUBLISHED);
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));

            EventResponseDTO response = eventService.findEventById(event.getEventId());

            assertThat(response.getEventId()).isEqualTo(event.getEventId());
            assertThat(response.getStatus()).isEqualTo(EventStatusEnum.PUBLISHED);
        }

        @Test
        @DisplayName("findEventById fails when the id does not exist")
        void failsWhenEventIdDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(eventRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.findEventById(unknownId))
                    .isInstanceOf(EventNotFoundException.class)
                    .hasMessage("Event not found.");
        }

        @Test
        @DisplayName("findAllEvents maps every event")
        void findsAllEvents() {
            EventModel first = existingEvent(EventStatusEnum.DRAFT);
            EventModel second = existingEvent(EventStatusEnum.PUBLISHED);
            second.setName("Second Event");
            when(eventRepository.findAll()).thenReturn(List.of(first, second));

            assertThat(eventService.findAllEvents()).hasSize(2)
                    .extracting(EventResponseDTO::getName)
                    .containsExactly("Old Event Name", "Second Event");
        }

        @Test
        @DisplayName("findAllEvents returns an empty list when there is no event")
        void findsNoEvent() {
            when(eventRepository.findAll()).thenReturn(List.of());

            assertThat(eventService.findAllEvents()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("findEventByName delegates to the repository")
        void findsEventByName() {
            EventModel event = existingEvent(EventStatusEnum.PUBLISHED);
            when(eventRepository.findAllByName("Old Event Name")).thenReturn(List.of(event));

            assertThat(eventService.findEventByName("Old Event Name")).hasSize(1);
            verify(eventRepository).findAllByName("Old Event Name");
        }

        @Test
        @DisplayName("findEventByType delegates to the repository")
        void findsEventByType() {
            EventModel event = existingEvent(EventStatusEnum.PUBLISHED);
            when(eventRepository.findAllByType(EventTypeEnum.SHOW)).thenReturn(List.of(event));

            assertThat(eventService.findEventByType(EventTypeEnum.SHOW))
                    .hasSize(1)
                    .first()
                    .extracting(EventResponseDTO::getType)
                    .isEqualTo(EventTypeEnum.SHOW);
        }

        @Test
        @DisplayName("findEventByCity delegates to the repository")
        void findsEventByCity() {
            EventModel event = existingEvent(EventStatusEnum.PUBLISHED);
            when(eventRepository.findAllByVenue_City("Sao Paulo")).thenReturn(List.of(event));

            assertThat(eventService.findEventByCity("Sao Paulo")).hasSize(1);
            verify(eventRepository).findAllByVenue_City("Sao Paulo");
        }

        @Test
        @DisplayName("findEventByDate delegates to the repository with the given range")
        void findsEventByDate() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.plusMonths(3);
            EventModel event = existingEvent(EventStatusEnum.PUBLISHED);
            when(eventRepository.findAllByDateBetween(start, end)).thenReturn(List.of(event));

            assertThat(eventService.findEventByDate(start, end)).hasSize(1);
            verify(eventRepository).findAllByDateBetween(start, end);
        }
    }

    @Nested
    @DisplayName("updateEvent")
    class UpdateEvent {

        @Test
        @DisplayName("updates only the fields sent and keeps creator and status")
        void updatesOnlyNonNullFields() {
            EventModel event = existingEvent(EventStatusEnum.PUBLISHED);
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
            when(eventRepository.save(any(EventModel.class))).thenAnswer(call -> call.getArgument(0));

            EventRequestDTO partial = new EventRequestDTO();
            partial.setName("New Event Name");
            partial.setArtist("New Artist");

            EventResponseDTO response = eventService.updateEvent(event.getEventId(), partial);

            assertThat(response.getName()).isEqualTo("New Event Name");
            assertThat(response.getArtist()).isEqualTo("New Artist");
            assertThat(response.getDescription()).isEqualTo("Old description");
            assertThat(response.getStatus()).isEqualTo(EventStatusEnum.PUBLISHED);
            assertThat(response.getUserId()).isEqualTo(loggedUser.getUserId());
            verify(venueRepository, never()).findById(any());
        }

        @Test
        @DisplayName("moves the event to another active venue")
        void movesEventToAnotherVenue() {
            EventModel event = existingEvent(EventStatusEnum.DRAFT);
            VenueModel newVenue = new VenueModel();
            newVenue.setVenueId(UUID.randomUUID());
            newVenue.setName("Maracana");
            newVenue.setActive(true);

            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
            when(venueRepository.findById(newVenue.getVenueId())).thenReturn(Optional.of(newVenue));
            when(eventRepository.save(any(EventModel.class))).thenAnswer(call -> call.getArgument(0));

            EventRequestDTO partial = new EventRequestDTO();
            partial.setVenueId(newVenue.getVenueId());

            EventResponseDTO response = eventService.updateEvent(event.getEventId(), partial);

            assertThat(response.getVenueId()).isEqualTo(newVenue.getVenueId());
        }

        @Test
        @DisplayName("refuses to move the event to an inactive venue")
        void refusesInactiveVenueOnUpdate() {
            EventModel event = existingEvent(EventStatusEnum.DRAFT);
            VenueModel inactiveVenue = new VenueModel();
            inactiveVenue.setVenueId(UUID.randomUUID());
            inactiveVenue.setActive(false);

            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
            when(venueRepository.findById(inactiveVenue.getVenueId())).thenReturn(Optional.of(inactiveVenue));

            EventRequestDTO partial = new EventRequestDTO();
            partial.setVenueId(inactiveVenue.getVenueId());

            assertThatThrownBy(() -> eventService.updateEvent(event.getEventId(), partial))
                    .isInstanceOf(VenueIsNotActiveException.class)
                    .hasMessage("You cannot update event in an inactive venue.");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the new venue does not exist")
        void failsWhenNewVenueDoesNotExist() {
            EventModel event = existingEvent(EventStatusEnum.DRAFT);
            UUID unknownVenueId = UUID.randomUUID();

            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
            when(venueRepository.findById(unknownVenueId)).thenReturn(Optional.empty());

            EventRequestDTO partial = new EventRequestDTO();
            partial.setVenueId(unknownVenueId);

            assertThatThrownBy(() -> eventService.updateEvent(event.getEventId(), partial))
                    .isInstanceOf(VenueNotFoundException.class)
                    .hasMessage("Venue not found");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("updates every field when all of them are sent")
        void updatesAllFields() {
            EventModel event = existingEvent(EventStatusEnum.DRAFT);
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));
            when(eventRepository.save(any(EventModel.class))).thenAnswer(call -> call.getArgument(0));

            EventResponseDTO response = eventService.updateEvent(event.getEventId(), request);

            assertThat(response.getName()).isEqualTo("Rock in Rio");
            assertThat(response.getDescription()).isEqualTo("Music festival");
            assertThat(response.getType()).isEqualTo(EventTypeEnum.FESTIVAL);
            assertThat(response.getArtist()).isEqualTo("Various artists");
            assertThat(response.getAgeRate()).isEqualTo(16);
            assertThat(response.getDate()).isEqualTo(request.getDate());
            assertThat(response.getEndDate()).isEqualTo(request.getEndDate());
            assertThat(response.getSalesStartAt()).isEqualTo(request.getSalesStartAt());
            assertThat(response.getSalesEndAt()).isEqualTo(request.getSalesEndAt());
        }

        @Test
        @DisplayName("fails when the event does not exist")
        void failsWhenEventDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(eventRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.updateEvent(unknownId, request))
                    .isInstanceOf(EventNotFoundException.class);

            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("publishEvent")
    class PublishEvent {

        @Test
        @DisplayName("publishes an event that is in DRAFT")
        void publishesDraftEvent() {
            EventModel event = existingEvent(EventStatusEnum.DRAFT);
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
            when(eventRepository.save(any(EventModel.class))).thenAnswer(call -> call.getArgument(0));

            EventResponseDTO response = eventService.publishEvent(event.getEventId());

            assertThat(response.getStatus()).isEqualTo(EventStatusEnum.PUBLISHED);
        }

        @Test
        @DisplayName("refuses to publish an event that is already PUBLISHED")
        void refusesAlreadyPublishedEvent() {
            EventModel event = existingEvent(EventStatusEnum.PUBLISHED);
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.publishEvent(event.getEventId()))
                    .isInstanceOf(IllegalEventStateException.class)
                    .hasMessage("Event state is wrong.");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses to publish a CANCELLED event")
        void refusesCancelledEvent() {
            EventModel event = existingEvent(EventStatusEnum.CANCELLED);
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.publishEvent(event.getEventId()))
                    .isInstanceOf(IllegalEventStateException.class);

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the event does not exist")
        void failsWhenEventDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(eventRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.publishEvent(unknownId))
                    .isInstanceOf(EventNotFoundException.class);

            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancelEvent")
    class CancelEvent {

        @Test
        @DisplayName("cancels a PUBLISHED event")
        void cancelsPublishedEvent() {
            EventModel event = existingEvent(EventStatusEnum.PUBLISHED);
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));

            eventService.cancelEvent(event.getEventId());

            verify(eventRepository).save(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getStatus()).isEqualTo(EventStatusEnum.CANCELLED);
        }

        @Test
        @DisplayName("refuses to cancel a FINISHED event")
        void refusesFinishedEvent() {
            EventModel event = existingEvent(EventStatusEnum.FINISHED);
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.cancelEvent(event.getEventId()))
                    .isInstanceOf(IllegalEventStateException.class)
                    .hasMessage("You cannot cancel a FINISHED event.");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuses to cancel an event that is already CANCELLED")
        void refusesAlreadyCancelledEvent() {
            EventModel event = existingEvent(EventStatusEnum.CANCELLED);
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.cancelEvent(event.getEventId()))
                    .isInstanceOf(IllegalEventStateException.class)
                    .hasMessage("This event is already cancelled.");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the event does not exist")
        void failsWhenEventDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(eventRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.cancelEvent(unknownId))
                    .isInstanceOf(EventNotFoundException.class);

            verify(eventRepository, never()).save(any());
        }
    }
}
