package com.kubee.pos.common.security;

import com.kubee.security.JwtAuthFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ Enabled CORS here
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**",
                                "/ws/**",
                                "/v1/razorpay/webhook"  // Called by Razorpay servers, no JWT — secured by HMAC signature instead
                        ).permitAll()
                        // Manager-only (owner / admin / manager); cashiers get 403. See PosPermissions.
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/**").hasAuthority(PosPermissions.MANAGE)
                        .requestMatchers(HttpMethod.POST, "/api/v1/catalog/**").hasAuthority(PosPermissions.MANAGE)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/catalog/**").hasAuthority(PosPermissions.MANAGE)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/catalog/**").hasAuthority(PosPermissions.MANAGE)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/catalog/**").hasAuthority(PosPermissions.MANAGE)
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/payments/*/refund").hasAuthority(PosPermissions.MANAGE)
                        .requestMatchers(HttpMethod.POST, "/api/v1/bills/*/cancel").hasAuthority(PosPermissions.MANAGE)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "Login required"))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, HttpStatus.FORBIDDEN, "Only a manager or the owner can do this"))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Same {code, message} envelope as every other API error. */
    private static void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":" + status.value() + ",\"message\":\"" + message + "\"}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "https://www.ez-hub.in",
                "https://app.ez-hub.in",
                "https://www.kubee.in",
                "https://kubee.in",
                "https://www.app.kubee.in",
                "https://app.kubee.in",
                "http://localhost:3000",
                "http://localhost:4200"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION)); // file names of report downloads
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}