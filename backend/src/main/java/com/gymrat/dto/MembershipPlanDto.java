package com.gymrat.dto;

import java.math.BigDecimal;

public record MembershipPlanDto(
        Long id,
        String name,
        Integer durationMonths,
        BigDecimal price,
        String description
) {}
