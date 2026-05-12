package com.example.walletservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service("securityService")
@Slf4j
public class SecurityService {

    /** Keycloak subject (user UUID) from the current JWT, or null if no JWT principal. */
    public String getCurrentKeycloakId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    public boolean isCurrentUserByKeycloakId(String keycloakId) {
        String current = getCurrentKeycloakId();
        return current != null && current.equals(keycloakId);
    }
}
