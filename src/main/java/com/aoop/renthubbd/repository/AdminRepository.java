package com.aoop.renthubbd.repository;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsername(String username);
    boolean existsByUsername(String username);

    List<Admin> findAllByOrderByCreatedAtDesc();
    long countByStatus(AccountStatus status);
    long countByRole(AdminRole role);
}