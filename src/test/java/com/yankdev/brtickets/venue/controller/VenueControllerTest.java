package com.yankdev.brtickets.venue.controller;

import com.yankdev.brtickets.shared.exception.IllegalVenueCapacityException;
import com.yankdev.brtickets.shared.exception.VenueNotFoundException;
import com.yankdev.brtickets.shared.security.JwtUtils;
import com.yankdev.brtickets.shared.security.SecurityConfig;
import com.yankdev.brtickets.venue.dto.VenueRequestDTO;
import com.yankdev.brtickets.venue.dto.VenueResponseDTO;
import com.yankdev.brtickets.venue.model.enums.VenueEnum;
import com.yankdev.brtickets.venue.service.VenueService;
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

@WebMvcTest(controllers = VenueController.class)
@Import(SecurityConfig.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VenueService venueService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID venueId;
    private VenueRequestDTO request;
    private VenueResponseDTO response;

    @BeforeEach
    void setUp() {
        venueId = UUID.randomUUID();

        request = new VenueRequestDTO();
        request.setName("Allianz Parque");
        request.setDescription("Arena in Sao Paulo");
        request.setType(VenueEnum.ARENA);
        request.setStreet("Av. Francisco Matarazzo, 1705");
        request.setCity("Sao Paulo");
        request.setState("SP");
        request.setZipCode("05001-200");
        request.setCountry("Brazil");
        request.setCapacity(43000);

        response = new VenueResponseDTO();
        response.setVenueId(venueId);
        response.setName("Allianz Parque");
        response.setType(VenueEnum.ARENA);
        response.setCity("Sao Paulo");
        response.setCapacity(43000);
        response.setActive(true);
    }

    @Test
    @DisplayName("POST /venues returns 201 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void createVenueReturnsCreatedForAdmin() throws Exception {
        when(venueService.createVenue(any(VenueRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.venueId").value(venueId.toString()))
                .andExpect(jsonPath("$.name").value("Allianz Parque"))
                .andExpect(jsonPath("$.capacity").value(43000));
    }

    @Test
    @DisplayName("POST /venues returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void createVenueReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(post("/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(venueService, never()).createVenue(any());
    }

    @Test
    @DisplayName("POST /venues returns 400 when the capacity is invalid")
    @WithMockUser(roles = "ADMIN")
    void createVenueReturnsBadRequest() throws Exception {
        when(venueService.createVenue(any(VenueRequestDTO.class)))
                .thenThrow(new IllegalVenueCapacityException("Venue needs to have at least 1 seat."));

        mockMvc.perform(post("/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /venues/{venueId} is public and returns 200")
    @WithAnonymousUser
    void findVenueByIdReturnsOk() throws Exception {
        when(venueService.findVenueById(venueId)).thenReturn(response);

        mockMvc.perform(get("/venues/{venueId}", venueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Sao Paulo"));
    }

    @Test
    @DisplayName("GET /venues/{venueId} returns 404 when the venue does not exist")
    @WithAnonymousUser
    void findVenueByIdReturnsNotFound() throws Exception {
        when(venueService.findVenueById(venueId)).thenThrow(new VenueNotFoundException("Venue not found."));

        mockMvc.perform(get("/venues/{venueId}", venueId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /venues?city= returns the active venues of the city")
    @WithAnonymousUser
    void findAllVenuesReturnsOk() throws Exception {
        when(venueService.findAllActiveVenues("Sao Paulo")).thenReturn(List.of(response));

        mockMvc.perform(get("/venues").param("city", "Sao Paulo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Allianz Parque"));
    }

    @Test
    @DisplayName("GET /venues without the city param returns 400")
    @WithAnonymousUser
    void findAllVenuesRequiresCityParam() throws Exception {
        mockMvc.perform(get("/venues"))
                .andExpect(status().isBadRequest());

        verify(venueService, never()).findAllActiveVenues(any());
    }

    @Test
    @DisplayName("PATCH /venues/{venueId} returns 200 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void updateVenueReturnsOkForAdmin() throws Exception {
        response.setName("New Arena Name");
        when(venueService.updateVenue(eq(venueId), any(VenueRequestDTO.class))).thenReturn(response);

        mockMvc.perform(patch("/venues/{venueId}", venueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Arena Name"));
    }

    @Test
    @DisplayName("PATCH /venues/{venueId} returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void updateVenueReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(patch("/venues/{venueId}", venueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(venueService, never()).updateVenue(any(), any());
    }

    @Test
    @DisplayName("DELETE /venues/{venueId} returns 204 for an ADMIN")
    @WithMockUser(roles = "ADMIN")
    void deleteVenueReturnsNoContentForAdmin() throws Exception {
        mockMvc.perform(delete("/venues/{venueId}", venueId))
                .andExpect(status().isNoContent());

        verify(venueService).deactivateVenue(venueId);
    }

    @Test
    @DisplayName("DELETE /venues/{venueId} returns 403 for a common USER")
    @WithMockUser(roles = "USER")
    void deleteVenueReturnsForbiddenForUser() throws Exception {
        mockMvc.perform(delete("/venues/{venueId}", venueId))
                .andExpect(status().isForbidden());

        verify(venueService, never()).deactivateVenue(any());
    }
}
