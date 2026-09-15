package com.aoop.renthubbd.service;

import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.model.Property;
import com.aoop.renthubbd.model.User;
import com.aoop.renthubbd.repository.AdminRepository;
import com.aoop.renthubbd.repository.PropertyRepository;
import com.aoop.renthubbd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class FileExportService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AdminRepository adminRepository;

    @Value("${app.export.dir}")
    private String exportDir;

    public FileExportService(UserRepository userRepository,
                             PropertyRepository propertyRepository,
                             AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.adminRepository = adminRepository;
    }

    @Async
    public CompletableFuture<Path> exportUsersCsv() {
        List<User> users = userRepository.findAllByOrderByFirstNameAsc();
        Path out = dir().resolve("users-" + LocalDateTime.now().format(STAMP) + ".csv");

        StringBuilder sb = new StringBuilder();
        sb.append("id,firstName,lastName,email,countryCode,phoneNumber,userType,status\n");
        for (User u : users) {
            sb.append(csv(u.getId())).append(',')
                    .append(csv(u.getFirstName())).append(',')
                    .append(csv(u.getLastName())).append(',')
                    .append(csv(u.getEmail())).append(',')
                    .append(csv(u.getCountryCode())).append(',')
                    .append(csv(u.getPhoneNumber())).append(',')
                    .append(csv(u.getUserType() != null ? u.getUserType().name() : "")).append(',')
                    .append(csv(u.getStatus() != null ? u.getStatus().name() : "")).append('\n');
        }
        write(out, sb.toString());
        return CompletableFuture.completedFuture(out);
    }

    @Async
    public CompletableFuture<Path> exportListingsCsv() {
        List<Property> listings = propertyRepository.findAllByOrderByCreatedAtDesc();
        Path out = dir().resolve("listings-" + LocalDateTime.now().format(STAMP) + ".csv");

        StringBuilder sb = new StringBuilder();
        sb.append("id,title,category,propertyType,city,area,monthlyRent,totalCost,status,ownerId\n");
        for (Property p : listings) {
            sb.append(csv(p.getId())).append(',')
                    .append(csv(p.getTitle())).append(',')
                    .append(csv(p.getCategory() != null ? p.getCategory().name() : "")).append(',')
                    .append(csv(p.getPropertyType() != null ? p.getPropertyType().name() : "")).append(',')
                    .append(csv(p.getCity())).append(',')
                    .append(csv(p.getArea())).append(',')
                    .append(csv(p.getMonthlyRent())).append(',')
                    .append(csv(p.getTotalMonthlyCost())).append(',')
                    .append(csv(p.getStatus() != null ? p.getStatus().name() : "")).append(',')
                    .append(csv(p.getOwner() != null ? p.getOwner().getId() : ""))
                    .append('\n');
        }
        write(out, sb.toString());
        return CompletableFuture.completedFuture(out);
    }

    @Async
    public CompletableFuture<Path> exportAdminsCsv() {
        List<Admin> admins = adminRepository.findAllByOrderByCreatedAtDesc();
        Path out = dir().resolve("admins-" + LocalDateTime.now().format(STAMP) + ".csv");

        StringBuilder sb = new StringBuilder();
        sb.append("id,username,email,role,status,createdAt\n");
        for (Admin a : admins) {
            sb.append(csv(a.getId())).append(',')
                    .append(csv(a.getUsername())).append(',')
                    .append(csv(a.getEmail())).append(',')
                    .append(csv(a.getRole() != null ? a.getRole().name() : "")).append(',')
                    .append(csv(a.getStatus() != null ? a.getStatus().name() : "")).append(',')
                    .append(csv(a.getCreatedAt())).append('\n');
        }
        write(out, sb.toString());
        return CompletableFuture.completedFuture(out);
    }

    private Path dir() {
        Path d = Paths.get(exportDir);
        try { if (!Files.exists(d)) Files.createDirectories(d); }
        catch (IOException ignored) {}
        return d;
    }

    private void write(Path target, String content) {
        try { Files.writeString(target, content, StandardCharsets.UTF_8); }
        catch (IOException ignored) {}
    }

    private String csv(Object v) {
        if (v == null) return "";
        String s = v.toString().replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s + "\"";
        return s;
    }
}