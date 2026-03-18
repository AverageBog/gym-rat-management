package com.gymrat.dto;

import java.time.LocalDate;

public record MemberDto(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate joinDate,
        String status,
        Long membershipPlanId,
        String membershipPlanName
) {}
