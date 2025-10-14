package com.innowise.imageservice.controller;

import com.innowise.imageservice.dto.CommentRequestDto;
import com.innowise.imageservice.dto.CommentResponseDto;
import com.innowise.imageservice.dto.ImageRequestDto;
import com.innowise.imageservice.dto.ImageResponseDto;
import com.innowise.imageservice.dto.PaginatedSliceResponseDto;
import com.innowise.imageservice.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ImageController {

    private final ImageService imageService;

    @PostMapping(path = "/images", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponseDto> upload(
            @RequestHeader("X-User-Id") String userId,
            @RequestPart("imageRequest") ImageRequestDto imageRequestDto,
            @RequestPart("file") MultipartFile imageFile) {
        return new ResponseEntity<>(imageService.upload(userId, imageRequestDto, imageFile),
                HttpStatus.CREATED);
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<ImageResponseDto> getById(@PathVariable("id") Long imageId) {
        return ResponseEntity.ok(imageService.getById(imageId));
    }

    @GetMapping("/users/images")
    public ResponseEntity<PaginatedSliceResponseDto<ImageResponseDto>> getAllUByUserId(@RequestHeader("X-User-Id") String currentUserId,
                                                                                       @RequestParam(defaultValue = "0") int page,
                                                                                       @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(imageService.getAllByUserId(currentUserId, page, size));
    }

    @GetMapping("/images")
    public ResponseEntity<PaginatedSliceResponseDto<ImageResponseDto>> getAllBy(@RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(imageService.getAll(page, size));
    }

    @PutMapping("/images/{id}/likes")
    public ResponseEntity<String> pressOrDeleteLike(@RequestHeader("X-User-Id") String userId,
                                                    @PathVariable("id") Long imageId) {
        return ResponseEntity.ok(imageService.setOrRemoveLike(userId, imageId));
    }

    @PostMapping("/images/{id}/comments")
    public ResponseEntity<CommentResponseDto> addComment(@RequestHeader("X-User-Id") String userId,
                                                         @PathVariable("id") Long imageId,
                                                         @Valid @RequestBody CommentRequestDto commentRequestDto) {
        return new ResponseEntity<>(imageService.addComment(userId, imageId, commentRequestDto), HttpStatus.CREATED);
    }

    @DeleteMapping("/images/{id}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@RequestHeader("X-User-Id") String userId,
                                           @PathVariable("id") Long imageId,
                                           @PathVariable("commentId") Long commentId) {
        imageService.deleteComment(userId, imageId, commentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/images/{id}/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(@RequestHeader("X-User-Id") String userId,
                                                            @PathVariable("id") Long imageId,
                                                            @PathVariable("commentId") Long commentId,
                                                            @Valid @RequestBody CommentRequestDto commentRequestDto) {
        return ResponseEntity.ok(imageService.updateComment(userId, imageId, commentId, commentRequestDto));
    }
}
