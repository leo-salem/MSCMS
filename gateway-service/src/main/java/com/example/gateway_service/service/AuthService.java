package com.example.gateway_service.service;

import com.example.gateway_service.dto.AdminCreateUserRequest;
import com.example.gateway_service.dto.SignupRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.*;

@Service
@Slf4j
public class AuthService {

    @Value("${keycloak.auth.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.auth.realm}")
    private String realm;

    @Value("${keycloak.auth.client-id}")
    private String clientId;

    @Value("${keycloak.auth.admin-username}")
    private String adminUsername;

    @Value("${keycloak.auth.admin-password}")
    private String adminPassword;

    private final RestClient restClient = RestClient.create();

    // ======================== LOGIN ========================

    public Map<String, Object> login(String username, String password) {
        String url = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        String formBody = buildForm(Map.of(
                "client_id", clientId,
                "grant_type", "password",
                "username", username,
                "password", password));

        log.debug("Login attempt for user: {}", username);

        Map<String, Object> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new RuntimeException("Login failed: no access token returned");
        }

        log.info("User {} logged in successfully", username);
        return response;
    }

    // ======================== REFRESH ========================

    public Map<String, Object> refresh(String refreshToken) {
        String url = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        String formBody = buildForm(Map.of(
                "client_id", clientId,
                "grant_type", "refresh_token",
                "refresh_token", refreshToken));

        Map<String, Object> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new RuntimeException("Token refresh failed");
        }

        return response;
    }

    // ======================== LOGOUT ========================

    public void logout(String refreshToken) {
        String url = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        String formBody = buildForm(Map.of(
                "client_id", clientId,
                "refresh_token", refreshToken));

        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .retrieve()
                .toBodilessEntity();

        log.info("User logged out successfully");
    }

    // ======================== SIGNUP (FAN) ========================

    public Map<String, Object> signup(SignupRequest request) {
        // 1. Get admin token
        String adminToken = getAdminToken();

        // 2. Create user in Keycloak
        String userId = createKeycloakUser(adminToken, request.getUsername(), request.getEmail(),
                request.getPassword(), request.getFirstName(), request.getLastName(),
                request.getAge(), request.getGender(), request.getPhone(), request.getAddress());

        // 3. Assign FAN realm role
        assignRealmRole(adminToken, userId, "FAN");

        // 4. Auto-login the new user to return tokens
        Map<String, Object> tokens = login(request.getUsername(), request.getPassword());

        log.info("New fan user registered: {}", request.getUsername());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "User registered successfully");
        result.put("keycloakId", userId);
        result.putAll(tokens);
        return result;
    }

    // ======================== ADMIN CREATE USER ========================

    public Map<String, Object> adminCreateUser(AdminCreateUserRequest request, String authHeader) {
        // 1. Get admin token
        String adminToken = getAdminToken();

        // 2. Create user in Keycloak
        String userId = createKeycloakUser(adminToken, request.getUsername(), request.getEmail(),
                request.getPassword(), request.getFirstName(), request.getLastName(),
                request.getAge(), request.getGender(), request.getPhone(), request.getAddress());

        // 3. Assign the requested realm role
        assignRealmRole(adminToken, userId, request.getRole().toUpperCase());

        log.info("Admin created user {} with role {}", request.getUsername(), request.getRole());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "User created successfully");
        result.put("keycloakId", userId);
        result.put("username", request.getUsername());
        result.put("role", request.getRole());
        return result;
    }

    // ======================== HELPERS ========================

    private String getAdminToken() {
        String url = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";

        String formBody = buildForm(Map.of(
                "client_id", "admin-cli",
                "grant_type", "password",
                "username", adminUsername,
                "password", adminPassword));

        Map<String, Object> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new RuntimeException("Failed to obtain Keycloak admin token");
        }

        return (String) response.get("access_token");
    }

    private String createKeycloakUser(String adminToken, String username, String email,
            String password, String firstName, String lastName,
            Integer age, String gender, String phone, String address) {
        String url = keycloakServerUrl + "/admin/realms/" + realm + "/users";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("enabled", true);
        payload.put("emailVerified", true);
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);

        // Credentials
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", password,
                "temporary", false);
        payload.put("credentials", List.of(credential));

        // Custom attributes
        Map<String, List<String>> attributes = new LinkedHashMap<>();
        if (age != null)
            attributes.put("age", List.of(String.valueOf(age)));
        if (gender != null)
            attributes.put("gender", List.of(gender));
        if (phone != null)
            attributes.put("phone", List.of(phone));
        if (address != null)
            attributes.put("address", List.of(address));
        if (!attributes.isEmpty())
            payload.put("attributes", attributes);

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(Void.class);

            URI location = response.getHeaders().getLocation();
            if (location == null) {
                throw new RuntimeException("Keycloak did not return user ID");
            }

            return location.getPath().substring(location.getPath().lastIndexOf('/') + 1);

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new RuntimeException(
                        "User with username '" + username + "' or email '" + email + "' already exists");
            }
            throw new RuntimeException("Failed to create user in Keycloak: " + ex.getResponseBodyAsString());
        }
    }

    private void assignRealmRole(String adminToken, String userId, String roleName) {
        // First get the role representation
        String roleUrl = keycloakServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;

        Map<String, Object> role = restClient.get()
                .uri(roleUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(Map.class);

        if (role == null) {
            throw new RuntimeException("Role not found: " + roleName);
        }

        // Then assign the role to the user
        String assignUrl = keycloakServerUrl + "/admin/realms/" + realm
                + "/users/" + userId + "/role-mappings/realm";

        restClient.post()
                .uri(assignUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();

        log.debug("Assigned realm role {} to user {}", roleName, userId);
    }

    private String buildForm(Map<String, String> params) {
        StringBuilder body = new StringBuilder();
        params.forEach((key, value) -> {
            if (body.length() > 0)
                body.append("&");
            body.append(key).append("=").append(value);
        });
        return body.toString();
    }
}
