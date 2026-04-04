package com.gymrat.security;

/**
 * Lightweight principal stored in the SecurityContext — built from JWT claims,
 * no additional DB round-trip needed on every request.
 */
public record AuthenticatedUser(String email, String role, Long memberId) {}
