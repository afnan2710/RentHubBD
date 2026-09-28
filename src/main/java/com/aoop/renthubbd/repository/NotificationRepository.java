package com.aoop.renthubbd.repository;

import com.aoop.renthubbd.model.Notification;
import com.aoop.renthubbd.model.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
            NotificationRecipient type, Long recipientId);

    List<Notification> findTop5ByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
            NotificationRecipient type, Long recipientId);

    long countByRecipientTypeAndRecipientIdAndReadFalse(
            NotificationRecipient type, Long recipientId);

    Optional<Notification> findByIdAndRecipientTypeAndRecipientId(
            Long id, NotificationRecipient type, Long recipientId);
}