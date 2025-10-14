package com.innowise.imageservice.dto;

import java.time.LocalDateTime;

public record ImageResponseDto(
        Long id,
        String url,
        String description,
        LocalDateTime uploadedAt,
        Long likes,
        Long userId) {
}
