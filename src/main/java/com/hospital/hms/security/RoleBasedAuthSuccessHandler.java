package com.hospital.hms.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        String redirectUrl = "/";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_ADMIN" -> redirectUrl = "/admin/dashboard";
                case "ROLE_DOCTOR" -> redirectUrl = "/doctor/dashboard";
                case "ROLE_PATIENT" -> redirectUrl = "/patient/dashboard";
                default -> redirectUrl = "/";
            }
        }
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
