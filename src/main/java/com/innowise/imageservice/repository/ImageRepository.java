package com.innowise.imageservice.repository;

import com.innowise.imageservice.model.Image;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ImageRepository extends JpaRepository<Image, Long> {

    Slice<Image> findByUserId(Long userId, Pageable pageable);

    @Query("FROM Image i")
    Slice<Image> findSlicedAll(Pageable pageable);
}
