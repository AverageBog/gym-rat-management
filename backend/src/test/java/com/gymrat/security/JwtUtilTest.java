package com.gymrat.security;

import com.gymrat.entity.AppUser;
import com.gymrat.entity.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                jwtUtil,
                "secret",
                "test-only-jwt-secret-key-for-unit-and-integration-tests-not-for-production"
        );
    }

    // -------------------------------------------------------------------------
    // generate + parse — admin user (no linked member)
    // -------------------------------------------------------------------------

    @Test
    void generate_adminUser_producesValidToken() {
        AppUser admin = AppUser.builder()
                .id(1L)
                .email("admin@gym.com")
                .role(UserRole.ADMIN)
                .build();

        String token = jwtUtil.generate(admin);

        assertThat(token).isNotBlank();
    }

    @Test
    void parse_adminToken_returnsCorrectClaims() {
        AppUser admin = AppUser.builder()
                .id(1L)
                .email("admin@gym.com")
                .role(UserRole.ADMIN)
                .build();

        Claims claims = jwtUtil.parse(jwtUtil.generate(admin));

        assertThat(claims.getSubject()).isEqualTo("admin@gym.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("memberId")).isNull();
    }

    // -------------------------------------------------------------------------
    // generate + parse — member user (with linked member)
    // -------------------------------------------------------------------------

    @Test
    void parse_memberToken_includesMemberId() {
        com.gymrat.entity.Member member = new com.gymrat.entity.Member();
        member.setId(42L);
        member.setName("Jane Doe");

        AppUser memberUser = AppUser.builder()
                .id(2L)
                .email("jane@example.com")
                .role(UserRole.MEMBER)
                .member(member)
                .build();

        Claims claims = jwtUtil.parse(jwtUtil.generate(memberUser));

        assertThat(claims.getSubject()).isEqualTo("jane@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("MEMBER");
        assertThat(((Number) claims.get("memberId")).longValue()).isEqualTo(42L);
        assertThat(claims.get("name", String.class)).isEqualTo("Jane Doe");
    }

    // -------------------------------------------------------------------------
    // parse — tampered token is rejected
    // -------------------------------------------------------------------------

    @Test
    void parse_tamperedToken_throwsException() {
        AppUser admin = AppUser.builder()
                .id(1L)
                .email("admin@gym.com")
                .role(UserRole.ADMIN)
                .build();

        String token = jwtUtil.generate(admin);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtUtil.parse(tampered))
                .isInstanceOf(Exception.class);
    }

    // -------------------------------------------------------------------------
    // parse — token signed with a different secret is rejected
    // -------------------------------------------------------------------------

    @Test
    void parse_tokenSignedWithDifferentSecret_throwsException() {
        JwtUtil otherUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                otherUtil,
                "secret",
                "completely-different-secret-key-that-is-long-enough-for-hs256-minimum"
        );

        AppUser admin = AppUser.builder()
                .id(1L)
                .email("admin@gym.com")
                .role(UserRole.ADMIN)
                .build();

        String foreignToken = otherUtil.generate(admin);

        assertThatThrownBy(() -> jwtUtil.parse(foreignToken))
                .isInstanceOf(Exception.class);
    }
}
