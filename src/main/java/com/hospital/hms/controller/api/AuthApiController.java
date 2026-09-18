package com.hospital.hms.controller.api;

import com.hospital.hms.dto.PatientRegistrationDto;
import com.hospital.hms.entity.User;
import com.hospital.hms.repository.UserRepository;
import com.hospital.hms.security.SecurityUtil;
import com.hospital.hms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * REST endpoints for authentication — consumed by the static frontend.
 * Spring Security still processes the actual /auth/login form POST;
 * this controller handles registration, current-user info, and logout for the API.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /** Returns current logged-in user info, or 401 if not authenticated. */
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        String role = "ROLE_" + user.getRole().name(); // ROLE_PATIENT, ROLE_DOCTOR, ROLE_ADMIN
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "name", user.getFullName(),
                "role", role
        ));
    }

    /** REST registration endpoint for the static frontend. */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody PatientRegistrationDto dto) {
        try {
            authService.registerPatient(dto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Registration successful. Please log in."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    /** REST logout — invalidates the session. */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}