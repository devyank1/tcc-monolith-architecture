package com.yankdev.brtickets.shared.security;

import com.yankdev.brtickets.user.model.UserModel;
import com.yankdev.brtickets.user.model.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private static final String SECRET = "VwjZLRV0UswHzhavZnR1hHCCNnyIQAvz+AOaCTYWfbc=";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LWZvci10ZXN0cy0zMmJ5dGVz";
    private static final long ONE_DAY = 86_400_000L;

    private JwtUtils jwtUtils;
    private UserModel user;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expiration", ONE_DAY);

        user = new UserModel();
        user.setUserId(UUID.randomUUID());
        user.setEmail("yan@brtickets.com");
        user.setRole(UserRole.ADMIN);
    }

    @Test
    @DisplayName("generateToken produces a signed JWT with three parts")
    void generatesSignedToken() {
        String token = jwtUtils.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("the token carries the email as subject plus the role and userId claims")
    void tokenCarriesUserClaims() {
        String token = jwtUtils.generateToken(user);

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("yan@brtickets.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("userId", String.class)).isEqualTo(user.getUserId().toString());
        assertThat(claims.getIssuedAt()).isNotNull();
    }

    @Test
    @DisplayName("extractEmail reads back the email that was signed")
    void extractsEmailFromToken() {
        String token = jwtUtils.generateToken(user);

        assertThat(jwtUtils.extractEmail(token)).isEqualTo("yan@brtickets.com");
    }

    @Test
    @DisplayName("getExpiration returns a date about one day ahead")
    void returnsExpirationDate() {
        String token = jwtUtils.generateToken(user);

        Date expiration = jwtUtils.getExpiration(token);

        assertThat(expiration)
                .isAfter(new Date(System.currentTimeMillis() + ONE_DAY - 60_000))
                .isBefore(new Date(System.currentTimeMillis() + ONE_DAY + 60_000));
    }

    @Test
    @DisplayName("isTokenValid accepts a fresh token issued for the same email")
    void acceptsValidToken() {
        String token = jwtUtils.generateToken(user);

        assertThat(jwtUtils.isTokenValid(token, "yan@brtickets.com")).isTrue();
    }

    @Test
    @DisplayName("isTokenValid rejects a token when the email does not match")
    void rejectsTokenOfAnotherEmail() {
        String token = jwtUtils.generateToken(user);

        assertThat(jwtUtils.isTokenValid(token, "someone.else@brtickets.com")).isFalse();
    }

    @Test
    @DisplayName("an expired token is rejected with ExpiredJwtException instead of returning false")
    void expiredTokenThrows() {
        ReflectionTestUtils.setField(jwtUtils, "expiration", -ONE_DAY);
        String expiredToken = jwtUtils.generateToken(user);

        assertThatThrownBy(() -> jwtUtils.isTokenValid(expiredToken, "yan@brtickets.com"))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("a token signed with another secret fails the signature check")
    void rejectsTokenSignedWithAnotherKey() {
        String foreignToken = Jwts.builder()
                .subject("yan@brtickets.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ONE_DAY))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(OTHER_SECRET)))
                .compact();

        assertThatThrownBy(() -> jwtUtils.extractEmail(foreignToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("a malformed token is rejected")
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtUtils.extractEmail("this-is-not-a-jwt"))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("two tokens for the same user are both valid")
    void generatesValidTokensRepeatedly() {
        String first = jwtUtils.generateToken(user);
        String second = jwtUtils.generateToken(user);

        assertThat(jwtUtils.isTokenValid(first, "yan@brtickets.com")).isTrue();
        assertThat(jwtUtils.isTokenValid(second, "yan@brtickets.com")).isTrue();
    }
}
