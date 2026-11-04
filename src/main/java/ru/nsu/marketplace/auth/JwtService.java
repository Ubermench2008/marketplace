package ru.nsu.marketplace.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.marketplace.user.Role;
import ru.nsu.marketplace.user.UserEntity;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;
    private final SecretKey jwtSecretKey;

    public String createAccessToken(UserEntity user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::name)
                .toList();

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plus(jwtProperties.accessTokenTtl())
                ))
                .signWith(jwtSecretKey)
                .compact();
    }

    public JwtUser parseToken(String jwtToken) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(jwtToken)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());

        List<?> roles = claims.get("roles", List.class);

        Set<Role> roleSet = roles.stream()
                .map(Object::toString)
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        return new JwtUser(userId, roleSet);
    }
}
