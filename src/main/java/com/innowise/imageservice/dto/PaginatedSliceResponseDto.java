package com.innowise.imageservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
@Setter
@Builder
public class PaginatedSliceResponseDto<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private boolean hasNext;

    public static <T> PaginatedSliceResponseDto<T> of(Slice<T> slice) {
        return PaginatedSliceResponseDto.<T>builder()
                .content(slice.getContent())
                .pageNumber(slice.getNumber())
                .pageSize(slice.getSize())
                .hasNext(slice.hasNext())
                .build();
    }
}
