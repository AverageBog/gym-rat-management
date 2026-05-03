package com.gymrat.config;

import com.gymrat.controller.AuthController;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Locks SecurityConfig CORS behavior against silent dev/prod drift. Two patterns
// are configured here so we exercise the multi-origin case the prod profile uses.
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@TestPropertySource(properties =
        "app.cors.allowed-origin-patterns=http://localhost:*,https://*.a.run.app")
class SecurityConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2, FilterChain.class);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void preflight_fromAllowedLocalhostOrigin_returnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void preflight_fromAllowedCloudRunOrigin_returnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://gym-rat-management-abc-uc.a.run.app")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://gym-rat-management-abc-uc.a.run.app"));
    }

    @Test
    void post_fromDisallowedOrigin_isRejectedAsInvalidCors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "https://evil.example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x\",\"password\":\"y\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid CORS request"));
    }
}
