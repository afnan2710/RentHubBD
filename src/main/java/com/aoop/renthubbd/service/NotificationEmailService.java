package com.aoop.renthubbd.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationEmailService {

    private final JavaMailSender mailSender;

    public NotificationEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendAccountStatusEmail(String to, String name, boolean activated) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(activated ? "RentHub BD - Account Reactivated" : "RentHub BD - Account Deactivated");
        msg.setText("Hello " + name + ",\n\nYour RentHub BD account has been "
                + (activated ? "reactivated. You can now log in." : "deactivated by an administrator.")
                + "\n\nIf you believe this is a mistake, reply to this email.");
        mailSender.send(msg);
    }

    @Async
    public void sendListingApprovedEmail(String to, String ownerName, String listingTitle) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("RentHub BD - Listing Approved");
        msg.setText("Hello " + ownerName + ",\n\nYour listing \"" + listingTitle
                + "\" has been approved and is now live on RentHub BD.");
        mailSender.send(msg);
    }

    @Async
    public void sendListingRejectedEmail(String to, String ownerName, String listingTitle, String reason) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("RentHub BD - Listing Rejected");
        msg.setText("Hello " + ownerName + ",\n\nYour listing \"" + listingTitle
                + "\" was rejected.\nReason: " + reason
                + "\n\nYou can edit it and resubmit for review.");
        mailSender.send(msg);
    }

    @Async
    public void sendAdminWelcomeEmail(String to, String username, String role) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("RentHub BD - Admin Account Created");
        msg.setText("Hello,\n\nAn admin account has been created.\n"
                + "Username: " + username + "\nRole: " + role
                + "\n\nPlease change your password after first login.");
        mailSender.send(msg);
    }
}