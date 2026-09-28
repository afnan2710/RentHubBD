package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.ReviewForm;
import com.aoop.renthubbd.model.*;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.repository.ReviewRepository;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public ReviewService(ReviewRepository reviewRepository,
                         PropertyRepository propertyRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         AuditLogService auditLogService) {
        this.reviewRepository = reviewRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    public List<Review> getForProperty(Long propertyId) {
        return reviewRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId);
    }

    public List<Review> getForOwner(Long ownerId) {
        return reviewRepository.findByPropertyOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    public boolean hasReviewed(Long propertyId, Long reviewerId) {
        return reviewRepository.existsByPropertyIdAndReviewerId(propertyId, reviewerId);
    }

    public double averageRating(Long propertyId) {
        List<Review> reviews = reviewRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId);
        if (reviews.isEmpty()) return 0.0;
        int sum = 0;
        for (Review r : reviews) sum += r.getRating();
        return (double) sum / reviews.size();
    }

    public Map<Integer, Long> ratingDistribution(Long propertyId) {
        Map<Integer, Long> dist = new HashMap<>();
        for (int i = 1; i <= 5; i++) dist.put(i, 0L);
        for (Review r : reviewRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId)) {
            dist.merge(r.getRating(), 1L, Long::sum);
        }
        return dist;
    }

    public Review postReview(Long propertyId, Long reviewerId, ReviewForm form) {
        if (form.getRating() == null || form.getRating() < 1 || form.getRating() > 5)
            throw new IllegalArgumentException("Rating must be between 1 and 5.");

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found."));

        if (property.getOwner() != null && property.getOwner().getId().equals(reviewerId))
            throw new IllegalArgumentException("You cannot review your own listing.");

        if (reviewRepository.existsByPropertyIdAndReviewerId(propertyId, reviewerId))
            throw new IllegalArgumentException("You have already reviewed this listing.");

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Review review = new Review();
        review.setProperty(property);
        review.setReviewer(reviewer);
        review.setRating(form.getRating());
        review.setTitle(form.getTitle() != null && !form.getTitle().isBlank() ? form.getTitle().trim() : null);
        review.setComment(form.getComment() != null && !form.getComment().isBlank() ? form.getComment().trim() : null);

        Review saved = reviewRepository.save(review);

        if (property.getOwner() != null) {
            notificationService.notifyUser(
                    property.getOwner().getId(),
                    NotificationType.NEW_REVIEW,
                    "New review on your listing",
                    reviewer.getFirstName() + " rated \"" + property.getTitle()
                            + "\" " + form.getRating() + " out of 5.",
                    property.getId());
        }

        auditLogService.record(reviewer.getEmail(), AuditAction.REVIEW_POSTED,
                property.getTitle() + " :: " + form.getRating() + "/5");

        return saved;
    }
}