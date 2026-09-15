package com.aoop.renthubbd.service;

import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AuditAction;
import com.aoop.renthubbd.model.ListingStatus;
import com.aoop.renthubbd.model.Property;
import com.aoop.renthubbd.model.NotificationType;
import com.aoop.renthubbd.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminListingService {

    private final PropertyRepository propertyRepository;
    private final AuditLogService auditLogService;
    private final NotificationEmailService emailService;
    private final NotificationService notificationService;

    public AdminListingService(PropertyRepository propertyRepository,
                               AuditLogService auditLogService,
                               NotificationEmailService emailService,
                               NotificationService notificationService) {
        this.propertyRepository = propertyRepository;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    public List<Property> getPending() {
        return propertyRepository.findByStatusOrderByCreatedAtAsc(ListingStatus.PENDING);
    }

    public List<Property> getAll() {
        return propertyRepository.findAllByOrderByCreatedAtDesc();
    }


    public Property getById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found."));
    }

    public void approve(Long listingId, Admin actor) {
        Property p = propertyRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found."));
        p.setStatus(ListingStatus.PUBLISHED);
        p.setRejectionReason(null);
        propertyRepository.save(p);

        auditLogService.record(actor.getUsername(), AuditAction.LISTING_APPROVED, p.getTitle());

        if (p.getOwner() != null) {
            notificationService.notifyUser(
                    p.getOwner().getId(),
                    NotificationType.LISTING_APPROVED,
                    "Listing approved",
                    "Your listing \"" + p.getTitle() + "\" has been approved and is now published.",
                    p.getId());
            emailService.sendListingApprovedEmail(
                    p.getOwner().getEmail(),
                    p.getOwner().getFirstName(),
                    p.getTitle());
        }
    }

    public void reject(Long listingId, String reason, Admin actor) {
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("A rejection reason is required.");
        Property p = propertyRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found."));
        p.setStatus(ListingStatus.REJECTED);
        p.setRejectionReason(reason.trim());
        propertyRepository.save(p);

        auditLogService.record(actor.getUsername(), AuditAction.LISTING_REJECTED,
                p.getTitle() + " :: " + reason);

        if (p.getOwner() != null) {
            notificationService.notifyUser(
                    p.getOwner().getId(),
                    NotificationType.LISTING_REJECTED,
                    "Listing rejected",
                    "Your listing \"" + p.getTitle() + "\" was rejected. Reason: " + reason,
                    p.getId());
            emailService.sendListingRejectedEmail(
                    p.getOwner().getEmail(),
                    p.getOwner().getFirstName(),
                    p.getTitle(),
                    reason);
        }
    }


    public void archive(Long listingId, Admin actor) {
        Property p = propertyRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found."));
        if (p.getStatus() == ListingStatus.ARCHIVED)
            throw new IllegalArgumentException("This listing is already archived.");

        p.setStatus(ListingStatus.ARCHIVED);
        propertyRepository.save(p);

        auditLogService.record(actor.getUsername(), AuditAction.LISTING_ARCHIVED, p.getTitle());

        if (p.getOwner() != null) {
            notificationService.notifyUser(
                    p.getOwner().getId(),
                    NotificationType.LISTING_ARCHIVED,
                    "Listing archived",
                    "Your listing \"" + p.getTitle()
                            + "\" was archived by an admin and is no longer visible on the public site.",
                    p.getId());
        }
    }

    public void unarchive(Long listingId, Admin actor) {
        Property p = propertyRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found."));
        if (p.getStatus() != ListingStatus.ARCHIVED)
            throw new IllegalArgumentException("This listing is not archived.");

        p.setStatus(ListingStatus.PUBLISHED);
        propertyRepository.save(p);

        auditLogService.record(actor.getUsername(), AuditAction.LISTING_UNARCHIVED, p.getTitle());

        if (p.getOwner() != null) {
            notificationService.notifyUser(
                    p.getOwner().getId(),
                    NotificationType.LISTING_UNARCHIVED,
                    "Listing restored",
                    "Your listing \"" + p.getTitle() + "\" has been restored and is publicly visible again.",
                    p.getId());
        }
    }
}