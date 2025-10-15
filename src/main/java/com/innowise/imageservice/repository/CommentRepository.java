package com.innowise.imageservice.repository;

import com.innowise.imageservice.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Slice<Comment> findAllByImageId(Long imageId, Pageable pageable);
}
