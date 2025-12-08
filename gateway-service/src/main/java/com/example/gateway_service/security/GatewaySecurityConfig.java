package com.example.gateway_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/v3/api-docs.json").permitAll()
                        .requestMatchers("/webjars/**", "/swagger-resources/**", "/swagger-ui.html/**").permitAll()
                        .requestMatchers("/api-docs/**", "/api-docs.yaml", "/api-docs.json").permitAll()

                        .requestMatchers("/actuator/**", "/eureka/**").permitAll()

                        // ADMIN-only
                        .requestMatchers(HttpMethod.POST, "/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")
                        .requestMatchers("/sport-managers/**").hasRole("ADMIN")
                        .requestMatchers("/team-managers/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**").hasRole("ADMIN")
                        .requestMatchers("/sponsors/**").hasRole("ADMIN")
                        .requestMatchers("/national-teams/**").hasRole("ADMIN")

                        // Sport Manager can manage team managers + view staff
                        .requestMatchers("/sport-managers/{id}/team-managers/**").hasRole("SPORT_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/sport-managers/**").hasRole("SPORT_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/staff/team/**").hasRole("SPORT_MANAGER")


                        .requestMatchers(HttpMethod.PUT, "/team-managers/{id}/assign-staff/**").hasRole("TEAM_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/players/team/**").hasRole("TEAM_MANAGER")


                        .requestMatchers(HttpMethod.GET, "/players/**").hasAnyRole(
                                "HEAD_COACH", "ASSISTANT_COACH", "SPECIFIC_COACH",
                                "FITNESS_COACH", "PERFORMANCE_ANALYST", "TEAM_DOCTOR",
                                "PHYSIOTHERAPIST"
                        )

                        .requestMatchers("/scouts/**").hasRole("SCOUT")

                        .requestMatchers("/sponsors/**").hasRole("SPONSOR")

                        .requestMatchers("/national-teams/**").hasRole("NATIONAL_TEAM")

                        .requestMatchers("/fans/**").hasRole("FAN")

                        // any other endpoint → must be authenticated
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));

        return http.build();
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // realm_access (realm roles)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof Collection<?> roles) {
                for (Object role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
        }

        return authorities;
    }
}
