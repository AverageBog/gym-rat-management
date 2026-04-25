package com.gymrat.dto;

public record LoginResponse(String token, String role, Long memberId, String email, String name) {}
