package com.innowise.imageservice.service.impl;

import com.innowise.imageservice.config.ImageProperties;
import com.innowise.imageservice.dto.CommentRequestDto;
import com.innowise.imageservice.dto.CommentResponseDto;
import com.innowise.imageservice.dto.CommentWithOwnersResponseDto;
import com.innowise.imageservice.dto.ImageRequestDto;
import com.innowise.imageservice.dto.ImageResponseDto;
import com.innowise.imageservice.dto.ImageWithLikeByCurrentUserResponseDto;
import com.innowise.imageservice.dto.PaginatedSliceResponseDto;
import com.innowise.imageservice.dto.UserNamesResponseDto;
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
import com.innowise.imageservice.service.AuthServiceClient;
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
import java.util.List;
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
    private final AuthServiceClient authServiceClient;

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
    public ImageWithLikeByCurrentUserResponseDto getById(String currentUserId, Long imageId) {
        ImageWithLikeByCurrentUserResponseDto imageWithLikeByCurrentUserResponseDto = imageRepository
                .findWithLikeByCurrentUserId(Long.valueOf(currentUserId), imageId).orElseThrow(() ->
                new ImageNotFoundException(IMAGE_NOT_FOUND_EXCEPTION_MESSAGE));

        Long userId = imageWithLikeByCurrentUserResponseDto.getUserId();
        UserNamesResponseDto userNamesByIds = authServiceClient.getUserNamesByIds(List.of(userId));
        String userName = userNamesByIds.names().get(userId);
        imageWithLikeByCurrentUserResponseDto.setUserName(userName);
        return imageWithLikeByCurrentUserResponseDto;
    }

    private Image findById(Long imageId) {
        return imageRepository.findById(imageId).orElseThrow(() ->
                new ImageNotFoundException(IMAGE_NOT_FOUND_EXCEPTION_MESSAGE));
    }

    @Override
    public PaginatedSliceResponseDto<ImageWithLikeByCurrentUserResponseDto> getAllByUserId(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Slice<ImageWithLikeByCurrentUserResponseDto> imageInfo = imageRepository
                .findAllByOwnerIdWithLikeFlag(Long.valueOf(userId), pageable);


        List<ImageWithLikeByCurrentUserResponseDto> updatedImages = updateUserNames(imageInfo.getContent());

        return PaginatedSliceResponseDto.<ImageWithLikeByCurrentUserResponseDto>builder()
                .content(updatedImages)
                .pageNumber(imageInfo.getNumber())
                .pageSize(imageInfo.getSize())
                .hasNext(imageInfo.hasNext())
                .build();
    }

    @Override
    public PaginatedSliceResponseDto<ImageWithLikeByCurrentUserResponseDto> getAll(String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Slice<ImageWithLikeByCurrentUserResponseDto> imageInfo = imageRepository
                .findAllWithLikeFlag(Long.valueOf(currentUserId), pageable);


        List<ImageWithLikeByCurrentUserResponseDto> updatedImages = updateUserNames(imageInfo.getContent());

        return PaginatedSliceResponseDto.<ImageWithLikeByCurrentUserResponseDto>builder()
                .content(updatedImages)
                .pageNumber(imageInfo.getNumber())
                .pageSize(imageInfo.getSize())
                .hasNext(imageInfo.hasNext())
                .build();
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
        UserNamesResponseDto userNamesByIds = authServiceClient.getUserNamesByIds(List.of(Long.valueOf(userId)));
        String userName = userNamesByIds.names().get(Long.valueOf(userId));

        Comment comment = Comment.builder()
                .content(commentRequestDto.content())
                .createdAt(LocalDateTime.now())
                .image(image)
                .userId(Long.valueOf(userId))
                .build();
        Comment savedComment = commentRepository.save(comment);
        CommentResponseDto commentResponseDto = commentMapper.toCommentResponseDto(savedComment);
        commentResponseDto.setUserName(userName);
        return commentResponseDto;
    }

    @Override
    @Transactional
    public void deleteComment(String userId, Long imageId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(COMMENT_NOT_FOUND_EXCEPTION_MESSAGE));
        Image image = findById(imageId);

        boolean isCommentOwner = comment.getUserId().equals(Long.valueOf(userId));
        boolean isImageOwner = image.getUserId().equals(Long.valueOf(userId));

        if (!isCommentOwner && !isImageOwner) {
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
    public PaginatedSliceResponseDto<CommentWithOwnersResponseDto> getAllCommentsByImageId(
            Long imageId, String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<CommentWithOwnersResponseDto> slice =
                commentRepository.findAllByImageIdWithOwner(imageId, Long.valueOf(currentUserId), pageable);
        PaginatedSliceResponseDto<CommentWithOwnersResponseDto> commentWithOwners = PaginatedSliceResponseDto.of(slice);

        List<CommentWithOwnersResponseDto> updatedComments = updateOwnerNames(commentWithOwners.getContent());

        return PaginatedSliceResponseDto.<CommentWithOwnersResponseDto>builder()
                .content(updatedComments)
                .pageNumber(commentWithOwners.getPageNumber())
                .pageSize(commentWithOwners.getPageSize())
                .hasNext(commentWithOwners.isHasNext())
                .build();
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

    private List<ImageWithLikeByCurrentUserResponseDto> updateUserNames(List<ImageWithLikeByCurrentUserResponseDto> images) {
        if (images == null || images.isEmpty()) {
            return images;
        }

        List<Long> userIds = images.stream()
                .map(ImageWithLikeByCurrentUserResponseDto::getUserId)
                .distinct()
                .toList();

        UserNamesResponseDto userNamesByIds = authServiceClient.getUserNamesByIds(userIds);

        return images.stream()
                .peek(image -> {
                    String userName = userNamesByIds.names().get(image.getUserId());
                    image.setUserName(userName);
                })
                .toList();
    }

    private List<CommentWithOwnersResponseDto> updateOwnerNames(List<CommentWithOwnersResponseDto> comments) {
        if (comments == null || comments.isEmpty()) {
            return comments;
        }

        List<Long> userIds = comments.stream()
                .map(CommentWithOwnersResponseDto::getUserId)
                .distinct()
                .toList();


        UserNamesResponseDto userNamesByIds = authServiceClient.getUserNamesByIds(userIds);

        return comments.stream()
                .peek(comment -> {
                    String userName = userNamesByIds.names().get(comment.getUserId());
                    comment.setOwnerName(userName);
                })
                .toList();
    }
}
