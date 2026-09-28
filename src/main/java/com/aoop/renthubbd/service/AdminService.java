package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.AdminRegisterForm;
import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AdminRole;
import com.aoop.renthubbd.model.AuditAction;
import com.aoop.renthubbd.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final NotificationEmailService emailService;

    public AdminService(AdminRepository adminRepository,
                        PasswordEncoder passwordEncoder,
                        AuditLogService auditLogService,
                        NotificationEmailService emailService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    public List<Admin> listAll() {
        return adminRepository.findAllByOrderByCreatedAtDesc();
    }

    public Admin register(AdminRegisterForm form, Admin actor) {
        if (actor.getRole() != AdminRole.SUPERADMIN)
            throw new IllegalStateException("Only superadmins can create admin accounts.");
        if (form.getUsername() == null || form.getUsername().isBlank())
            throw new IllegalArgumentException("Username is required.");
        if (form.getPassword() == null || form.getPassword().length() < 8)
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        if (!form.getPassword().equals(form.getConfirmPassword()))
            throw new IllegalArgumentException("Passwords do not match.");
        if (form.getRole() == null)
            throw new IllegalArgumentException("Role is required.");
        if (adminRepository.existsByUsername(form.getUsername().trim()))
            throw new IllegalArgumentException("Username is already taken.");

        Admin a = new Admin();
        a.setUsername(form.getUsername().trim());
        a.setEmail(Admin.SHARED_EMAIL);
        a.setPassword(passwordEncoder.encode(form.getPassword()));
        a.setRole(form.getRole());
        a.setStatus(AccountStatus.ACTIVE);
        Admin saved = adminRepository.save(a);

        auditLogService.record(actor.getUsername(), AuditAction.ADMIN_CREATED,
                saved.getUsername() + " as " + saved.getRole().name());
        emailService.sendAdminWelcomeEmail(saved.getEmail(), saved.getUsername(), saved.getRole().getLabel());
        return saved;
    }

    public void toggleStatus(Long adminId, Admin actor) {
        if (actor.getRole() != AdminRole.SUPERADMIN)
            throw new IllegalStateException("Only superadmins can change admin status.");
        Admin target = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found."));
        if (target.getId().equals(actor.getId()))
            throw new IllegalArgumentException("You cannot change your own status.");

        target.setStatus(target.getStatus() == AccountStatus.ACTIVE
                ? AccountStatus.INACTIVE : AccountStatus.ACTIVE);
        adminRepository.save(target);
        auditLogService.record(actor.getUsername(), AuditAction.ADMIN_STATUS_CHANGED,
                target.getUsername() + " -> " + target.getStatus().name());
    }
}