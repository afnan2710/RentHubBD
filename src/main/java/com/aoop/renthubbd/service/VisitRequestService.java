package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.VisitRequestForm;
import com.aoop.renthubbd.model.*;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.repository.UserRepository;
import com.aoop.renthubbd.repository.VisitRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class VisitRequestService {

    private final VisitRequestRepository visitRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public VisitRequestService(VisitRequestRepository visitRepository,
                               PropertyRepository propertyRepository,
                               UserRepository userRepository,
                               NotificationService notificationService,
                               AuditLogService auditLogService) {
        this.visitRepository = visitRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    public VisitRequest create(Long propertyId, Long renterId, VisitRequestForm form) {
        if (form.getRequestedDate() == null)
            throw new IllegalArgumentException("Please choose a preferred date.");
        if (form.getRequestedDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("The date must be today or later.");
        if (form.getRequestedTime() == null || form.getRequestedTime().isBlank())
            throw new IllegalArgumentException("Please choose a preferred time.");

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found."));

        if (property.getOwner() == null)
            throw new IllegalArgumentException("This listing has no owner.");

        if (property.getOwner().getId().equals(renterId))
            throw new IllegalArgumentException("You cannot request a visit on your own listing.");

        if (property.getStatus() != ListingStatus.PUBLISHED)
            throw new IllegalArgumentException("This listing is not currently available.");

        User renter = userRepository.findById(renterId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        VisitRequest v = new VisitRequest();
        v.setProperty(property);
        v.setRenter(renter);
        v.setOwner(property.getOwner());
        v.setRequestedDate(form.getRequestedDate());
        v.setRequestedTime(form.getRequestedTime().trim());
        v.setMessage(form.getMessage());
        v.setStatus(VisitStatus.PENDING);

        VisitRequest saved = visitRepository.save(v);

        notificationService.notifyUser(
                property.getOwner().getId(),
                NotificationType.VISIT_REQUESTED,
                "New visit request",
                renter.getFirstName() + " " + renter.getLastName()
                        + " requested a visit to \"" + property.getTitle() + "\".",
                property.getId());

        auditLogService.record(renter.getEmail(), AuditAction.VISIT_RESCHEDULED,
                "New request on " + property.getTitle());

        return saved;
    }

    public List<VisitRequest> listForRenter(Long renterId) {
        return visitRepository.findByRenterIdOrderByCreatedAtDesc(renterId);
    }

    public List<VisitRequest> listForOwner(Long ownerId) {
        return visitRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    public Map<VisitStatus, Long> summaryForOwner(Long ownerId) {
        Map<VisitStatus, Long> summary = new EnumMap<>(VisitStatus.class);
        for (VisitRequest v : listForOwner(ownerId)) {
            summary.merge(v.getStatus(), 1L, Long::sum);
        }
        return summary;
    }

    public Map<VisitStatus, Long> summaryForRenter(Long renterId) {
        Map<VisitStatus, Long> summary = new EnumMap<>(VisitStatus.class);
        for (VisitRequest v : listForRenter(renterId)) {
            summary.merge(v.getStatus(), 1L, Long::sum);
        }
        return summary;
    }

    public void accept(Long id, Long ownerId, String ownerNote) {
        VisitRequest v = visitRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (v.getStatus() != VisitStatus.PENDING && v.getStatus() != VisitStatus.RESCHEDULED)
            throw new IllegalArgumentException("This request can no longer be accepted.");

        v.setStatus(VisitStatus.ACCEPTED);
        v.setOwnerNote(ownerNote != null && !ownerNote.isBlank() ? ownerNote.trim() : null);
        visitRepository.save(v);

        notificationService.notifyUser(
                v.getRenter().getId(),
                NotificationType.VISIT_ACCEPTED,
                "Visit request accepted",
                "Your visit to \"" + v.getProperty().getTitle() + "\" on "
                        + v.getRequestedDate() + " at " + v.getRequestedTime() + " was accepted.",
                v.getProperty().getId());
    }

    public void reject(Long id, Long ownerId, String ownerNote) {
        VisitRequest v = visitRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (v.getStatus() == VisitStatus.CANCELLED || v.getStatus() == VisitStatus.COMPLETED)
            throw new IllegalArgumentException("This request can no longer be rejected.");

        v.setStatus(VisitStatus.REJECTED);
        v.setOwnerNote(ownerNote != null && !ownerNote.isBlank() ? ownerNote.trim() : null);
        visitRepository.save(v);

        notificationService.notifyUser(
                v.getRenter().getId(),
                NotificationType.VISIT_REJECTED,
                "Visit request declined",
                "Your visit to \"" + v.getProperty().getTitle() + "\" was declined."
                        + (v.getOwnerNote() != null ? " Reason: " + v.getOwnerNote() : ""),
                v.getProperty().getId());
    }

    public void reschedule(Long id, Long ownerId,
                           LocalDate newDate, String newTime, String ownerNote) {
        if (newDate == null || newTime == null || newTime.isBlank())
            throw new IllegalArgumentException("Please propose a new date and time.");
        if (newDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("The new date must be today or later.");

        VisitRequest v = visitRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (v.getStatus() == VisitStatus.CANCELLED || v.getStatus() == VisitStatus.COMPLETED)
            throw new IllegalArgumentException("This request can no longer be rescheduled.");

        v.setStatus(VisitStatus.RESCHEDULED);
        v.setProposedDate(newDate);
        v.setProposedTime(newTime.trim());
        v.setOwnerNote(ownerNote != null && !ownerNote.isBlank() ? ownerNote.trim() : null);
        visitRepository.save(v);

        notificationService.notifyUser(
                v.getRenter().getId(),
                NotificationType.VISIT_RESCHEDULED,
                "Visit rescheduled",
                "The owner proposed a new time for your visit to \"" + v.getProperty().getTitle()
                        + "\": " + newDate + " at " + newTime + ".",
                v.getProperty().getId());
    }

    public void confirmReschedule(Long id, Long renterId) {
        VisitRequest v = visitRepository.findByIdAndRenterId(id, renterId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (v.getStatus() != VisitStatus.RESCHEDULED)
            throw new IllegalArgumentException("This request is not in a reschedule state.");

        v.setRequestedDate(v.getProposedDate());
        v.setRequestedTime(v.getProposedTime());
        v.setProposedDate(null);
        v.setProposedTime(null);
        v.setStatus(VisitStatus.ACCEPTED);
        visitRepository.save(v);

        notificationService.notifyUser(
                v.getOwner().getId(),
                NotificationType.VISIT_ACCEPTED,
                "Reschedule confirmed",
                v.getRenter().getFirstName() + " confirmed the new time for \""
                        + v.getProperty().getTitle() + "\".",
                v.getProperty().getId());
    }

    public void cancel(Long id, Long renterId) {
        VisitRequest v = visitRepository.findByIdAndRenterId(id, renterId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found."));
        if (v.getStatus() == VisitStatus.CANCELLED || v.getStatus() == VisitStatus.COMPLETED)
            throw new IllegalArgumentException("This request can no longer be cancelled.");

        v.setStatus(VisitStatus.CANCELLED);
        visitRepository.save(v);

        notificationService.notifyUser(
                v.getOwner().getId(),
                NotificationType.VISIT_CANCELLED,
                "Visit cancelled",
                v.getRenter().getFirstName() + " cancelled the visit to \""
                        + v.getProperty().getTitle() + "\".",
                v.getProperty().getId());
    }
}