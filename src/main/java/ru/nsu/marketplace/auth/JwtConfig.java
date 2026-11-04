package ru.nsu.marketplace.auth;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtProperties jwtProperties;

    @Bean
    SecretKey jwtSecretKey() {
        byte[] secretBytes = Decoders.BASE64.decode(
                jwtProperties.secretKey()
        );

        return Keys.hmacShaKeyFor(secretBytes);
    }

}
