package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.model.UserType;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@Controller
public class RegisterController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public RegisterController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String countryCode,
            @RequestParam String phoneNumber,
            @RequestParam LocalDate dob,
            @RequestParam String address,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String userType,
            @RequestParam(required = false) MultipartFile profilePhoto,
            RedirectAttributes redirectAttributes
    ) {
        if (userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "This email is already registered.");
            return "redirect:/register";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/register";
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setCountryCode(countryCode);
        user.setPhoneNumber(phoneNumber);
        user.setDob(dob);
        user.setAddress(address);
        user.setPassword(passwordEncoder.encode(password));
        user.setUserType(UserType.valueOf(userType));

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String fileName = UUID.randomUUID() + "_" + StringUtils.cleanPath(profilePhoto.getOriginalFilename());
                Files.copy(profilePhoto.getInputStream(), uploadPath.resolve(fileName));
                user.setProfilePhoto(fileName);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Could not upload photo, try again.");
                return "redirect:/register";
            }
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Account created successfully. You can now log in.");
        return "redirect:/register";
    }
}