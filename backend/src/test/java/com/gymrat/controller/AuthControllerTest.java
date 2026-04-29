package com.gymrat.controller;

import com.gymrat.config.SecurityConfig;
import com.gymrat.dto.LoginRequest;
import com.gymrat.dto.LoginResponse;
import com.gymrat.security.JwtAuthFilter;
import com.gymrat.security.JwtUtil;
import com.gymrat.service.AuthService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    // JwtUtil is wired into JwtAuthFilter; mock it so the filter can be instantiated.
    @MockBean
    private JwtUtil jwtUtil;

    // JwtAuthFilter must be a bean so SecurityConfig.filterChain can be created.
    // Without this, @WebMvcTest falls back to default security (CSRF on, login blocked).
    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void configureJwtFilter() throws Exception {
        // Stub the filter to pass requests through without setting any authentication.
        // This matches real behavior when no Authorization header is present (e.g., login).
        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2, FilterChain.class);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void login_withValidCredentials_returnsTokenAndRole() throws Exception {
        LoginRequest req = new LoginRequest("admin@gym.com", "Admin123!");
        LoginResponse res = new LoginResponse("test-token", "ADMIN", null, "admin@gym.com", "admin@gym.com");

        when(authService.login(any(LoginRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                .header("Origin", "http://localhost:5173")
                .contentType(APPLICATION_JSON)
                .content("""
                        {"email":"admin@gym.com","password":"Admin123!"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_withInvalidEmail_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("""
                        {"email":"invalid@gym.com","password":"wrongpassword"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_allowsCorsPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
    }

    @Test
    void login_allowsAnonymousAccess() throws Exception {
        // The login endpoint should be accessible without authentication
        LoginResponse res = new LoginResponse("test-token", "MEMBER", 1L, "member@gym.com", "John");
        when(authService.login(any(LoginRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("""
                        {"email":"member@gym.com","password":"Member123!"}
                        """))
                .andExpect(status().isOk());
    }
}
