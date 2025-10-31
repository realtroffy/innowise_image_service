package com.innowise.imageservice.repository;

import com.innowise.imageservice.dto.ImageWithLikeByCurrentUserResponseDto;
import com.innowise.imageservice.model.Image;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    Slice<Image> findByUserId(Long userId, Pageable pageable);

    @Query("FROM Image i")
    Slice<Image> findSlicedAll(Pageable pageable);

    @Query("""
            select new com.innowise.imageservice.dto.ImageWithLikeByCurrentUserResponseDto(
                   i.id,
                   i.url,
                   i.description,
                   i.uploadedAt,
                   i.likes,
                   case when l.id is not null then true else false end,
                   i.userId)
            from Image i
            left join Like l on l.image = i and l.userId = :currentUserId
            where i.id = :imageId
            """)
    Optional<ImageWithLikeByCurrentUserResponseDto> findWithLikeByCurrentUserId(
            Long currentUserId,
            Long imageId);
}
