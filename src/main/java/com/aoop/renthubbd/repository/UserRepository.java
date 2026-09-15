package com.aoop.renthubbd.repository;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);

    List<User> findAllByOrderByFirstNameAsc();
    List<User> findByUserTypeOrderByFirstNameAsc(UserType userType);
    List<User> findByStatusOrderByFirstNameAsc(AccountStatus status);

    long countByUserType(UserType userType);
    long countByStatus(AccountStatus status);
    long countByUserTypeAndStatus(UserType userType, AccountStatus status);
}