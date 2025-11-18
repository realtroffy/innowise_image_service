package com.innowise.imageservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEvent {
    private Long userId;
    private Long imageId;
    private Long commentId;
    private String content;
    private Instant createdAt;
}

