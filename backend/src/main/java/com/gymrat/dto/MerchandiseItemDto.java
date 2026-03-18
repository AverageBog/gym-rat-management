package com.gymrat.dto;

import java.math.BigDecimal;

public record MerchandiseItemDto(
        Long id,
        String name,
        Integer quantity,
        BigDecimal price,
        String description
) {}
