package com.aoop.renthubbd.config;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AdminRole;
import com.aoop.renthubbd.repository.AdminRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.count() > 0) return;

        Admin root = new Admin();
        root.setUsername("superadmin");
        root.setEmail(Admin.SHARED_EMAIL);
        root.setPassword(passwordEncoder.encode("SuperAdmin@123"));
        root.setRole(AdminRole.SUPERADMIN);
        root.setStatus(AccountStatus.ACTIVE);
        adminRepository.save(root);
    }
}