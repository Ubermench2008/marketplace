package ru.nsu.marketplace.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtProperties properties;

    @Bean
    SecretKey jwtSecretKey() {
        byte[] secretBytes = Decoders.BASE64.decode(
                properties.secretKey()
        );

        return Keys.hmacShaKeyFor(secretBytes);
    }

}
