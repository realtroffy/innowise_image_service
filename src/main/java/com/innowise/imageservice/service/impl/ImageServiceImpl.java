package com.innowise.imageservice.service.impl;

import com.innowise.imageservice.config.ImageProperties;
import com.innowise.imageservice.dto.CommentRequestDto;
import com.innowise.imageservice.dto.CommentResponseDto;
import com.innowise.imageservice.dto.ImageRequestDto;
import com.innowise.imageservice.dto.ImageResponseDto;
import com.innowise.imageservice.dto.PaginatedSliceResponseDto;
import com.innowise.imageservice.exception.CommentNotFoundException;
import com.innowise.imageservice.exception.ImageFileRequiredException;
import com.innowise.imageservice.exception.ImageNotFoundException;
import com.innowise.imageservice.exception.InvalidImageSizeException;
import com.innowise.imageservice.exception.InvalidImageTypeException;
import com.innowise.imageservice.exception.OperationNotAllowedException;
import com.innowise.imageservice.mapper.CommentMapper;
import com.innowise.imageservice.mapper.ImageMapper;
import com.innowise.imageservice.model.Comment;
import com.innowise.imageservice.model.Image;
import com.innowise.imageservice.model.Like;
import com.innowise.imageservice.repository.CommentRepository;
import com.innowise.imageservice.repository.ImageRepository;
import com.innowise.imageservice.repository.LikeRepository;
import com.innowise.imageservice.service.ImageService;
import com.innowise.imageservice.service.S3Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    public static final String IMAGE_FILE_REQUIRED_EXCEPTION_MESSAGE = "Image file is required";
    public static final String IMAGE_SIZE_TOO_SMALL_EXCEPTION_MESSAGE = "Image size is too small. Minimum size: %d bytes";
    public static final String IMAGE_SIZE_TOO_LARGE_EXCEPTION_MESSAGE = "Image size is too large. Maximum size: %d bytes";
    public static final String INVALID_IMAGE_TYPE_EXCEPTION_MESSAGE = "Unsupported image type: %s. Allowed types: %s";
    public static final String IMAGE_NOT_FOUND_EXCEPTION_MESSAGE = "Image not found";
    public static final String COMMENT_NOT_FOUND_EXCEPTION_MESSAGE = "Comment not found";
    public static final String OPERATION_DELETE_NOT_ALLOWED_EXCEPTION_MESSAGE =
            "You cannot delete a comment that is not yours or from an image that is not yours";
    public static final String OPERATION_UPDATE_NOT_ALLOWED_EXCEPTION_MESSAGE =
            "You cannot update a comment that is not yours";

    private final ImageRepository imageRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ImageProperties imageProperties;
    private final S3Service s3Service;
    private final ImageMapper imageMapper;
    private final CommentMapper commentMapper;

    @Override
    public ImageResponseDto upload(String userId, ImageRequestDto imageRequestDto, MultipartFile imageFile) {
        validateImageFile(imageFile);

        String generatedUniqueFilename = generateUniqueFilename(userId, imageFile.getOriginalFilename());
        String imageUrl = s3Service.uploadImage(imageFile, generatedUniqueFilename);

        try {
            Image image = Image.builder()
                    .description(imageRequestDto.description())
                    .url(imageUrl)
                    .uploadedAt(LocalDateTime.now())
                    .likes(0L)
                    .userId(Long.valueOf(userId))
                    .build();

            return imageMapper.toImageResponseDto(imageRepository.save(image));

        } catch (Exception e) {
            s3Service.deleteFile(generatedUniqueFilename);
            throw e;
        }
    }

    @Override
    public ImageResponseDto getById(Long imageId) {
        return imageMapper.toImageResponseDto(findById(imageId));
    }

    private Image findById(Long imageId) {
        return imageRepository.findById(imageId).orElseThrow(() ->
                new ImageNotFoundException(IMAGE_NOT_FOUND_EXCEPTION_MESSAGE));
    }

    @Override
    public PaginatedSliceResponseDto<ImageResponseDto> getAllByUserId(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<ImageResponseDto> imageInfo = imageRepository.findByUserId(Long.valueOf(userId), pageable)
                .map(imageMapper::toImageResponseDto);
        return PaginatedSliceResponseDto.of(imageInfo);
    }

    @Override
    public PaginatedSliceResponseDto<ImageResponseDto> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<ImageResponseDto> imageInfo = imageRepository.findSlicedAll(pageable)
                .map(imageMapper::toImageResponseDto);
        return PaginatedSliceResponseDto.of(imageInfo);
    }

    @Override
    @Transactional
    public String setOrRemoveLike(String userId, Long imageId) {
        Image image = findById(imageId);
        Like like = likeRepository.findByUserIdAndImageId(Long.valueOf(userId), imageId).orElse(null);
        if (like == null) {
            Like newLike = Like.builder()
                    .image(image)
                    .userId(Long.valueOf(userId))
                    .createdAt(LocalDateTime.now())
                    .build();
            likeRepository.save(newLike);
            image.setLikes(image.getLikes() + 1L);
            imageRepository.save(image);
            return "Liked";
        } else {
            likeRepository.delete(like);
            image.setLikes(image.getLikes() - 1L);
            imageRepository.save(image);
            return "Disliked";
        }
    }

    @Override
    public CommentResponseDto addComment(String userId, Long imageId, CommentRequestDto commentRequestDto) {
        Image image = findById(imageId);
        Comment comment = Comment.builder()
                .content(commentRequestDto.content())
                .createdAt(LocalDateTime.now())
                .image(image)
                .userId(Long.valueOf(userId))
                .build();
        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toCommentResponseDto(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(String userId, Long imageId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(COMMENT_NOT_FOUND_EXCEPTION_MESSAGE));
        Image image = findById(imageId);

        if (!comment.getUserId().equals(Long.valueOf(userId))
                || !image.getUserId().equals(Long.valueOf(userId))) {
            throw new OperationNotAllowedException(OPERATION_DELETE_NOT_ALLOWED_EXCEPTION_MESSAGE);
        }

        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(String userId, Long imageId, Long commentId, CommentRequestDto commentRequestDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(COMMENT_NOT_FOUND_EXCEPTION_MESSAGE));
        findById(imageId);
        if (!comment.getUserId().equals(Long.valueOf(userId))) {
            throw new OperationNotAllowedException(OPERATION_UPDATE_NOT_ALLOWED_EXCEPTION_MESSAGE);
        }
        comment.setContent(commentRequestDto.content());
        return commentMapper.toCommentResponseDto(commentRepository.save(comment));
    }

    @Override
    public PaginatedSliceResponseDto<CommentResponseDto> getAllCommentsByImageId(Long imageId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<CommentResponseDto> commentInfo = commentRepository.findAllByImageId(imageId, pageable)
                .map(commentMapper::toCommentResponseDto);
        return PaginatedSliceResponseDto.of(commentInfo);
    }

    private String generateUniqueFilename(String userId, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return userId + "/" + UUID.randomUUID() + extension;
    }

    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new ImageFileRequiredException(IMAGE_FILE_REQUIRED_EXCEPTION_MESSAGE);
        }
        validateImageSize(imageFile);
        validateImageContentType(imageFile);
    }

    private void validateImageSize(MultipartFile imageFile) {
        long fileSize = imageFile.getSize();
        long minSize = imageProperties.getMinBytes();
        long maxSize = imageProperties.getMaxBytes();

        if (fileSize < minSize) {
            throw new InvalidImageSizeException(
                    String.format(IMAGE_SIZE_TOO_SMALL_EXCEPTION_MESSAGE, minSize)
            );
        }

        if (fileSize > maxSize) {
            throw new InvalidImageSizeException(
                    String.format(IMAGE_SIZE_TOO_LARGE_EXCEPTION_MESSAGE, maxSize)
            );
        }
    }

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/bmp",
            "image/webp"
    );

    private void validateImageContentType(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidImageTypeException(
                    String.format(INVALID_IMAGE_TYPE_EXCEPTION_MESSAGE,
                            contentType, ALLOWED_IMAGE_TYPES)
            );
        }
    }

}
