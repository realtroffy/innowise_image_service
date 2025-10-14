package com.innowise.imageservice.dto;

import java.time.LocalDateTime;

public record CommentResponseDto(
        Long id,
        String content,
        LocalDateTime createdAt,
        Long userId,
        Long imageId) {
}
