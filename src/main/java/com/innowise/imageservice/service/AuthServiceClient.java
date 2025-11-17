package com.innowise.imageservice.service;

import com.innowise.imageservice.dto.UserNamesRequestDto;
import com.innowise.imageservice.dto.UserNamesResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${auth.service.secret}")
    private String serviceSecret;

    public UserNamesResponseDto getUserNamesByIds(List<Long> userIds) {
        UserNamesRequestDto requestDto = new UserNamesRequestDto(userIds);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Secret", serviceSecret);
        HttpEntity<UserNamesRequestDto> requestEntity = new HttpEntity<>(requestDto, headers);
        ResponseEntity<UserNamesResponseDto> response = restTemplate.exchange(
                authServiceUrl,
                HttpMethod.POST,
                requestEntity,
                UserNamesResponseDto.class
        );

        return response.getBody();
    }
}
