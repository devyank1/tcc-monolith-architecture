package com.yankdev.brtickets.ticket.controller;

import com.yankdev.brtickets.shared.exception.EventNotFoundException;
import com.yankdev.brtickets.shared.exception.TicketNotFoundException;
import com.yankdev.brtickets.shared.security.JwtUtils;
import com.yankdev.brtickets.shared.security.SecurityConfig;
import com.yankdev.brtickets.ticket.dto.TicketRequestDTO;
import com.yankdev.brtickets.ticket.dto.TicketResponseDTO;
import com.yankdev.brtickets.ticket.model.enums.TicketStatusEnum;
import com.yankdev.brtickets.ticket.model.enums.TicketTypeEnum;
import com.yankdev.brtickets.ticket.service.TicketService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TicketController.class)
@Import(SecurityConfig.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID ticketId;
    private UUID eventId;
    private TicketRequestDTO request;
    private TicketResponseDTO response;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        request = new TicketRequestDTO();
        request.setEventId(eventId);
        request.setSector("Pista Premium");
        request.setRow("A");
        request.setSeat("12");
        request.setPrice(new BigDecimal("450.00"));
        request.setType(TicketTypeEnum.FULL_TICKET);

        response = new TicketResponseDTO();
        response.setTicketId(ticketId);
        response.setEventId(eventId);
        response.setSector("Pista Premium");
        response.setRow("A");
        response.setSeat("12");
        response.setPrice(new BigDecimal("450.00"));
        response.setStatus(TicketStatusEnum.AVAILABLE);
        response.setType(TicketTypeEnum.FULL_TICKET);
        response.setQrCode(UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("POST /tickets returns 201 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void createTicketReturnsCreatedForAdmin() throws Exception {
        when(ticketService.createTicket(any(TicketRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(ticketId.toString()))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.qrCode").isNotEmpty());
    }

    @Test
    @DisplayName("POST /tickets returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void createTicketReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).createTicket(any());
    }

    @Test
    @DisplayName("POST /tickets returns 404 when the event does not exist")
    @WithMockUser(roles = "ADMIN")
    void createTicketReturnsNotFoundForUnknownEvent() throws Exception {
        when(ticketService.createTicket(any(TicketRequestDTO.class)))
                .thenThrow(new EventNotFoundException("Event not found."));

        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /tickets/{ticketId} returns the ticket for an authenticated user")
    @WithMockUser
    void findTicketByIdReturnsOk() throws Exception {
        when(ticketService.findTicketById(ticketId)).thenReturn(response);

        mockMvc.perform(get("/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sector").value("Pista Premium"));
    }

    @Test
    @DisplayName("GET /tickets/{ticketId} returns 404 when the ticket does not exist")
    @WithMockUser
    void findTicketByIdReturnsNotFound() throws Exception {
        when(ticketService.findTicketById(ticketId))
                .thenThrow(new TicketNotFoundException("Ticket not found by ID."));

        mockMvc.perform(get("/tickets/{ticketId}", ticketId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /tickets?eventId= is public and lists the tickets of the event")
    @WithAnonymousUser
    void findAllTicketsByEventReturnsOk() throws Exception {
        when(ticketService.findAllTicketsByEvent(eventId)).thenReturn(List.of(response));

        mockMvc.perform(get("/tickets").param("eventId", eventId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].eventId").value(eventId.toString()));
    }

    @Test
    @DisplayName("GET /tickets without the eventId param returns 400")
    @WithAnonymousUser
    void findAllTicketsRequiresEventIdParam() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isBadRequest());

        verify(ticketService, never()).findAllTicketsByEvent(any());
    }

    @Test
    @DisplayName("PATCH /tickets/{ticketId} returns 200 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void updateTicketReturnsOkForAdmin() throws Exception {
        response.setSector("New Sector");
        when(ticketService.updateTicket(any(TicketRequestDTO.class), eq(ticketId))).thenReturn(response);

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sector").value("New Sector"));
    }

    @Test
    @DisplayName("PATCH /tickets/{ticketId} returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void updateTicketReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).updateTicket(any(), any());
    }

    @Test
    @DisplayName("DELETE /tickets/{ticketId} returns 204 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void deleteTicketReturnsNoContentForAdmin() throws Exception {
        mockMvc.perform(delete("/tickets/{ticketId}", ticketId))
                .andExpect(status().isNoContent());

        verify(ticketService).deactivateTicket(ticketId);
    }

    @Test
    @DisplayName("DELETE /tickets/{ticketId} returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void deleteTicketReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(delete("/tickets/{ticketId}", ticketId))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).deactivateTicket(any());
    }
}
