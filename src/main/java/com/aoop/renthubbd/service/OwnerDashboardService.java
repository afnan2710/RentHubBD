package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.OwnerDashboardStats;
import com.aoop.renthubbd.model.ListingStatus;
import com.aoop.renthubbd.model.VisitStatus;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.repository.VisitRequestRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class OwnerDashboardService {

    private final PropertyRepository propertyRepository;
    private final VisitRequestRepository visitRepository;
    private final ReviewService reviewService;
    private final ChatService chatService;

    public OwnerDashboardService(PropertyRepository propertyRepository,
                                 VisitRequestRepository visitRepository,
                                 ReviewService reviewService,
                                 ChatService chatService) {
        this.propertyRepository = propertyRepository;
        this.visitRepository = visitRepository;
        this.reviewService = reviewService;
        this.chatService = chatService;
    }

    public OwnerDashboardStats getStats(Long ownerId) {
        OwnerDashboardStats s = new OwnerDashboardStats();
        s.setTotalListings(propertyRepository.countByOwnerId(ownerId));
        s.setPublishedListings(propertyRepository.countByOwnerIdAndStatus(ownerId, ListingStatus.PUBLISHED));
        s.setPendingListings(propertyRepository.countByOwnerIdAndStatus(ownerId, ListingStatus.PENDING));
        s.setRejectedListings(propertyRepository.countByOwnerIdAndStatus(ownerId, ListingStatus.REJECTED));

        long pendingVisits = visitRepository.countByOwnerIdAndStatus(ownerId, VisitStatus.PENDING)
                + visitRepository.countByOwnerIdAndStatus(ownerId, VisitStatus.RESCHEDULED);
        s.setPendingVisits(pendingVisits);

        s.setUnreadMessages(chatService.totalUnreadForUser(ownerId));

        var reviews = reviewService.getForOwner(ownerId);
        if (reviews.isEmpty()) {
            s.setAverageRating(null);
        } else {
            int sum = 0;
            for (var r : reviews) sum += r.getRating();
            s.setAverageRating((double) sum / reviews.size());
        }

        return s;
    }

    public Map<VisitStatus, Long> visitSummary(Long ownerId) {
        Map<VisitStatus, Long> summary = new EnumMap<>(VisitStatus.class);
        for (VisitStatus st : VisitStatus.values()) {
            summary.put(st, visitRepository.countByOwnerIdAndStatus(ownerId, st));
        }
        return summary;
    }
}