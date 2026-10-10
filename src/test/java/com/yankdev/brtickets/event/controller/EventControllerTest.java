package com.yankdev.brtickets.event.controller;

import com.yankdev.brtickets.event.dto.EventRequestDTO;
import com.yankdev.brtickets.event.dto.EventResponseDTO;
import com.yankdev.brtickets.event.model.enums.EventStatusEnum;
import com.yankdev.brtickets.event.model.enums.EventTypeEnum;
import com.yankdev.brtickets.event.service.EventService;
import com.yankdev.brtickets.shared.exception.EventNotFoundException;
import com.yankdev.brtickets.shared.exception.IllegalEventStateException;
import com.yankdev.brtickets.shared.security.JwtUtils;
import com.yankdev.brtickets.shared.security.SecurityConfig;
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

import java.time.LocalDateTime;
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

@WebMvcTest(controllers = EventController.class)
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID eventId;
    private UUID venueId;
    private EventRequestDTO request;
    private EventResponseDTO response;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        venueId = UUID.randomUUID();

        request = new EventRequestDTO();
        request.setVenueId(venueId);
        request.setName("Rock in Rio");
        request.setDescription("Music festival");
        request.setDate(LocalDateTime.now().plusMonths(2));
        request.setType(EventTypeEnum.FESTIVAL);
        request.setArtist("Various artists");
        request.setAgeRate(16);

        response = new EventResponseDTO();
        response.setEventId(eventId);
        response.setVenueId(venueId);
        response.setUserId(UUID.randomUUID());
        response.setName("Rock in Rio");
        response.setType(EventTypeEnum.FESTIVAL);
        response.setStatus(EventStatusEnum.DRAFT);
    }

    @Test
    @DisplayName("POST /events returns 201 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void createEventReturnsCreatedForAdmin() throws Exception {
        when(eventService.createEvent(any(EventRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("POST /events returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void createEventReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(eventService, never()).createEvent(any());
    }

    @Test
    @DisplayName("GET /events is public and returns every event")
    @WithAnonymousUser
    void findAllEventsReturnsOk() throws Exception {
        when(eventService.findAllEvents()).thenReturn(List.of(response));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Rock in Rio"));
    }

    @Test
    @DisplayName("GET /events?name= filters by name")
    @WithAnonymousUser
    void findEventByNameReturnsOk() throws Exception {
        when(eventService.findEventByName("Rock in Rio")).thenReturn(List.of(response));

        mockMvc.perform(get("/events").param("name", "Rock in Rio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rock in Rio"));

        verify(eventService).findEventByName("Rock in Rio");
    }

    @Test
    @DisplayName("GET /events?type= filters by type")
    @WithAnonymousUser
    void findEventByTypeReturnsOk() throws Exception {
        when(eventService.findEventByType(EventTypeEnum.FESTIVAL)).thenReturn(List.of(response));

        mockMvc.perform(get("/events").param("type", "FESTIVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("FESTIVAL"));

        verify(eventService).findEventByType(EventTypeEnum.FESTIVAL);
    }

    @Test
    @DisplayName("GET /events?type= returns 400 for an unknown type")
    @WithAnonymousUser
    void findEventByTypeRejectsUnknownType() throws Exception {
        mockMvc.perform(get("/events").param("type", "KARAOKE"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).findEventByType(any());
    }

    @Test
    @DisplayName("GET /events?city= filters by city")
    @WithAnonymousUser
    void findEventByCityReturnsOk() throws Exception {
        when(eventService.findEventByCity("Sao Paulo")).thenReturn(List.of(response));

        mockMvc.perform(get("/events").param("city", "Sao Paulo"))
                .andExpect(status().isOk());

        verify(eventService).findEventByCity("Sao Paulo");
    }

    @Test
    @DisplayName("GET /events?date=&start=&end= filters by date range")
    @WithAnonymousUser
    void findEventByDateReturnsOk() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59);
        when(eventService.findEventByDate(start, end)).thenReturn(List.of(response));

        mockMvc.perform(get("/events")
                        .param("date", "range")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk());

        verify(eventService).findEventByDate(start, end);
    }

    @Test
    @DisplayName("GET /events/{eventId} returns the event")
    @WithAnonymousUser
    void findEventByIdReturnsOk() throws Exception {
        when(eventService.findEventById(eventId)).thenReturn(response);

        mockMvc.perform(get("/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()));
    }

    @Test
    @DisplayName("GET /events/{eventId} returns 404 when the event does not exist")
    @WithAnonymousUser
    void findEventByIdReturnsNotFound() throws Exception {
        when(eventService.findEventById(eventId)).thenThrow(new EventNotFoundException("Event not found."));

        mockMvc.perform(get("/events/{eventId}", eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /events/{eventId} returns 200 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void updateEventReturnsOkForAdmin() throws Exception {
        response.setName("New Event Name");
        when(eventService.updateEvent(eq(eventId), any(EventRequestDTO.class))).thenReturn(response);

        mockMvc.perform(patch("/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Event Name"));
    }

    @Test
    @DisplayName("PATCH /events/{eventId} returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void updateEventReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(patch("/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(eventService, never()).updateEvent(any(), any());
    }

    @Test
    @DisplayName("POST /events/{eventId}/publish returns 200 with status PUBLISHED")
    @WithMockUser(roles = "ADMIN")
    void publishEventReturnsOkForAdmin() throws Exception {
        response.setStatus(EventStatusEnum.PUBLISHED);
        when(eventService.publishEvent(eventId)).thenReturn(response);

        mockMvc.perform(post("/events/{eventId}/publish", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("POST /events/{eventId}/publish returns 400 when the state is wrong")
    @WithMockUser(roles = "ADMIN")
    void publishEventReturnsBadRequestOnWrongState() throws Exception {
        when(eventService.publishEvent(eventId))
                .thenThrow(new IllegalEventStateException("Event state is wrong."));

        mockMvc.perform(post("/events/{eventId}/publish", eventId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events/{eventId}/publish returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void publishEventReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(post("/events/{eventId}/publish", eventId))
                .andExpect(status().isForbidden());

        verify(eventService, never()).publishEvent(any());
    }

    @Test
    @DisplayName("DELETE /events/{eventId} returns 204 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void deleteEventReturnsNoContentForAdmin() throws Exception {
        mockMvc.perform(delete("/events/{eventId}", eventId))
                .andExpect(status().isNoContent());

        verify(eventService).cancelEvent(eventId);
    }

    @Test
    @DisplayName("DELETE /events/{eventId} returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void deleteEventReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(delete("/events/{eventId}", eventId))
                .andExpect(status().isForbidden());

        verify(eventService, never()).cancelEvent(any());
    }
}
