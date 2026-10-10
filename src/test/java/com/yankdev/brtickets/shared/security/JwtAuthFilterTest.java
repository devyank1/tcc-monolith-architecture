package com.yankdev.brtickets.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String EMAIL = "yan@brtickets.com";
    private static final String TOKEN = "valid.jwt.token";

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails userDetails() {
        return User.builder()
                .username(EMAIL)
                .password("hashed-password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("authenticates the request when the Bearer token is valid")
    void authenticatesValidToken() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtUtils.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails());
        when(jwtUtils.isTokenValid(TOKEN, EMAIL)).thenReturn(true);

        jwtAuthFilter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(EMAIL);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
        assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("lets the request through unauthenticated when there is no Authorization header")
    void skipsWhenHeaderIsMissing() throws Exception {
        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    @DisplayName("ignores an Authorization header that is not a Bearer token")
    void skipsWhenSchemeIsNotBearer() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    @DisplayName("does not authenticate when the token fails validation")
    void doesNotAuthenticateInvalidToken() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtUtils.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails());
        when(jwtUtils.isTokenValid(TOKEN, EMAIL)).thenReturn(false);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("does not authenticate when the token carries no email")
    void doesNotAuthenticateWhenEmailIsNull() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtUtils.extractEmail(TOKEN)).thenReturn(null);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("keeps an authentication that is already in the context")
    void keepsExistingAuthentication() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("already@brtickets.com", "password"));
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtUtils.extractEmail(TOKEN)).thenReturn(EMAIL);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("already@brtickets.com");
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }
}
