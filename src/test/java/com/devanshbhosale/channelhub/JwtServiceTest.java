package com.devanshbhosale.channelhub;

import com.devanshbhosale.channelhub.domain.User;
import com.devanshbhosale.channelhub.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest extends IntegrationTestBase {

    @Value("${app.jwt.secret}")
    private String secret;

    @Test
    void issuedTokenParsesBackToSameUser() {
        User user = newUser(uniqueEmail("jwt"), "Password@123", UserRole.RESELLER, false);
        String token = jwtService.issue(user);
        Optional<User> parsed = jwtService.parse(token);
        assertThat(parsed).isPresent();
        assertThat(parsed.get().getEmail()).isEqualTo(user.getEmail());
        assertThat(parsed.get().getId()).isEqualTo(user.getId());
    }

    @Test
    void tokenExpiresWithin24Hours() {
        User user = newUser(uniqueEmail("jwt-exp"), "Password@123", UserRole.RESELLER, false);
        String token = jwtService.issue(user);
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();
        long minutesUntilExpiry = Duration.between(Instant.now(), claims.getExpiration().toInstant()).toMinutes();
        assertThat(minutesUntilExpiry).isBetween(23 * 60L, 24 * 60L);
    }
}
