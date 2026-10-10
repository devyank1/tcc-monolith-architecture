package com.yankdev.brtickets.ticket.service;

import com.yankdev.brtickets.event.model.EventModel;
import com.yankdev.brtickets.event.repository.EventRepository;
import com.yankdev.brtickets.shared.exception.EventNotFoundException;
import com.yankdev.brtickets.shared.exception.TicketNotFoundException;
import com.yankdev.brtickets.ticket.dto.TicketRequestDTO;
import com.yankdev.brtickets.ticket.dto.TicketResponseDTO;
import com.yankdev.brtickets.ticket.model.TicketModel;
import com.yankdev.brtickets.ticket.model.enums.TicketStatusEnum;
import com.yankdev.brtickets.ticket.model.enums.TicketTypeEnum;
import com.yankdev.brtickets.ticket.repository.TicketRepository;
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
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private TicketService ticketService;

    @Captor
    private ArgumentCaptor<TicketModel> ticketCaptor;

    private EventModel event;
    private TicketRequestDTO request;

    @BeforeEach
    void setUp() {
        event = new EventModel();
        event.setEventId(UUID.randomUUID());
        event.setName("Rock in Rio");

        request = new TicketRequestDTO();
        request.setEventId(event.getEventId());
        request.setSector("Pista Premium");
        request.setRow("A");
        request.setSeat("12");
        request.setPrice(new BigDecimal("450.00"));
        request.setType(TicketTypeEnum.FULL_TICKET);
    }

    private TicketModel existingTicket() {
        TicketModel ticket = new TicketModel();
        ticket.setTicketId(UUID.randomUUID());
        ticket.setEvent(event);
        ticket.setSector("Old Sector");
        ticket.setRow("B");
        ticket.setSeat("99");
        ticket.setPrice(new BigDecimal("200.00"));
        ticket.setStatus(TicketStatusEnum.AVAILABLE);
        ticket.setType(TicketTypeEnum.HALF_TICKET);
        ticket.setQrCode(UUID.randomUUID().toString());
        ticket.setCreatedAt(LocalDateTime.now().minusDays(5));
        return ticket;
    }

    @Nested
    @DisplayName("createTicket")
    class CreateTicket {

        @Test
        @DisplayName("creates an AVAILABLE ticket linked to the event")
        void createsTicket() {
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
            when(ticketRepository.save(any(TicketModel.class))).thenAnswer(call -> call.getArgument(0));

            TicketResponseDTO response = ticketService.createTicket(request);

            assertThat(response.getEventId()).isEqualTo(event.getEventId());
            assertThat(response.getSector()).isEqualTo("Pista Premium");
            assertThat(response.getRow()).isEqualTo("A");
            assertThat(response.getSeat()).isEqualTo("12");
            assertThat(response.getPrice()).isEqualByComparingTo("450.00");
            assertThat(response.getType()).isEqualTo(TicketTypeEnum.FULL_TICKET);
            assertThat(response.getStatus()).isEqualTo(TicketStatusEnum.AVAILABLE);
        }

        @Test
        @DisplayName("generates a QR code and a creation timestamp, with no update timestamp")
        void generatesQrCodeAndTimestamps() {
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
            when(ticketRepository.save(any(TicketModel.class))).thenAnswer(call -> call.getArgument(0));

            TicketResponseDTO response = ticketService.createTicket(request);

            assertThat(response.getQrCode()).isNotBlank();
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("gives every ticket a different QR code")
        void generatesUniqueQrCodePerTicket() {
            when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
            when(ticketRepository.save(any(TicketModel.class))).thenAnswer(call -> call.getArgument(0));

            String firstQrCode = ticketService.createTicket(request).getQrCode();
            String secondQrCode = ticketService.createTicket(request).getQrCode();

            assertThat(firstQrCode).isNotEqualTo(secondQrCode);
        }

        @Test
        @DisplayName("fails when the event does not exist and never saves")
        void failsWhenEventDoesNotExist() {
            when(eventRepository.findById(request.getEventId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.createTicket(request))
                    .isInstanceOf(EventNotFoundException.class)
                    .hasMessage("Event not found.");

            verify(ticketRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findTicketById / findAllTicketsByEvent")
    class Queries {

        @Test
        @DisplayName("returns the ticket found by id")
        void findsTicketById() {
            TicketModel ticket = existingTicket();
            when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));

            TicketResponseDTO response = ticketService.findTicketById(ticket.getTicketId());

            assertThat(response.getTicketId()).isEqualTo(ticket.getTicketId());
            assertThat(response.getSector()).isEqualTo("Old Sector");
        }

        @Test
        @DisplayName("fails when the ticket id does not exist")
        void failsWhenTicketIdDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(ticketRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.findTicketById(unknownId))
                    .isInstanceOf(TicketNotFoundException.class)
                    .hasMessage("Ticket not found by ID.");
        }

        @Test
        @DisplayName("returns every ticket of an event")
        void findsAllTicketsByEvent() {
            TicketModel first = existingTicket();
            TicketModel second = existingTicket();
            second.setSeat("100");
            when(ticketRepository.findAllByEvent_EventId(event.getEventId()))
                    .thenReturn(List.of(first, second));

            List<TicketResponseDTO> response = ticketService.findAllTicketsByEvent(event.getEventId());

            assertThat(response).hasSize(2)
                    .extracting(TicketResponseDTO::getSeat)
                    .containsExactly("99", "100");
        }

        @Test
        @DisplayName("returns an empty list when the event has no ticket")
        void findsNoTicketForEvent() {
            UUID eventId = UUID.randomUUID();
            when(ticketRepository.findAllByEvent_EventId(eventId)).thenReturn(List.of());

            assertThat(ticketService.findAllTicketsByEvent(eventId)).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("updateTicket")
    class UpdateTicket {

        @Test
        @DisplayName("updates only the fields sent in the request")
        void updatesOnlyNonNullFields() {
            TicketModel ticket = existingTicket();
            when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(TicketModel.class))).thenAnswer(call -> call.getArgument(0));

            TicketRequestDTO partial = new TicketRequestDTO();
            partial.setSector("New Sector");
            partial.setSeat("1");

            TicketResponseDTO response = ticketService.updateTicket(partial, ticket.getTicketId());

            assertThat(response.getSector()).isEqualTo("New Sector");
            assertThat(response.getSeat()).isEqualTo("1");
            assertThat(response.getRow()).isEqualTo("B");
            assertThat(response.getPrice()).isEqualByComparingTo("200.00");
            assertThat(response.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("stamps updatedAt with the current time when the request carries one")
        void stampsUpdatedAt() {
            TicketModel ticket = existingTicket();
            when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(TicketModel.class))).thenAnswer(call -> call.getArgument(0));

            TicketRequestDTO partial = new TicketRequestDTO();
            partial.setRow("Z");
            partial.setUpdatedAt(LocalDateTime.now().minusYears(1));

            TicketResponseDTO response = ticketService.updateTicket(partial, ticket.getTicketId());

            assertThat(response.getRow()).isEqualTo("Z");
            assertThat(response.getUpdatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
        }

        @Test
        @DisplayName("fails when the ticket does not exist and never saves")
        void failsWhenTicketDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(ticketRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.updateTicket(request, unknownId))
                    .isInstanceOf(TicketNotFoundException.class)
                    .hasMessage("Ticket not found.");

            verify(ticketRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivateTicket")
    class DeactivateTicket {

        @Test
        @DisplayName("soft deletes the ticket by setting status CANCELLED")
        void cancelsTicket() {
            TicketModel ticket = existingTicket();
            when(ticketRepository.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));

            ticketService.deactivateTicket(ticket.getTicketId());

            verify(ticketRepository).save(ticketCaptor.capture());
            assertThat(ticketCaptor.getValue().getStatus()).isEqualTo(TicketStatusEnum.CANCELLED);
        }

        @Test
        @DisplayName("fails when the ticket does not exist")
        void failsWhenTicketDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(ticketRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.deactivateTicket(unknownId))
                    .isInstanceOf(TicketNotFoundException.class)
                    .hasMessage("Ticket not found");

            verify(ticketRepository, never()).save(any());
        }
    }
}
