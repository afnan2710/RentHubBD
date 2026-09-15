package com.aoop.renthubbd.repository;

import com.aoop.renthubbd.model.ListingStatus;
import com.aoop.renthubbd.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<Property> findByStatusOrderByCreatedAtDesc(ListingStatus status);
    long countByOwnerIdAndStatus(Long ownerId, ListingStatus status);

    long countByOwnerId(Long ownerId);
    Optional<Property> findByIdAndOwnerId(Long id, Long ownerId);
    List<Property> findTop5ByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Property> findAllByOrderByCreatedAtDesc();
    List<Property> findByStatusOrderByCreatedAtAsc(ListingStatus status);
    long countByStatus(ListingStatus status);
}