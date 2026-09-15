package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.UserType;
import com.aoop.renthubbd.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(
            @RequestParam String identifier,
            @RequestParam String password,
            @RequestParam String loginAs,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String trimmedIdentifier = identifier.trim();
        Optional<User> userOptional = trimmedIdentifier.contains("@")
                ? userRepository.findByEmail(trimmedIdentifier)
                : userRepository.findByPhoneNumber(trimmedIdentifier);

        String genericError = "Invalid credentials. Please recheck your email/phone and password and try again.";

        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", genericError);
            return "redirect:/login";
        }

        User user = userOptional.get();

        if (user.getStatus() != null && user.getStatus() == AccountStatus.INACTIVE) {
            redirectAttributes.addFlashAttribute("error", "Your account has been deactivated. Contact support.");
            return "redirect:/login";
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", genericError);
            return "redirect:/login";
        }

        if (!user.getUserType().name().equals(loginAs)) {
            redirectAttributes.addFlashAttribute("error", "Invalid credentials. Please recheck your email/phone and password and try again.");
            return "redirect:/login";
        }

        session.setAttribute("userId", user.getId());
        session.setAttribute("userType", user.getUserType().name());
        session.setAttribute("firstName", user.getFirstName());

        return user.getUserType() == UserType.OWNER ? "redirect:/owner/dashboard" : "redirect:/renter/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}