package com.gymrat.controller;

import com.gymrat.dto.AdminMemberDto;
import com.gymrat.dto.MemberProfileDto;
import com.gymrat.security.AuthenticatedUser;
import com.gymrat.security.JwtUtil;
import com.gymrat.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MemberService memberService;

    // JwtUtil is a @Component wired into JwtAuthFilter; mock it so the filter
    // can be instantiated without a real secret key in the test slice.
    @MockBean
    JwtUtil jwtUtil;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser("admin@gym.com", "ADMIN", null),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private static UsernamePasswordAuthenticationToken memberAuth(Long memberId) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser("member@gym.com", "MEMBER", memberId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
    }

    private static AdminMemberDto sampleAdminDto() {
        return new AdminMemberDto(
                1L, "Jane Doe", "jane@example.com", "555-0100",
                LocalDate.of(2024, 1, 15), "ACTIVE",
                2L, "Premium",
                "CARD", "4111111111111111", "12/27", "321",
                "123 Main St", "Apt 4", "Springfield", "IL", "62701",
                LocalDate.of(2026, 4, 15)
        );
    }

    // -------------------------------------------------------------------------
    // Admin — contact & payment visibility
    // -------------------------------------------------------------------------

    @Test
    void adminGetById_returnsContactFields() throws Exception {
        when(memberService.getAdminDetail(1L)).thenReturn(sampleAdminDto());

        mockMvc.perform(get("/api/members/1").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.phone").value("555-0100"));
    }

    @Test
    void adminGetById_returnsPaymentFields() throws Exception {
        when(memberService.getAdminDetail(1L)).thenReturn(sampleAdminDto());

        mockMvc.perform(get("/api/members/1").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.cardNumber").value("4111111111111111"))
                .andExpect(jsonPath("$.cardExpiryDate").value("12/27"))
                .andExpect(jsonPath("$.streetAddress").value("123 Main St"))
                .andExpect(jsonPath("$.aptUnit").value("Apt 4"))
                .andExpect(jsonPath("$.city").value("Springfield"))
                .andExpect(jsonPath("$.state").value("IL"))
                .andExpect(jsonPath("$.zipCode").value("62701"));
    }

    @Test
    void adminGetById_memberWithNoPaymentData_stillReturnsOk() throws Exception {
        AdminMemberDto noPaymentDto = new AdminMemberDto(
                2L, "John Smith", "john@example.com", null,
                LocalDate.of(2025, 6, 1), "ACTIVE",
                1L, "Basic",
                null, null, null, null,
                null, null, null, null, null,
                null
        );
        when(memberService.getAdminDetail(2L)).thenReturn(noPaymentDto);

        mockMvc.perform(get("/api/members/2").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Smith"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    // -------------------------------------------------------------------------
    // Member — own profile access
    // -------------------------------------------------------------------------

    @Test
    void memberGetOwnProfile_returnsOk() throws Exception {
        MemberProfileDto profileDto = new MemberProfileDto(
                1L, "Jane Doe", "jane@example.com", "555-0100",
                "ACTIVE", 2L, "Premium",
                LocalDate.of(2026, 4, 15),
                "CARD", "4111111111111111", "12/27", "321",
                "123 Main St", null, "Springfield", "IL", "62701"
        );
        when(memberService.getMemberProfile(1L)).thenReturn(profileDto);

        mockMvc.perform(get("/api/members/1").with(authentication(memberAuth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    // -------------------------------------------------------------------------
    // Member — cross-member access prevention
    // -------------------------------------------------------------------------

    @Test
    void memberGetAnotherMembersProfile_returns403() throws Exception {
        when(memberService.getMemberProfile(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/members/99").with(authentication(memberAuth(1L))))
                .andExpect(status().isForbidden());
    }
}
