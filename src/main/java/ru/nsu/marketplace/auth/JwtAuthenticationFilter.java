package ru.nsu.marketplace.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SecurityErrorWriter errorWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            JwtUser jwtUser = jwtService.parseToken(token);

            List<SimpleGrantedAuthority> authorities = jwtUser.userRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(
                            "ROLE_" + role.name()
                    ))
                    .toList();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    jwtUser.userId(),
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException exception) {
            log.warn("JWT authentication failed: method={}, path={}, reason={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getClass().getSimpleName());

            errorWriter.write(response, HttpStatus.UNAUTHORIZED, "Invalid access token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
