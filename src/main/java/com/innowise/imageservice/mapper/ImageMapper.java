package com.innowise.imageservice.mapper;

import com.innowise.imageservice.dto.ImageResponseDto;
import com.innowise.imageservice.model.Image;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    ImageResponseDto toImageResponseDto(Image image);
}
