package com.ems.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Slf4j
public class ClientUtils {

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            // For user authentication, the subject is the email
            // For client authentication, the subject is the client_id
            String subject = jwt.getSubject();
            
            // Check if it's a user token (email format) or client token
            if (subject != null && subject.contains("@")) {
                return subject;
            }
            
            // For client tokens, try to get preferred_username or return client_id
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null) {
                return preferredUsername;
            }
            
            return subject; // Return client_id
        }
        
        return "system";
    }

    public static String getClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            // Try to get client_id from azp claim (authorized party)
            String clientId = jwt.getClaimAsString("azp");
            if (clientId != null) {
                return clientId;
            }
            
            // Fallback to aud claim
            Object aud = jwt.getClaim("aud");
            if (aud instanceof String) {
                return (String) aud;
            }
            
            return jwt.getSubject();
        }
        
        return "unknown";
    }

    public static boolean isClientToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            String subject = jwt.getSubject();
            
            // If subject doesn't contain @ and doesn't look like email, it's likely a client token
            return subject != null && !subject.contains("@");
        }
        
        return false;
    }
}