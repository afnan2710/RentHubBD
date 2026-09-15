package com.aoop.renthubbd.service;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.Notification;
import com.aoop.renthubbd.model.NotificationRecipient;
import com.aoop.renthubbd.model.NotificationType;
import com.aoop.renthubbd.repository.AdminRepository;
import com.aoop.renthubbd.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AdminRepository adminRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               AdminRepository adminRepository) {
        this.notificationRepository = notificationRepository;
        this.adminRepository = adminRepository;
    }

    public void notifyUser(Long userId, NotificationType type,
                           String title, String message, Long relatedListingId) {
        Notification n = new Notification();
        n.setRecipientType(NotificationRecipient.USER);
        n.setRecipientId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setRelatedListingId(relatedListingId);
        notificationRepository.save(n);
    }

    public void notifyAdmin(Long adminId, NotificationType type,
                            String title, String message, Long relatedListingId) {
        Notification n = new Notification();
        n.setRecipientType(NotificationRecipient.ADMIN);
        n.setRecipientId(adminId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setRelatedListingId(relatedListingId);
        notificationRepository.save(n);
    }

    public void notifyAllAdmins(NotificationType type,
                                String title, String message, Long relatedListingId) {
        List<Admin> admins = adminRepository.findAllByOrderByCreatedAtDesc();
        for (Admin a : admins) {
            if (a.getStatus() != AccountStatus.ACTIVE) continue;
            notifyAdmin(a.getId(), type, title, message, relatedListingId);
        }
    }

    public List<Notification> listForUser(Long userId) {
        return notificationRepository
                .findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(NotificationRecipient.USER, userId);
    }

    public List<Notification> listForAdmin(Long adminId) {
        return notificationRepository
                .findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(NotificationRecipient.ADMIN, adminId);
    }

    public List<Notification> recentForUser(Long userId) {
        return notificationRepository
                .findTop5ByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(NotificationRecipient.USER, userId);
    }

    public long countUnreadForUser(Long userId) {
        return notificationRepository
                .countByRecipientTypeAndRecipientIdAndReadFalse(NotificationRecipient.USER, userId);
    }

    public long countUnreadForAdmin(Long adminId) {
        return notificationRepository
                .countByRecipientTypeAndRecipientIdAndReadFalse(NotificationRecipient.ADMIN, adminId);
    }

    public void markRead(Long id, Long recipientId, NotificationRecipient type) {
        notificationRepository.findByIdAndRecipientTypeAndRecipientId(id, type, recipientId)
                .ifPresent(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    public void markAllReadForUser(Long userId) {
        for (Notification n : listForUser(userId)) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }

    public void markAllReadForAdmin(Long adminId) {
        for (Notification n : listForAdmin(adminId)) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }
}