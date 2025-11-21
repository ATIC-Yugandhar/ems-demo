package com.ems.service;

import com.ems.dto.LoginRequest;
import com.ems.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public LoginResponse login(LoginRequest loginRequest) {
        try {
            String tokenEndpoint = String.format("%s/realms/%s/protocol/openid-connect/token", 
                                                keycloakServerUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("username", loginRequest.getEmail());
            formData.add("password", loginRequest.getPassword());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                String accessToken = (String) responseBody.get("access_token");

                log.info("Login successful for user: {}", loginRequest.getEmail());
                return new LoginResponse(true, "Login successful", "Bearer " + accessToken);
            } else {
                log.warn("Login failed for user: {}", loginRequest.getEmail());
                return new LoginResponse(false, "Login failed", null);
            }

        } catch (Exception e) {
            log.error("Login error for user {}: {}", loginRequest.getEmail(), e.getMessage());
            return new LoginResponse(false, "Login failed: " + e.getMessage(), null);
        }
    }
}