package com.innowise.imageservice.repository;

import com.innowise.imageservice.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    @Query("SELECT l FROM Like l WHERE l.userId = :userId AND l.image.id = :imageId")
    Optional<Like> findByUserIdAndImageId(Long userId, Long imageId);
}
