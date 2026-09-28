package com.aoop.renthubbd.repository;

import com.aoop.renthubbd.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByPropertyIdAndRenterId(Long propertyId, Long renterId);

    List<Conversation> findByRenterIdOrderByLastMessageAtDesc(Long renterId);
    List<Conversation> findByOwnerIdOrderByLastMessageAtDesc(Long ownerId);

    Optional<Conversation> findByIdAndRenterId(Long id, Long renterId);
    Optional<Conversation> findByIdAndOwnerId(Long id, Long ownerId);
}