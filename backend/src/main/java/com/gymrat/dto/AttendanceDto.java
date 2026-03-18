package com.gymrat.dto;

import java.time.LocalDateTime;

public record AttendanceDto(
        Long id,
        Long memberId,
        String memberName,
        LocalDateTime checkInTime
) {}
