package com.innowise.imageservice.mapper;

import com.innowise.imageservice.dto.CommentResponseDto;
import com.innowise.imageservice.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "imageId", expression = "java(comment.getImage().getId())")
    CommentResponseDto toCommentResponseDto(Comment comment);
}
