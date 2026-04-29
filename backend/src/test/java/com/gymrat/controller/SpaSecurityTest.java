package com.gymrat.controller;

import com.gymrat.config.SecurityConfig;
import com.gymrat.security.JwtAuthFilter;
import com.gymrat.security.JwtUtil;
import com.gymrat.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Explicitly import SecurityConfig + JwtAuthFilter so this slice exercises the
// real security chain; without this, @WebMvcTest falls back to Spring Boot's
// default chain which requires auth for every path and would mask matcher bugs.
@WebMvcTest(MemberController.class)
@Import({SpaFallbackAdvice.class, SecurityConfig.class, JwtAuthFilter.class})
class SpaSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MemberService memberService;

    @MockBean
    JwtUtil jwtUtil;

    @Test
    void apiEndpoint_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rootPath_unauthenticated_forwardsToIndex() throws Exception {
        // Security must permit anonymous access AND the SPA fallback must
        // forward to /index.html. forwardedUrl confirms both in one assertion:
        // a security block would short-circuit before any forward is recorded.
        mockMvc.perform(get("/"))
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void spaRoute_unauthenticated_forwardsToIndex() throws Exception {
        mockMvc.perform(get("/members"))
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void nestedSpaRoute_unauthenticated_forwardsToIndex() throws Exception {
        mockMvc.perform(get("/members/3/edit"))
                .andExpect(forwardedUrl("/index.html"));
    }
}
