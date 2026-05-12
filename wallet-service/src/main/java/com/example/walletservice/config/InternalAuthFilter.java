package com.example.walletservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates X-Internal-Service-Token on /internal/** requests and grants ROLE_INTERNAL_SERVICE.
 * Other (user-facing) requests pass through untouched, falling back to JWT auth.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Service-Token";
    public static final String INTERNAL_ROLE = "ROLE_INTERNAL_SERVICE";

    @Value("${mscms.internal.token}")
    private String configuredToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/internal/")) {
            String provided = request.getHeader(HEADER);
            if (provided != null && constantTimeEquals(provided, configuredToken)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "internal-service",
                        null,
                        List.of(new SimpleGrantedAuthority(INTERNAL_ROLE))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("Rejected internal call to {} - missing/invalid {} header", path, HEADER);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid internal service token\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
