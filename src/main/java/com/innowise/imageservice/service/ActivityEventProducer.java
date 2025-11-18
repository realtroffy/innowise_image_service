package com.innowise.imageservice.service;

import com.innowise.imageservice.dto.event.CommentEvent;
import com.innowise.imageservice.dto.event.LikeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityEventProducer {

    private static final String ADD_LIKE_TOPIC = "add_like";
    private static final String REMOVE_LIKE_TOPIC = "remove_like";
    private static final String CREATE_COMMENT_TOPIC = "create_comment";
    private static final String REMOVE_COMMENT_TOPIC = "remove_comment";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendAddLikeEvent(Long userId, Long imageId) {
        LikeEvent event = LikeEvent.builder()
                .userId(userId)
                .imageId(imageId)
                .createdAt(Instant.now())
                .build();
        
        sendEvent(ADD_LIKE_TOPIC, event, userId, imageId);
    }

    public void sendRemoveLikeEvent(Long userId, Long imageId) {
        LikeEvent event = LikeEvent.builder()
                .userId(userId)
                .imageId(imageId)
                .createdAt(Instant.now())
                .build();
        
        sendEvent(REMOVE_LIKE_TOPIC, event, userId, imageId);
    }

    public void sendCreateCommentEvent(Long userId, Long imageId, Long commentId, String content) {
        CommentEvent event = CommentEvent.builder()
                .userId(userId)
                .imageId(imageId)
                .commentId(commentId)
                .content(content)
                .createdAt(Instant.now())
                .build();
        
        sendEvent(CREATE_COMMENT_TOPIC, event, userId, imageId);
    }

    public void sendRemoveCommentEvent(Long userId, Long imageId, Long commentId) {
        CommentEvent event = CommentEvent.builder()
                .userId(userId)
                .imageId(imageId)
                .commentId(commentId)
                .createdAt(Instant.now())
                .build();
        
        sendEvent(REMOVE_COMMENT_TOPIC, event, userId, imageId);
    }

    private void sendEvent(String topic, Object event, Long userId, Long imageId) {
        String key = "%d_%d".formatted(userId, imageId);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully sent event to topic {}: {}", topic, event);
            } else {
                log.error("Failed to send event to topic {}: {}", topic, event, ex);
            }
        });
    }
}

