package com.aoop.renthubbd.config;

import com.aoop.renthubbd.model.AccountStatus;
import com.aoop.renthubbd.model.Admin;
import com.aoop.renthubbd.repository.AdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminSessionInterceptor implements HandlerInterceptor {

    private final AdminRepository adminRepository;

    public AdminSessionInterceptor(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getRequestURI();

        if (!uri.startsWith("/admin")) return true;
        if (uri.startsWith("/admin/login")) return true;
        if (uri.startsWith("/admin/logout")) return true;

        HttpSession session = request.getSession(false);
        if (session == null) return true;

        Object adminId = session.getAttribute("adminId");
        if (!(adminId instanceof Long)) return true;

        Admin admin = adminRepository.findById((Long) adminId).orElse(null);
        if (admin == null || admin.getStatus() != AccountStatus.ACTIVE) {
            session.invalidate();
            response.sendRedirect("/admin/login?sessionEnded");
            return false;
        }
        return true;
    }
}