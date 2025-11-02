package com.innowise.imageservice.unit.service;

import com.innowise.imageservice.config.ImageProperties;
import com.innowise.imageservice.dto.CommentRequestDto;
import com.innowise.imageservice.dto.CommentResponseDto;
import com.innowise.imageservice.dto.ImageRequestDto;
import com.innowise.imageservice.dto.ImageResponseDto;
import com.innowise.imageservice.dto.ImageWithLikeByCurrentUserResponseDto;
import com.innowise.imageservice.dto.PaginatedSliceResponseDto;
import com.innowise.imageservice.exception.ImageFileRequiredException;
import com.innowise.imageservice.exception.ImageNotFoundException;
import com.innowise.imageservice.exception.OperationNotAllowedException;
import com.innowise.imageservice.mapper.CommentMapper;
import com.innowise.imageservice.mapper.ImageMapper;
import com.innowise.imageservice.model.Comment;
import com.innowise.imageservice.model.Image;
import com.innowise.imageservice.model.Like;
import com.innowise.imageservice.repository.CommentRepository;
import com.innowise.imageservice.repository.ImageRepository;
import com.innowise.imageservice.repository.LikeRepository;
import com.innowise.imageservice.service.S3Service;
import com.innowise.imageservice.service.impl.ImageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    private static final String IMAGE_FILE_NAME = "photo.jpg";
    private static final String IMAGE_CONTENT_TYPE = "image/jpeg";
    private static final long MIN_FILE_SIZE = 102400L; // 100 KB
    private static final long MAX_FILE_SIZE = 10485760L; // 10 MB
    private static final long VALID_FILE_SIZE = 200_000L; // 200 KB
    private static final long IMAGE_ID = 1L;
    private static final long COMMENT_ID = 1L;
    private static final long USER_ID_1 = 1L;
    private static final long USER_ID_2 = 2L;
    private static final String USER_ID_1_STRING = "1";
    private static final String USER_NAME_STRING = "User";
    private static final boolean LIKED_BY_CURRENT_USER_BOOLEAN = false;
    private static final long ZERO_LIKES = 0L;
    private static final long ONE_LIKE = 1L;
    private static final String IMAGE_DESCRIPTION = "description";
    private static final String IMAGE_URL = "url";
    private static final String SHORT_DESCRIPTION = "desc";
    private static final String COMMENT_CONTENT = "content";
    private static final String OLD_COMMENT_CONTENT = "old";
    private static final String NEW_COMMENT_CONTENT = "new";
    private static final String LIKED_RESPONSE = "Liked";
    private static final String DISLIKED_RESPONSE = "Disliked";
    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 10;
    private static final int SINGLE_ITEM_SIZE = 1;
    private static final LocalDateTime UPLOADED_AT = LocalDateTime.now();

    @Mock
    private ImageRepository imageRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ImageMapper imageMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private S3Service s3Service;

    @Mock
    private ImageProperties imageProperties;

    @InjectMocks
    private ImageServiceImpl imageService;

    @Test
    void upload_shouldUploadImageSuccessfully() {
        MultipartFile file = mock(MultipartFile.class);
        when(imageProperties.getMinBytes()).thenReturn(MIN_FILE_SIZE);
        when(imageProperties.getMaxBytes()).thenReturn(MAX_FILE_SIZE);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(VALID_FILE_SIZE);
        when(file.getOriginalFilename()).thenReturn(IMAGE_FILE_NAME);
        when(file.getContentType()).thenReturn(IMAGE_CONTENT_TYPE);

        ImageRequestDto requestDto = new ImageRequestDto(IMAGE_DESCRIPTION);

        Image savedImage = Image.builder()
                .id(IMAGE_ID)
                .url(IMAGE_URL)
                .description(IMAGE_DESCRIPTION)
                .likes(ZERO_LIKES)
                .userId(USER_ID_1)
                .uploadedAt(UPLOADED_AT)
                .build();

        ImageResponseDto expectedDto = new ImageResponseDto(IMAGE_ID, IMAGE_URL, IMAGE_DESCRIPTION, UPLOADED_AT, ZERO_LIKES, USER_ID_1);

        when(s3Service.uploadImage(any(MultipartFile.class), anyString())).thenReturn(IMAGE_URL);
        when(imageRepository.save(any(Image.class))).thenReturn(savedImage);
        when(imageMapper.toImageResponseDto(savedImage)).thenReturn(expectedDto);

        ImageResponseDto result = imageService.upload(USER_ID_1_STRING, requestDto, file);

        assertNotNull(result);
        assertEquals(IMAGE_URL, result.url());
        verify(s3Service).uploadImage(any(MultipartFile.class), anyString());
        verify(imageRepository).save(any(Image.class));
    }

    @Test
    void upload_shouldThrowExceptionIfFileEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        ImageRequestDto requestDto = new ImageRequestDto(IMAGE_DESCRIPTION);

        assertThrows(ImageFileRequiredException.class,
                () -> imageService.upload(USER_ID_1_STRING, requestDto, file));
    }

    @Test
    void getById_shouldReturnImage() {
        ImageWithLikeByCurrentUserResponseDto imageWithLikeDto = new ImageWithLikeByCurrentUserResponseDto(
                IMAGE_ID, IMAGE_URL, SHORT_DESCRIPTION, UPLOADED_AT, ZERO_LIKES, LIKED_BY_CURRENT_USER_BOOLEAN, USER_ID_1);

        when(imageRepository.findWithLikeByCurrentUserId(USER_ID_1, IMAGE_ID)).thenReturn(Optional.of(imageWithLikeDto));

        ImageWithLikeByCurrentUserResponseDto result = imageService.getById(USER_ID_1_STRING, IMAGE_ID);

        assertEquals(imageWithLikeDto, result);
    }

    @Test
    void getById_shouldThrowIfNotFound() {
        when(imageRepository.findWithLikeByCurrentUserId(USER_ID_1, IMAGE_ID)).thenReturn(Optional.empty());

        assertThrows(ImageNotFoundException.class, () -> imageService.getById(USER_ID_1_STRING, IMAGE_ID));
    }

    @Test
    void setOrRemoveLike_shouldAddLike() {
        Image image = Image.builder().id(IMAGE_ID).likes(ZERO_LIKES).userId(USER_ID_1).build();
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));
        when(likeRepository.findByUserIdAndImageId(USER_ID_1, IMAGE_ID)).thenReturn(Optional.empty());
        when(likeRepository.save(any(Like.class))).thenReturn(new Like());
        when(imageRepository.save(image)).thenReturn(image);

        String result = imageService.setOrRemoveLike(USER_ID_1_STRING, IMAGE_ID);

        assertEquals(LIKED_RESPONSE, result);
        assertEquals(ONE_LIKE, image.getLikes());
    }

    @Test
    void setOrRemoveLike_shouldRemoveLike() {
        Image image = Image.builder().id(IMAGE_ID).likes(ONE_LIKE).userId(USER_ID_1).build();
        Like like = new Like();
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));
        when(likeRepository.findByUserIdAndImageId(USER_ID_1, IMAGE_ID)).thenReturn(Optional.of(like));
        doNothing().when(likeRepository).delete(like);
        when(imageRepository.save(image)).thenReturn(image);

        String result = imageService.setOrRemoveLike(USER_ID_1_STRING, IMAGE_ID);

        assertEquals(DISLIKED_RESPONSE, result);
        assertEquals(ZERO_LIKES, image.getLikes());
    }

    @Test
    void addComment_shouldAddComment() {
        Image image = Image.builder().id(IMAGE_ID).build();
        Comment comment = Comment.builder().id(COMMENT_ID).build();
        CommentRequestDto requestDto = new CommentRequestDto(COMMENT_CONTENT);
        CommentResponseDto dto = new CommentResponseDto(COMMENT_ID, COMMENT_CONTENT, UPLOADED_AT, USER_ID_1, IMAGE_ID, USER_NAME_STRING);

        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toCommentResponseDto(comment)).thenReturn(dto);

        CommentResponseDto result = imageService.addComment(USER_ID_1_STRING, USER_NAME_STRING, IMAGE_ID, requestDto);

        assertEquals(dto, result);
    }

    @Test
    void deleteComment_shouldDelete() {
        Image image = Image.builder().id(IMAGE_ID).userId(USER_ID_1).build();
        Comment comment = Comment.builder().id(COMMENT_ID).userId(USER_ID_1).build();

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));
        doNothing().when(commentRepository).delete(comment);

        assertDoesNotThrow(() -> imageService.deleteComment(USER_ID_1_STRING, IMAGE_ID, COMMENT_ID));
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_shouldThrowIfNotAllowed() {
        Image image = Image.builder().id(IMAGE_ID).userId(USER_ID_2).build();
        Comment comment = Comment.builder().id(COMMENT_ID).userId(USER_ID_1).build();

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));

        assertThrows(OperationNotAllowedException.class, () -> imageService.deleteComment(USER_ID_1_STRING, IMAGE_ID, COMMENT_ID));
    }

    @Test
    void updateComment_shouldUpdate() {
        Image image = Image.builder().id(IMAGE_ID).build();
        Comment comment = Comment.builder().id(COMMENT_ID).userId(USER_ID_1).content(OLD_COMMENT_CONTENT).build();
        CommentRequestDto requestDto = new CommentRequestDto(NEW_COMMENT_CONTENT);
        CommentResponseDto dto = new CommentResponseDto(COMMENT_ID, NEW_COMMENT_CONTENT, UPLOADED_AT, USER_ID_1, IMAGE_ID, USER_NAME_STRING);

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toCommentResponseDto(comment)).thenReturn(dto);

        CommentResponseDto result = imageService.updateComment(USER_ID_1_STRING, IMAGE_ID, COMMENT_ID, requestDto);

        assertEquals(dto, result);
        assertEquals(NEW_COMMENT_CONTENT, comment.getContent());
    }

    @Test
    void updateComment_shouldThrowIfNotAllowed() {
        Comment comment = Comment.builder().id(COMMENT_ID).userId(USER_ID_2).content(OLD_COMMENT_CONTENT).build();
        Image image = Image.builder().id(IMAGE_ID).build();
        CommentRequestDto requestDto = new CommentRequestDto(NEW_COMMENT_CONTENT);

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(imageRepository.findById(IMAGE_ID)).thenReturn(Optional.of(image));

        assertThrows(OperationNotAllowedException.class,
                () -> imageService.updateComment(USER_ID_1_STRING, IMAGE_ID, COMMENT_ID, requestDto));
    }

    @Test
    void getAllByUserId_shouldReturnSlice() {
        Image image = Image.builder().id(IMAGE_ID).build();
        ImageResponseDto dto = new ImageResponseDto(IMAGE_ID, IMAGE_URL, SHORT_DESCRIPTION, UPLOADED_AT, ZERO_LIKES, USER_ID_1);

        Slice<Image> slice = new SliceImpl<>(List.of(image));
        when(imageRepository.findByUserId(USER_ID_1, PageRequest.of(PAGE_NUMBER, PAGE_SIZE))).thenReturn(slice);
        when(imageMapper.toImageResponseDto(image)).thenReturn(dto);

        PaginatedSliceResponseDto<ImageResponseDto> result = imageService.getAllByUserId(String.valueOf(USER_ID_1), PAGE_NUMBER, PAGE_SIZE);

        assertEquals(SINGLE_ITEM_SIZE, result.getContent().size());
    }

    @Test
    void getAll_shouldReturnSlice() {
        Image image = Image.builder().id(IMAGE_ID).build();
        ImageResponseDto dto = new ImageResponseDto(IMAGE_ID, IMAGE_URL, SHORT_DESCRIPTION, UPLOADED_AT, ZERO_LIKES, USER_ID_1);

        Slice<Image> slice = new SliceImpl<>(List.of(image));
        when(imageRepository.findSlicedAll(PageRequest.of(PAGE_NUMBER, PAGE_SIZE))).thenReturn(slice);
        when(imageMapper.toImageResponseDto(image)).thenReturn(dto);

        PaginatedSliceResponseDto<ImageResponseDto> result = imageService.getAll(PAGE_NUMBER, PAGE_SIZE);

        assertEquals(SINGLE_ITEM_SIZE, result.getContent().size());
    }
}