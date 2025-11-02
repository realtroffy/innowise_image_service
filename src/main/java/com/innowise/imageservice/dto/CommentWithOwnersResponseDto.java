package com.innowise.imageservice.dto;

import java.time.LocalDateTime;

public record CommentWithOwnersResponseDto(
        Long id,
        String content,
        LocalDateTime createdAt,
        Long userId,
        Long imageId,
        boolean isCurrentUserOwner,
        String ownerName) {
}

