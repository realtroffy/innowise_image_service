package com.innowise.imageservice.dto;

import java.util.Map;

public record UserNamesResponseDto(Map<Long, String> names) {
}
