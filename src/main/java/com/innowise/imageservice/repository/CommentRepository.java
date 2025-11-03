package com.innowise.imageservice.repository;

import com.innowise.imageservice.dto.CommentWithOwnersResponseDto;
import com.innowise.imageservice.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
        select new com.innowise.imageservice.dto.CommentWithOwnersResponseDto(
               c.id,
               c.content,
               c.createdAt,
               c.userId,
               c.image.id,
               case when c.userId = :currentUserId then true else false end,
               null)
        from Comment c
        where c.image.id = :imageId
        order by c.createdAt desc
        """)
    Slice<CommentWithOwnersResponseDto> findAllByImageIdWithOwner(
            Long imageId,
            Long currentUserId,
            Pageable pageable);
}

