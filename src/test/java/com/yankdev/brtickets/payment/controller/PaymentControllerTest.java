package com.yankdev.brtickets.payment.controller;

import com.yankdev.brtickets.payment.dto.PaymentRequestDTO;
import com.yankdev.brtickets.payment.dto.PaymentResponseDTO;
import com.yankdev.brtickets.payment.model.enums.PaymentMethodEnum;
import com.yankdev.brtickets.payment.model.enums.PaymentStatusEnum;
import com.yankdev.brtickets.payment.service.PaymentService;
import com.yankdev.brtickets.shared.exception.AccessDeniedException;
import com.yankdev.brtickets.shared.exception.BookingNotFoundException;
import com.yankdev.brtickets.shared.exception.IllegalPaymentStatusException;
import com.yankdev.brtickets.shared.exception.IllegalPaymentStatusRefundException;
import com.yankdev.brtickets.shared.exception.PaymentNotFoundException;
import com.yankdev.brtickets.shared.security.JwtUtils;
import com.yankdev.brtickets.shared.security.SecurityConfig;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID bookingId;
    private UUID paymentId;
    private PaymentRequestDTO request;
    private PaymentResponseDTO response;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        request = new PaymentRequestDTO();
        request.setBookingId(bookingId);
        request.setMethod(PaymentMethodEnum.PIX);
        request.setGatewayTransactionId("tx-123456");

        response = new PaymentResponseDTO();
        response.setId(paymentId);
        response.setBookingId(bookingId);
        response.setMethod(PaymentMethodEnum.PIX);
        response.setStatus(PaymentStatusEnum.PENDING);
        response.setAmount(new BigDecimal("250.00"));
        response.setPaidAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /payments?bookingId= returns 201 with a PENDING payment")
    @WithMockUser
    void processPaymentReturnsCreated() throws Exception {
        when(paymentService.processPayment(eq(bookingId), any(PaymentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/payments")
                        .param("bookingId", bookingId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.method").value("PIX"));
    }

    @Test
    @DisplayName("POST /payments without the bookingId param returns 400")
    @WithMockUser
    void processPaymentRequiresBookingIdParam() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).processPayment(any(), any());
    }

    @Test
    @DisplayName("POST /payments returns 404 when the booking does not exist")
    @WithMockUser
    void processPaymentReturnsNotFound() throws Exception {
        when(paymentService.processPayment(eq(bookingId), any(PaymentRequestDTO.class)))
                .thenThrow(new BookingNotFoundException("We cannot found your booking."));

        mockMvc.perform(post("/payments")
                        .param("bookingId", bookingId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /payments returns 403 for a booking of another user")
    @WithMockUser
    void processPaymentReturnsForbidden() throws Exception {
        when(paymentService.processPayment(eq(bookingId), any(PaymentRequestDTO.class)))
                .thenThrow(new AccessDeniedException("You have not permission to process this payment"));

        mockMvc.perform(post("/payments")
                        .param("bookingId", bookingId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /payments requires authentication")
    @WithAnonymousUser
    void processPaymentRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/payments")
                        .param("bookingId", bookingId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(paymentService, never()).processPayment(any(), any());
    }

    @Test
    @DisplayName("POST /payments/{paymentId}/confirm returns 200 with APPROVED")
    @WithMockUser
    void confirmPaymentReturnsOk() throws Exception {
        response.setStatus(PaymentStatusEnum.APPROVED);
        when(paymentService.confirmPayment(paymentId)).thenReturn(response);

        mockMvc.perform(post("/payments/{paymentId}/confirm", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /payments/{paymentId}/confirm returns 400 when the status is not PENDING")
    @WithMockUser
    void confirmPaymentReturnsBadRequest() throws Exception {
        when(paymentService.confirmPayment(paymentId))
                .thenThrow(new IllegalPaymentStatusException("Your payment must be PENDING, because you didn't pay yet."));

        mockMvc.perform(post("/payments/{paymentId}/confirm", paymentId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /payments/{paymentId}/confirm returns 404 when the payment does not exist")
    @WithMockUser
    void confirmPaymentReturnsNotFound() throws Exception {
        when(paymentService.confirmPayment(paymentId))
                .thenThrow(new PaymentNotFoundException("We could not find your payment"));

        mockMvc.perform(post("/payments/{paymentId}/confirm", paymentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /payments/{paymentId}/refund returns 204")
    @WithMockUser
    void refundPaymentReturnsNoContent() throws Exception {
        mockMvc.perform(post("/payments/{paymentId}/refund", paymentId))
                .andExpect(status().isNoContent());

        verify(paymentService).refundPayment(paymentId);
    }

    @Test
    @DisplayName("POST /payments/{paymentId}/refund returns 400 when it is already refunded")
    @WithMockUser
    void refundPaymentReturnsBadRequest() throws Exception {
        doThrow(new IllegalPaymentStatusRefundException("Your payment has already refunded."))
                .when(paymentService).refundPayment(paymentId);

        mockMvc.perform(post("/payments/{paymentId}/refund", paymentId))
                .andExpect(status().isBadRequest());
    }
}
