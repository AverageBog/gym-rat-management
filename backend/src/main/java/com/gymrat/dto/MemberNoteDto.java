package com.gymrat.dto;

import java.time.LocalDateTime;

public record MemberNoteDto(
        Long id,
        Long memberId,
        String content,
        LocalDateTime createdAt
) {}
