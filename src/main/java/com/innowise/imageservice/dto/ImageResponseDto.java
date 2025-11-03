package com.innowise.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ImageResponseDto {

    private Long id;
    private String url;
    private String description;
    private LocalDateTime uploadedAt;
    private Long likes;
    private Long userId;
    private String userName;
}
