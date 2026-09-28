package com.aoop.renthubbd.repository;

import com.aoop.renthubbd.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPropertyIdOrderByCreatedAtDesc(Long propertyId);
    List<Review> findByPropertyOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Review> findByPropertyIdAndReviewerId(Long propertyId, Long reviewerId);
    boolean existsByPropertyIdAndReviewerId(Long propertyId, Long reviewerId);

    long countByPropertyId(Long propertyId);
}