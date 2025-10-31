package com.innowise.imageservice.dto;

import java.time.LocalDateTime;

public record ImageWithLikeByCurrentUserResponseDto(Long id,
                                                    String url,
                                                    String description,
                                                    LocalDateTime uploadedAt,
                                                    Long likes,
                                                    boolean likedByCurrentUser,
                                                    Long userId) {
}
