package com.hospital.hms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * On successful login:
 * - If the request contains Accept: application/json (static frontend via fetch),
 *   return a JSON payload with the user role so the JS can redirect.
 * - Otherwise perform the classic role-based HTTP redirect (Thymeleaf UI).
 */
@Component
@RequiredArgsConstructor
public class RoleBasedAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        String role = "ROLE_PATIENT";
        String redirectUrl = "/";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_ADMIN"   -> { role = "ROLE_ADMIN";   redirectUrl = "/admin/dashboard"; }
                case "ROLE_DOCTOR"  -> { role = "ROLE_DOCTOR";  redirectUrl = "/doctor/dashboard"; }
                case "ROLE_PATIENT" -> { role = "ROLE_PATIENT"; redirectUrl = "/patient/dashboard"; }
                default             -> redirectUrl = "/";
            }
        }

        String accept = request.getHeader("Accept");
        boolean isApiRequest = (accept != null && accept.contains("application/json"))
                || request.getRequestURI().startsWith("/api/");

        if (isApiRequest) {
            // Static frontend: return JSON so JS can handle routing
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            String name = authentication.getName();
            objectMapper.writeValue(response.getWriter(),
                    Map.of("success", true, "role", role, "name", name, "redirect", redirectUrl));
        } else {
            // Thymeleaf UI: classic redirect
            response.sendRedirect(request.getContextPath() + redirectUrl);
        }
    }
}