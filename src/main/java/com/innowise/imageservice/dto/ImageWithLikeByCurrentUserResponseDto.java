package com.innowise.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ImageWithLikeByCurrentUserResponseDto {

    private Long id;
    private String url;
    private String description;
    private LocalDateTime updatedAt;
    private Long likes;
    private boolean likedByCurrentUser;
    private String userName;
    private Long userId;
}
