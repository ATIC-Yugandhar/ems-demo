package com.ems.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${keycloak.resource}")
    private String clientId;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests(authz -> authz
                // Public endpoints
                .antMatchers("/health", "/migrate-passwords", "/api/auth/**").permitAll()
                .antMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs", "/v3/api-docs/**").permitAll()
                .antMatchers("/webjars/**").permitAll()
                // READ operations
                .antMatchers("GET", "/api/employees/search").hasAnyRole("READ_EMPLOYEES", "FULL_ACCESS", "CLIENT_READ")
                .antMatchers("GET", "/api/employees/{id}").hasAnyRole("READ_EMPLOYEES", "FULL_ACCESS", "CLIENT_READ")
                // CREATE operations
                .antMatchers("POST", "/api/employees/add").hasAnyRole("CREATE_EMPLOYEES", "FULL_ACCESS", "CLIENT_WRITE")
                .antMatchers("POST", "/api/employees/add-Multiple").hasAnyRole("FULL_ACCESS", "CLIENT_WRITE")
                .antMatchers("POST", "/api/employees/bulk-upload").hasAnyRole("FULL_ACCESS", "CLIENT_WRITE")
                // UPDATE operations
                .antMatchers("PUT", "/api/employees/update/**").hasAnyRole("UPDATE_EMPLOYEES", "FULL_ACCESS", "CLIENT_WRITE")
                // DELETE operations
                .antMatchers("DELETE", "/api/employees/delete/**").hasAnyRole("DELETE_EMPLOYEES", "FULL_ACCESS", "CLIENT_WRITE")
                // Admin endpoints
                .antMatchers("/api/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Extract user realm roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") != null) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList()));
            }

            // Extract client resource roles
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
                if (clientAccess != null && clientAccess.get("roles") != null) {
                    @SuppressWarnings("unchecked")
                    List<String> clientRoles = (List<String>) clientAccess.get("roles");
                    authorities.addAll(clientRoles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()));
                }
            }

            return authorities;
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}