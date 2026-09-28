package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.AdminDashboardStats;
import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.ListingStatus;
import com.aoop.renthubbd.model.UserType;
import com.aoop.renthubbd.repository.AdminRepository;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AdminRepository adminRepository;

    public AdminDashboardService(UserRepository userRepository,
                                 PropertyRepository propertyRepository,
                                 AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.adminRepository = adminRepository;
    }

    public AdminDashboardStats getStats() {
        AdminDashboardStats s = new AdminDashboardStats();
        s.setTotalUsers(userRepository.count());
        s.setTotalRenters(userRepository.countByUserType(UserType.RENTER));
        s.setTotalOwners(userRepository.countByUserType(UserType.OWNER));
        s.setInactiveUsers(userRepository.countByStatus(AccountStatus.INACTIVE));
        s.setTotalListings(propertyRepository.count());
        s.setPendingListings(propertyRepository.countByStatus(ListingStatus.PENDING));
        s.setPublishedListings(propertyRepository.countByStatus(ListingStatus.PUBLISHED));
        s.setRejectedListings(propertyRepository.countByStatus(ListingStatus.REJECTED));
        s.setTotalAdmins(adminRepository.count());
        s.setActiveAdmins(adminRepository.countByStatus(AccountStatus.ACTIVE));
        return s;
    }

    public Map<String, Long> getPlatformSnapshot() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("Users", userRepository.count());
        m.put("Renters", userRepository.countByUserType(UserType.RENTER));
        m.put("Owners", userRepository.countByUserType(UserType.OWNER));
        m.put("Listings", propertyRepository.count());
        m.put("Pending", propertyRepository.countByStatus(ListingStatus.PENDING));
        m.put("Published", propertyRepository.countByStatus(ListingStatus.PUBLISHED));
        m.put("Admins", adminRepository.count());
        return m;
    }
}