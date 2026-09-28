package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.ListingForm;
import com.aoop.renthubbd.dto.OwnerDashboardStats;
import com.aoop.renthubbd.model.*;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import com.aoop.renthubbd.model.NotificationType;

@Service
public class ListingService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.listing.upload.dir}")
    private String listingUploadDir;

    public ListingService(PropertyRepository propertyRepository,
                          UserRepository userRepository,
                          NotificationService notificationService) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Property createListing(Long ownerId, ListingForm form) throws IOException {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("Owner account not found"));

        Property p = new Property();
        p.setOwner(owner);
        p.setTitle(form.getTitle().trim());
        p.setDescription(form.getDescription());
        p.setCategory(form.getCategory());
        p.setPropertyType(form.getPropertyType());

        p.setAddress(form.getAddress().trim());
        p.setCity(form.getCity().trim());
        p.setArea(form.getArea().trim());
        p.setPostalCode(form.getPostalCode());

        p.setMonthlyRent(form.getMonthlyRent());
        p.setServiceCharge(form.getServiceCharge());
        p.setParkingFee(form.getParkingFee());
        p.setUtilityEstimate(form.getUtilityEstimate());
        p.setAdvanceMonths(form.getAdvanceMonths());

        p.setSizeSqft(form.getSizeSqft());
        p.setBedrooms(form.getBedrooms());
        p.setBathrooms(form.getBathrooms());
        p.setFloorNumber(form.getFloorNumber());
        p.setTotalFloors(form.getTotalFloors());
        p.setAvailableFrom(form.getAvailableFrom());

        // Collections: build a Set from the form list (dedupes automatically)
        Set<String> facilitySet = new HashSet<>();
        if (form.getFacilities() != null) {
            for (String f : form.getFacilities()) {
                if (f != null && !f.isBlank()) facilitySet.add(f.trim());
            }
        }
        p.setFacilities(facilitySet);

        p.setPhotos(savePhotos(form.getPhotos()));
        p.setStatus(ListingStatus.PENDING);

        Property saved = propertyRepository.save(p);

        notificationService.notifyAllAdmins(
                NotificationType.LISTING_SUBMITTED,
                "New listing submitted",
                owner.getFirstName() + " " + owner.getLastName()
                        + " submitted \"" + saved.getTitle() + "\" for review.",
                saved.getId());

        return saved;
    }

    public List<Property> getOwnerListings(Long ownerId) {
        // Collections: List<Property> — naturally a course requirement
        return propertyRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    /** Map<ListingStatus, Long> — demonstrates Map usage for the course. */
    public Map<ListingStatus, Long> getOwnerStatusSummary(Long ownerId) {
        Map<ListingStatus, Long> summary = new EnumMap<>(ListingStatus.class);
        List<Property> listings = getOwnerListings(ownerId);
        for (Property p : listings) {
            summary.merge(p.getStatus(), 1L, Long::sum);
        }
        return summary;
    }

    private List<String> savePhotos(MultipartFile[] uploads) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        if (uploads == null || uploads.length == 0) return savedPaths;

        Path dir = Paths.get(listingUploadDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        for (MultipartFile file : uploads) {
            if (file == null || file.isEmpty()) continue;
            String cleanName = StringUtils.cleanPath(
                    Objects.requireNonNullElse(file.getOriginalFilename(), "photo"));
            String filename = UUID.randomUUID() + "_" + cleanName;
            Files.copy(file.getInputStream(), dir.resolve(filename));
            // store relative path from uploads/ so /uploads/** can serve it
            savedPaths.add("listing-photos/" + filename);
        }
        return savedPaths;
    }

    public OwnerDashboardStats getDashboardStats(Long ownerId) {
        OwnerDashboardStats stats = new OwnerDashboardStats();
        stats.setTotalListings(propertyRepository.countByOwnerId(ownerId));
        stats.setPublishedListings(propertyRepository.countByOwnerIdAndStatus(ownerId, ListingStatus.PUBLISHED));
        stats.setPendingListings(propertyRepository.countByOwnerIdAndStatus(ownerId, ListingStatus.PENDING));
        stats.setRejectedListings(propertyRepository.countByOwnerIdAndStatus(ownerId, ListingStatus.REJECTED));
        // These become real numbers once Visit / Message / Review entities exist.
        stats.setPendingVisits(0);
        stats.setUnreadMessages(0);
        stats.setAverageRating(null);
        return stats;
    }

    public List<Property> getRecentListings(Long ownerId, int limit) {
        List<Property> all = propertyRepository.findTop5ByOwnerIdOrderByCreatedAtDesc(ownerId);
        return all.size() > limit ? all.subList(0, limit) : all;
    }

    public Property getOwnedListing(Long listingId, Long ownerId) {
        return propertyRepository.findByIdAndOwnerId(listingId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found or not yours"));
    }

    /** Loads existing listing into a form object for the edit screen. */
    public ListingForm toForm(Property p) {
        ListingForm f = new ListingForm();
        f.setId(p.getId());
        f.setTitle(p.getTitle());
        f.setDescription(p.getDescription());
        f.setCategory(p.getCategory());
        f.setPropertyType(p.getPropertyType());
        f.setAddress(p.getAddress());
        f.setCity(p.getCity());
        f.setArea(p.getArea());
        f.setPostalCode(p.getPostalCode());
        f.setMonthlyRent(p.getMonthlyRent());
        f.setServiceCharge(p.getServiceCharge());
        f.setParkingFee(p.getParkingFee());
        f.setUtilityEstimate(p.getUtilityEstimate());
        f.setAdvanceMonths(p.getAdvanceMonths());
        f.setSizeSqft(p.getSizeSqft());
        f.setBedrooms(p.getBedrooms());
        f.setBathrooms(p.getBathrooms());
        f.setFloorNumber(p.getFloorNumber());
        f.setTotalFloors(p.getTotalFloors());
        f.setAvailableFrom(p.getAvailableFrom());
        f.setFacilities(new ArrayList<>(p.getFacilities()));
        return f;
    }

    /**
     * Updates an existing listing. Photos are REPLACED if new ones are uploaded,
     * otherwise old photos are kept. Status is reset to PENDING so an admin must
     * re-approve — matches the "edited version needs re-approval" rule.
     */
    public Property updateListing(Long ownerId, Long listingId, ListingForm form) throws IOException {
        Property p = getOwnedListing(listingId, ownerId);

        p.setTitle(form.getTitle().trim());
        p.setDescription(form.getDescription());
        p.setCategory(form.getCategory());
        p.setPropertyType(form.getPropertyType());
        p.setAddress(form.getAddress().trim());
        p.setCity(form.getCity().trim());
        p.setArea(form.getArea().trim());
        p.setPostalCode(form.getPostalCode());
        p.setMonthlyRent(form.getMonthlyRent());
        p.setServiceCharge(form.getServiceCharge());
        p.setParkingFee(form.getParkingFee());
        p.setUtilityEstimate(form.getUtilityEstimate());
        p.setAdvanceMonths(form.getAdvanceMonths());
        p.setSizeSqft(form.getSizeSqft());
        p.setBedrooms(form.getBedrooms());
        p.setBathrooms(form.getBathrooms());
        p.setFloorNumber(form.getFloorNumber());
        p.setTotalFloors(form.getTotalFloors());
        p.setAvailableFrom(form.getAvailableFrom());

        Set<String> facilitySet = new HashSet<>();
        if (form.getFacilities() != null) {
            for (String f : form.getFacilities()) {
                if (f != null && !f.isBlank()) facilitySet.add(f.trim());
            }
        }
        p.setFacilities(facilitySet);

        List<String> newPhotos = savePhotos(form.getPhotos());
        if (!newPhotos.isEmpty()) {
            p.setPhotos(newPhotos);   // replace
        }                          // else: keep existing photos

        p.setStatus(ListingStatus.PENDING);
        p.setRejectionReason(null);

        Property saved = propertyRepository.save(p);

        notificationService.notifyAllAdmins(
                NotificationType.LISTING_UPDATED,
                "Listing edited by owner",
                "An owner updated \"" + saved.getTitle() + "\". It is back in the review queue.",
                saved.getId());

        return saved;
    }
}