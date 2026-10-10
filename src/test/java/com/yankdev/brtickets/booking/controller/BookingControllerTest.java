package com.yankdev.brtickets.booking.controller;

import com.yankdev.brtickets.booking.dto.BookingRequestDTO;
import com.yankdev.brtickets.booking.dto.BookingResponseDTO;
import com.yankdev.brtickets.booking.model.enums.BookingStatusEnum;
import com.yankdev.brtickets.booking.service.BookingService;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import com.yankdev.brtickets.shared.exception.AccessDeniedException;
import com.yankdev.brtickets.shared.exception.BookingNotFoundException;
import com.yankdev.brtickets.shared.exception.IllegalBookingCancellingException;
import com.yankdev.brtickets.shared.exception.IllegalTicketOnBookingException;
import com.yankdev.brtickets.shared.security.AuthenticatedUserProvider;
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

import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID userId;
    private UUID bookingId;
    private BookingRequestDTO request;
    private BookingResponseDTO response;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        request = new BookingRequestDTO();
        request.setTicketsId(List.of(UUID.randomUUID(), UUID.randomUUID()));
        request.setPaymentMethod(PaymentMethodEnum.PIX);

        response = new BookingResponseDTO();
        response.setBookingId(bookingId);
        response.setUserId(userId);
        response.setStatus(BookingStatusEnum.CONFIRMED);
        response.setTotalAmount(new BigDecimal("250.00"));
        response.setPaymentMethod(PaymentMethodEnum.PIX);
        response.setCreatedAt(LocalDateTime.now());
        response.setConfirmedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /bookings returns 201 with the confirmed booking")
    @WithMockUser
    void createBookingReturnsCreated() throws Exception {
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(userId);
        when(bookingService.createBooking(eq(userId), any(BookingRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(250.00));
    }

    @Test
    @DisplayName("POST /bookings returns 400 when no ticket id is sent")
    @WithMockUser
    void createBookingRejectsEmptyTicketList() throws Exception {
        BookingRequestDTO invalid = new BookingRequestDTO();
        invalid.setTicketsId(List.of());
        invalid.setPaymentMethod(PaymentMethodEnum.PIX);

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).createBooking(any(), any());
    }

    @Test
    @DisplayName("POST /bookings returns 400 when a ticket is not available")
    @WithMockUser
    void createBookingReturnsBadRequestForUnavailableTicket() throws Exception {
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(userId);
        when(bookingService.createBooking(eq(userId), any(BookingRequestDTO.class)))
                .thenThrow(new IllegalTicketOnBookingException("One or more tickets are not available."));

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /bookings requires authentication")
    @WithAnonymousUser
    void createBookingRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).createBooking(any(), any());
    }

    @Test
    @DisplayName("GET /bookings/{bookingId} returns the booking")
    @WithMockUser
    void findBookingByIdReturnsOk() throws Exception {
        when(bookingService.findBookingById(bookingId)).thenReturn(response);

        mockMvc.perform(get("/bookings/{bookingId}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()));
    }

    @Test
    @DisplayName("GET /bookings/{bookingId} returns 403 for someone else's booking")
    @WithMockUser
    void findBookingByIdReturnsForbiddenForOtherUser() throws Exception {
        when(bookingService.findBookingById(bookingId))
                .thenThrow(new AccessDeniedException("You have not permission to find bookings"));

        mockMvc.perform(get("/bookings/{bookingId}", bookingId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bookings/{bookingId} returns 404 when the booking does not exist")
    @WithMockUser
    void findBookingByIdReturnsNotFound() throws Exception {
        when(bookingService.findBookingById(bookingId))
                .thenThrow(new BookingNotFoundException("Booking not found"));

        mockMvc.perform(get("/bookings/{bookingId}", bookingId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /bookings lists the bookings of the authenticated user")
    @WithMockUser
    void findAllBookingByUserReturnsOk() throws Exception {
        when(authenticatedUserProvider.getCurrentUserId()).thenReturn(userId);
        when(bookingService.findAllByUser(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    @DisplayName("DELETE /bookings/{bookingId} returns 204")
    @WithMockUser
    void cancelBookingReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/bookings/{bookingId}", bookingId))
                .andExpect(status().isNoContent());

        verify(bookingService).cancelBooking(bookingId);
    }

    @Test
    @DisplayName("DELETE /bookings/{bookingId} returns 400 when it is already cancelled")
    @WithMockUser
    void cancelBookingReturnsBadRequestWhenAlreadyCancelled() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalBookingCancellingException("You cannot cancel a CANCELLED booking"))
                .when(bookingService).cancelBooking(bookingId);

        mockMvc.perform(delete("/bookings/{bookingId}", bookingId))
                .andExpect(status().isBadRequest());
    }
}
