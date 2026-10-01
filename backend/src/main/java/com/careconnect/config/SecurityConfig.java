package com.careconnect.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationFilter jwt, ObjectMapper json) throws Exception {
    http.csrf(c -> c.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, x) -> {
                          res.setStatus(401);
                          res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          json.writeValue(
                              res.getOutputStream(),
                              Map.of("error", "Sign in to access this resource."));
                        })
                    .accessDeniedHandler(
                        (req, res, x) -> {
                          res.setStatus(403);
                          res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          json.writeValue(
                              res.getOutputStream(),
                              Map.of("error", "You do not have access to this resource."));
                        }))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/api/health", "/api/auth/login", "/api/auth/register", "/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/auth/demo-session/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${careconnect.cors-origins}") String origins,
      @Value("${careconnect.production:false}") boolean production,
      @Value("${CORS_ORIGIN:}") String configuredOrigins) {
    if (production
        && (configuredOrigins.isBlank()
            || Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .anyMatch(
                    o ->
                        o.contains("localhost")
                            || o.contains("127.0.0.1")
                            || o.contains("[::1]")
                            || o.equals("*")
                            || o.equalsIgnoreCase("null"))))
      throw new IllegalStateException(
          "Set CORS_ORIGIN to the deployed frontend origin(s) before starting the production profile.");
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    c.setExposedHeaders(List.of("Location"));
    c.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);
    return s;
  }
}
