package com.aoop.renthubbd.repository;

import com.aoop.renthubbd.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);
    List<Message> findByConversationIdAndIdGreaterThanOrderBySentAtAsc(Long conversationId, Long afterId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(Long conversationId, Long senderId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.readAt IS NULL "
            + "AND m.sender.id <> :userId "
            + "AND m.conversation.id IN "
            + "(SELECT c.id FROM Conversation c WHERE c.renter.id = :userId OR c.owner.id = :userId)")
    long countAllUnreadForUser(@Param("userId") Long userId);
}