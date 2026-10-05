package ru.nsu.marketplace.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.nsu.marketplace.config.JwtProperties;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.security.JwtUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JwtServiceTest {
    private JwtService jwtService;
    private SecretKey jwtSecretKey;

    @BeforeEach
    void setUp() {
        jwtSecretKey = Keys.hmacShaKeyFor(
                "test-secret-key-for-jwt-service-tests".getBytes(StandardCharsets.UTF_8)
        );
        jwtService = new JwtService(
                new JwtProperties("unused", Duration.ofMinutes(15)),
                jwtSecretKey
        );
    }

    @Test
    void shouldParseCreatedTokenToOriginalUserIdAndRoles() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(10L);
        userEntity.setRoles(Set.of(Role.BUYER, Role.SELLER));

        String token = jwtService.createAccessToken(userEntity);

        JwtUser jwtUser = jwtService.parseToken(token);

        assertThat(jwtUser.userId()).isEqualTo(10L);
        assertThat(jwtUser.userRoles()).isEqualTo(Set.of(Role.BUYER, Role.SELLER));
    }

    @Test
    void shouldThrowJwtExceptionWhenTokenRolesAreTampered() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(10L);
        userEntity.setRoles(Set.of(Role.BUYER));

        String token = jwtService.createAccessToken(userEntity);

        String[] parts = token.split("\\.");

        String payload = new String(Decoders.BASE64URL.decode(parts[1]), StandardCharsets.UTF_8);

        String tampered = payload.replace(
                "\"roles\":[\"BUYER\"]",
                "\"roles\":[\"BUYER\",\"ADMIN\"]"
        );

        parts[1] = Encoders.BASE64URL.encode(tampered.getBytes(StandardCharsets.UTF_8));

        String tamperedToken = String.join(".", parts);

        assertThrows(JwtException.class, () -> jwtService.parseToken(tamperedToken));
    }

    @Test
    void shouldThrowExpiredJwtExceptionWhenTokenIsExpired() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(10L);
        userEntity.setRoles(Set.of(Role.BUYER));

        JwtService expiredTokenService = new JwtService(
                new JwtProperties("unused", Duration.ofSeconds(-1)),
                jwtSecretKey
        );
        String expiredToken = expiredTokenService.createAccessToken(userEntity);

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.parseToken(expiredToken)
        );
    }
}
