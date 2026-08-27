package com.interviewiq.interviewstarter.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }


    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // =========================================================
    // AUTHENTICATION MANAGER
    // =========================================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }


    // =========================================================
    // SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http


                // -------------------------------------------------
                // CSRF
                // -------------------------------------------------
                // Disabled because we are using JWT authentication
                // instead of session-based authentication.
                // -------------------------------------------------
                .csrf(csrf -> csrf.disable())


                // -------------------------------------------------
                // SESSION MANAGEMENT
                // -------------------------------------------------
                // JWT authentication is stateless.
                // The server does not maintain login sessions.
                // -------------------------------------------------
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                // -------------------------------------------------
                // EXCEPTION HANDLING
                // -------------------------------------------------
                .exceptionHandling(exception -> exception

                        // User is not authenticated
                        .authenticationEntryPoint(
                                (request, response, authException) -> {

                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED
                                    );

                                    response.setContentType(
                                            "application/json"
                                    );

                                    response.getWriter().write("""
                                            {
                                                "success": false,
                                                "message": "Authentication required"
                                            }
                                            """);
                                }
                        )


                        // User is authenticated but does not
                        // have permission to access the resource
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {

                                    response.setStatus(
                                            HttpServletResponse.SC_FORBIDDEN
                                    );

                                    response.setContentType(
                                            "application/json"
                                    );

                                    response.getWriter().write("""
                                            {
                                                "success": false,
                                                "message": "Access denied"
                                            }
                                            """);
                                }
                        )
                )


                // -------------------------------------------------
                // AUTHORIZATION RULES
                // -------------------------------------------------
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/signup.html",
                                "/dashboard.html",
                                "/setup-role.html",
                                "/experience.html",
                                "/difficulty.html",
                                "/duration.html",
                                "/live-interview.html",
                                "/submitting.html",
                                "/result.html",

                                "/style.css",
                                "/dashboard.css",
                                "/dashboard.js",
                                "/api.js"
                        ).permitAll()

                        .requestMatchers("/auth/**").permitAll()

                        .anyRequest().authenticated()
                )


                // -------------------------------------------------
                // JWT FILTER
                // -------------------------------------------------
                // Our JWT filter runs before Spring Security's
                // UsernamePasswordAuthenticationFilter.
                // -------------------------------------------------
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }
}