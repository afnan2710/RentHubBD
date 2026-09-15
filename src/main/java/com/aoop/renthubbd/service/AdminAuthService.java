package com.aoop.renthubbd.service;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AuditAction;
import com.aoop.renthubbd.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AdminAuthService(AdminRepository adminRepository,
                            PasswordEncoder passwordEncoder,
                            AuditLogService auditLogService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public Admin authenticate(String username, String password) {
        Optional<Admin> found = adminRepository.findByUsername(username.trim());
        if (found.isEmpty()) {
            auditLogService.record(username, AuditAction.LOGIN_FAILED, "no such admin");
            return null;
        }
        Admin admin = found.get();
        if (admin.getStatus() != AccountStatus.ACTIVE) {
            auditLogService.record(username, AuditAction.LOGIN_BLOCKED, "inactive account");
            return null;
        }
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            auditLogService.record(username, AuditAction.LOGIN_FAILED, "bad password");
            return null;
        }
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);
        auditLogService.record(admin.getUsername(), AuditAction.LOGIN_SUCCESS, admin.getRole().name());
        return admin;
    }

    public void changePassword(Admin admin, String current, String next, String confirm) {
        if (!passwordEncoder.matches(current, admin.getPassword()))
            throw new IllegalArgumentException("Current password is incorrect.");
        if (next == null || next.length() < 8)
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        if (!next.equals(confirm))
            throw new IllegalArgumentException("New passwords do not match.");
        if (passwordEncoder.matches(next, admin.getPassword()))
            throw new IllegalArgumentException("New password must be different from current.");

        admin.setPassword(passwordEncoder.encode(next));
        adminRepository.save(admin);
        auditLogService.record(admin.getUsername(), AuditAction.PASSWORD_CHANGED, "self");
    }
}