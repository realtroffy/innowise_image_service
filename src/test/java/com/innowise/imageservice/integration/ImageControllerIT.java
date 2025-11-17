package com.innowise.imageservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.imageservice.dto.CommentRequestDto;
import com.innowise.imageservice.dto.ImageRequestDto;
import com.innowise.imageservice.dto.ImageResponseDto;
import com.innowise.imageservice.dto.UserNamesResponseDto;
import com.innowise.imageservice.integration.config.IntegrationTestConfig;
import com.innowise.imageservice.service.AuthServiceClient;
import com.innowise.imageservice.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class ImageControllerIT extends IntegrationTestConfig {

    private static final String IMAGE_URL = "/api/images";
    private static final String USERS_URL = "/api/users/";
    private static final String IMAGES_PATH = "/images";
    private static final String LIKES_PATH = "/likes";
    private static final String COMMENTS_PATH = "/comments";
    private static final String USER_ID = "1";
    private static final String USER_NAME_STRING = "User";
    private static final String SECOND_USER_ID = "2";
    private static final String PAGE_PARAM = "page";
    private static final String SIZE_PARAM = "size";
    private static final String PAGE_VALUE = "0";
    private static final String SIZE_VALUE = "10";
    private static final String TEST_IMAGE_PATH = "images/test-image.jpg";
    private static final String TEST_IMAGE_NAME = "test-image.jpg";
    private static final String TEST_IMAGE_DESCRIPTION = "Test image description";
    private static final String LIKED_RESPONSE = "Liked";
    private static final String DISLIKED_RESPONSE = "Disliked";
    private static final String COMMENT_CONTENT = "Nice image!";
    private static final String UPDATED_COMMENT_CONTENT = "Updated comment";
    private static final int EXPECTED_IMAGE_COUNT = 2;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImageService imageService;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    private ImageRequestDto imageRequestDto;
    private MockMultipartFile imageFile;

    @BeforeEach
    void setUp() throws Exception {
        imageRequestDto = new ImageRequestDto(TEST_IMAGE_DESCRIPTION);
        ClassPathResource resource = new ClassPathResource(TEST_IMAGE_PATH);
        imageFile = new MockMultipartFile(
                "file",
                TEST_IMAGE_NAME,
                MediaType.IMAGE_JPEG_VALUE,
                Files.readAllBytes(resource.getFile().toPath())
        );
    }

    @Test
    void uploadImageShouldReturnCreated() throws Exception {
        MockMultipartFile jsonPart = new MockMultipartFile(
                "imageRequest",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(imageRequestDto)
        );

        mvc.perform(multipart(IMAGE_URL)
                        .file(imageFile)
                        .file(jsonPart)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(TEST_IMAGE_DESCRIPTION))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.url").exists());
    }

    @Test
    void getImageByIdIfExist() throws Exception {
        ImageResponseDto saved = imageService.upload(USER_ID, imageRequestDto, imageFile);

        UserNamesResponseDto mockUserNamesResponseDto = new UserNamesResponseDto(Map.of(saved.getUserId(), USER_NAME_STRING));
        when(authServiceClient.getUserNamesByIds(anyList())).thenReturn(mockUserNamesResponseDto);

        mvc.perform(get("/api/images/{id}", saved.getId())
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.description").value(TEST_IMAGE_DESCRIPTION))
                .andExpect(jsonPath("$.userName").value(USER_NAME_STRING))
                .andExpect(jsonPath("$.url").exists());
    }


    @Test
    void getAllImagesShouldReturnList() throws Exception {
        ImageResponseDto img1 = imageService.upload(USER_ID, imageRequestDto, imageFile);
        ImageResponseDto img2 = imageService.upload(SECOND_USER_ID, imageRequestDto, imageFile);

        UserNamesResponseDto namesMock = new UserNamesResponseDto(
                Map.of(img1.getUserId(), USER_NAME_STRING,
                        img2.getUserId(), USER_NAME_STRING));
        when(authServiceClient.getUserNamesByIds(anyList())).thenReturn(namesMock);

        mvc.perform(get(IMAGE_URL)
                        .param(PAGE_PARAM, PAGE_VALUE)
                        .param(SIZE_PARAM, SIZE_VALUE)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(EXPECTED_IMAGE_COUNT)))
                .andExpect(jsonPath("$.content[*].description",
                        everyItem(is(TEST_IMAGE_DESCRIPTION))));
    }

    @Test
    void getAllImagesByUserIdShouldReturnList() throws Exception {
        ImageResponseDto img1 = imageService.upload(USER_ID, imageRequestDto, imageFile);
        imageService.upload(USER_ID, imageRequestDto, imageFile);
        imageService.upload(SECOND_USER_ID, imageRequestDto, imageFile);

        UserNamesResponseDto namesMock = new UserNamesResponseDto(
                Map.of(img1.getUserId(), USER_NAME_STRING));
        when(authServiceClient.getUserNamesByIds(anyList())).thenReturn(namesMock);

        mvc.perform(get(USERS_URL + IMAGES_PATH)
                        .param(PAGE_PARAM, PAGE_VALUE)
                        .param(SIZE_PARAM, SIZE_VALUE)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(EXPECTED_IMAGE_COUNT)))
                .andExpect(jsonPath("$.content[*].description",
                        everyItem(is(TEST_IMAGE_DESCRIPTION))));
    }

    @Test
    void pressOrDeleteLikeShouldWork() throws Exception {
        ImageResponseDto saved = imageService.upload(USER_ID, imageRequestDto, imageFile);

        mvc.perform(put(IMAGE_URL + "/" + saved.getId() + LIKES_PATH)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(LIKED_RESPONSE));

        mvc.perform(put(IMAGE_URL + "/" + saved.getId() + LIKES_PATH)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(DISLIKED_RESPONSE));
    }

    @Test
    void addUpdateDeleteCommentShouldWork() throws Exception {
        ImageResponseDto saved = imageService.upload(USER_ID, imageRequestDto, imageFile);
        String postUrl = IMAGE_URL + "/" + saved.getId() + COMMENTS_PATH;

        UserNamesResponseDto namesMock = new UserNamesResponseDto(
                Map.of(Long.valueOf(USER_ID), USER_NAME_STRING));
        when(authServiceClient.getUserNamesByIds(anyList())).thenReturn(namesMock);

        CommentRequestDto commentDto = new CommentRequestDto(COMMENT_CONTENT);
        MvcResult result = mvc.perform(post(postUrl)
                        .header("X-User-Id", USER_ID)
                        .header("X-User-Name", USER_NAME_STRING)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value(COMMENT_CONTENT))
                .andReturn();

        long commentId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        String putUrl = IMAGE_URL + "/" + saved.getId() + COMMENTS_PATH + "/" + commentId;

        CommentRequestDto updatedDto = new CommentRequestDto(UPDATED_COMMENT_CONTENT);
        mvc.perform(put(putUrl)
                        .header("X-User-Id", USER_ID)
                        .header("X-User-Name", USER_NAME_STRING)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(UPDATED_COMMENT_CONTENT));

        String deleteUrl = IMAGE_URL + "/" + saved.getId() + COMMENTS_PATH + "/" + commentId;

        mvc.perform(delete(deleteUrl)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNoContent());
    }
}