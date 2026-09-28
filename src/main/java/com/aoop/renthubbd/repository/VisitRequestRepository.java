package com.aoop.renthubbd.repository;

import com.aoop.renthubbd.model.VisitRequest;
import com.aoop.renthubbd.model.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitRequestRepository extends JpaRepository<VisitRequest, Long> {

    List<VisitRequest> findByRenterIdOrderByCreatedAtDesc(Long renterId);
    List<VisitRequest> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<VisitRequest> findByOwnerIdAndStatusOrderByCreatedAtDesc(Long ownerId, VisitStatus status);

    Optional<VisitRequest> findByIdAndRenterId(Long id, Long renterId);
    Optional<VisitRequest> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerIdAndStatus(Long ownerId, VisitStatus status);
    long countByRenterIdAndStatus(Long renterId, VisitStatus status);
}