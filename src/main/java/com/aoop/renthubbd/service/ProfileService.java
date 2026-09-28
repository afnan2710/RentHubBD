package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.ProfileForm;
import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Account not found"));
    }

    public User updateProfile(Long userId, ProfileForm form) throws IOException {
        User user = getById(userId);

        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setCountryCode(form.getCountryCode());
        user.setPhoneNumber(form.getPhoneNumber().trim());
        user.setAddress(form.getAddress().trim());
        user.setDob(form.getDob());

        MultipartFile photo = form.getProfilePhoto();
        if (photo != null && !photo.isEmpty()) {
            Path dir = Paths.get(uploadDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            String cleanName = StringUtils.cleanPath(
                    Objects.requireNonNullElse(photo.getOriginalFilename(), "photo"));
            String filename = UUID.randomUUID() + "_" + cleanName;
            Files.copy(photo.getInputStream(), dir.resolve(filename));
            user.setProfilePhoto(filename);
        }

        return userRepository.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = getById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New passwords do not match.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}