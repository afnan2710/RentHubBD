package com.aoop.renthubbd.service;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AuditAction;
import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationEmailService emailService;

    public UserManagementService(UserRepository userRepository,
                                 AuditLogService auditLogService,
                                 NotificationEmailService emailService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    public List<User> listAll() { return userRepository.findAllByOrderByFirstNameAsc(); }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public void toggleStatus(Long userId, Admin actor) {
        User u = getById(userId);
        boolean activating = u.getStatus() != AccountStatus.ACTIVE;
        u.setStatus(activating ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);
        userRepository.save(u);

        auditLogService.record(actor.getUsername(),
                activating ? AuditAction.USER_ACTIVATED : AuditAction.USER_DEACTIVATED,
                u.getEmail());

        emailService.sendAccountStatusEmail(u.getEmail(), u.getFirstName(), activating);
    }
}