package com.aoop.renthubbd.controller;

import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.Random;

@Controller
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public ForgotPasswordController(UserRepository userRepository, PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @GetMapping("/forgetpassword")
    public String showForgetPasswordForm(HttpSession session, Model model) {
        String otpEmail = (String) session.getAttribute("otpEmail");
        if (otpEmail != null) {
            model.addAttribute("step", "verify");
            model.addAttribute("maskedEmail", maskEmail(otpEmail));
            model.addAttribute("captchaQuestion", session.getAttribute("captchaQuestion"));
        } else {
            model.addAttribute("step", "email");
        }
        return "forgetpassword";
    }

    @PostMapping("/forgetpassword/send-otp")
    public String sendOtp(@RequestParam String email, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userRepository.findByEmail(email.trim());
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No account found with this email.");
            return "redirect:/forgetpassword";
        }

        String otp = String.valueOf(new Random().nextInt(9000) + 1000);
        session.setAttribute("otp", otp);
        session.setAttribute("otpEmail", email.trim());
        session.setAttribute("otpExpiry", System.currentTimeMillis() + 5 * 60 * 1000);
        generateCaptcha(session);

        sendOtpEmail(email.trim(), otp);

        redirectAttributes.addFlashAttribute("success", "An OTP has been sent to your email.");
        return "redirect:/forgetpassword";
    }

    @GetMapping("/forgetpassword/resend")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String otpEmail = (String) session.getAttribute("otpEmail");
        if (otpEmail == null) {
            return "redirect:/forgetpassword";
        }

        String otp = String.valueOf(new Random().nextInt(9000) + 1000);
        session.setAttribute("otp", otp);
        session.setAttribute("otpExpiry", System.currentTimeMillis() + 5 * 60 * 1000);
        generateCaptcha(session);

        sendOtpEmail(otpEmail, otp);

        redirectAttributes.addFlashAttribute("success", "A new OTP has been sent.");
        return "redirect:/forgetpassword";
    }

    @GetMapping("/forgetpassword/reset-flow")
    public String resetFlow(HttpSession session) {
        session.removeAttribute("otp");
        session.removeAttribute("otpEmail");
        session.removeAttribute("otpExpiry");
        session.removeAttribute("captchaAnswer");
        session.removeAttribute("captchaQuestion");
        return "redirect:/forgetpassword";
    }

    @PostMapping("/forgetpassword/verify")
    public String verifyOtp(
            @RequestParam String otp,
            @RequestParam Integer captchaAnswer,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String sessionOtp = (String) session.getAttribute("otp");
        String otpEmail = (String) session.getAttribute("otpEmail");
        Long otpExpiry = (Long) session.getAttribute("otpExpiry");
        Integer expectedCaptcha = (Integer) session.getAttribute("captchaAnswer");

        if (sessionOtp == null || otpEmail == null || otpExpiry == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please start again.");
            return "redirect:/forgetpassword";
        }

        if (System.currentTimeMillis() > otpExpiry) {
            session.removeAttribute("otp");
            session.removeAttribute("otpEmail");
            session.removeAttribute("otpExpiry");
            redirectAttributes.addFlashAttribute("error", "Your OTP has expired. Please request a new one.");
            return "redirect:/forgetpassword";
        }

        if (expectedCaptcha == null || !expectedCaptcha.equals(captchaAnswer)) {
            generateCaptcha(session);
            redirectAttributes.addFlashAttribute("error", "That answer is incorrect. Try the new question.");
            return "redirect:/forgetpassword";
        }

        if (!sessionOtp.equals(otp.trim())) {
            generateCaptcha(session);
            redirectAttributes.addFlashAttribute("error", "Incorrect OTP. Please try again.");
            return "redirect:/forgetpassword";
        }

        session.setAttribute("otpVerified", true);
        session.removeAttribute("otp");
        session.removeAttribute("otpExpiry");
        return "redirect:/passwordreset";
    }

    @GetMapping("/passwordreset")
    public String showResetForm(HttpSession session) {
        Boolean verified = (Boolean) session.getAttribute("otpVerified");
        if (verified == null || !verified) {
            return "redirect:/forgetpassword";
        }
        return "passwordreset";
    }

    @PostMapping("/passwordreset")
    public String resetPassword(
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Boolean verified = (Boolean) session.getAttribute("otpVerified");
        String otpEmail = (String) session.getAttribute("otpEmail");

        if (verified == null || !verified || otpEmail == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please start again.");
            return "redirect:/forgetpassword";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/passwordreset";
        }

        Optional<User> userOptional = userRepository.findByEmail(otpEmail);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Account not found. Please start again.");
            return "redirect:/forgetpassword";
        }

        User user = userOptional.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        session.removeAttribute("otpEmail");
        session.removeAttribute("otpVerified");
        session.removeAttribute("captchaAnswer");
        session.removeAttribute("captchaQuestion");

        redirectAttributes.addFlashAttribute("success", "Your password has been reset. Please log in.");
        return "redirect:/login";
    }

    private void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("RentHubBD - Password Reset");
        message.setText("Your OTP to reset your RentHubBD account password is " + otp + ". This code expires in 5 minutes.");
        mailSender.send(message);
    }

    private void generateCaptcha(HttpSession session) {
        Random random = new Random();
        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        boolean subtract = random.nextBoolean();
        int answer;
        String question;
        if (subtract) {
            int larger = Math.max(a, b);
            int smaller = Math.min(a, b);
            answer = larger - smaller;
            question = larger + " - " + smaller;
        } else {
            answer = a + b;
            question = a + " + " + b;
        }
        session.setAttribute("captchaAnswer", answer);
        session.setAttribute("captchaQuestion", question);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String namePart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        String visible = namePart.substring(0, Math.min(2, namePart.length()));
        StringBuilder masked = new StringBuilder(visible);
        for (int i = visible.length(); i < namePart.length(); i++) {
            masked.append('*');
        }
        return masked + domainPart;
    }
}