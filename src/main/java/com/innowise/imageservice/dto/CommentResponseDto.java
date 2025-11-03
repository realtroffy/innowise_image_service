package com.innowise.imageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommentResponseDto {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long userId;
    private Long imageId;
    private String userName;
}
