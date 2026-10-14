package dev.devices.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

/**
 * HTTP Basic authentication with two roles:
 * <ul>
 *     <li>{@code VIEWER}: read-only access (GET).</li>
 *     <li>{@code ADMIN}: full access (create, update, delete).</li>
 * </ul>
 * API documentation and the health endpoint stay public.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    static final String ROLE_ADMIN = "ADMIN";
    static final String ROLE_VIEWER = "VIEWER";

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health/**",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, ObjectMapper objectMapper) throws Exception {

        return http
                // Stateless REST API without cookies: CSRF protection is not needed.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(ROLE_VIEWER, ROLE_ADMIN)
                        .requestMatchers("/api/**").hasRole(ROLE_ADMIN)
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setHeader("WWW-Authenticate", "Basic realm=\"Devices API\"");
                            writeProblem(objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                                    "Unauthorized", "Valid credentials are required.");
                        })
                        .accessDeniedHandler((request, response, ex) ->
                                writeProblem(objectMapper, request, response, HttpStatus.FORBIDDEN,
                                        "Forbidden", "You do not have permission to perform this operation.")))
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            SecurityProperties properties, PasswordEncoder passwordEncoder) {

        return new InMemoryUserDetailsManager(
                User.withUsername(properties.admin().username())
                        .password(passwordEncoder.encode(properties.admin().password()))
                        .roles(ROLE_ADMIN)
                        .build(),
                User.withUsername(properties.viewer().username())
                        .password(passwordEncoder.encode(properties.viewer().password()))
                        .roles(ROLE_VIEWER)
                        .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Passwords are stored BCrypt-hashed in memory, never in plain text.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Writes 401/403 as RFC 9457 Problem Details, matching the other API errors.
     */
    private static void writeProblem(
            ObjectMapper objectMapper,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail) throws IOException {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
