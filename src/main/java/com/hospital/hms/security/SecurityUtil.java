package com.hospital.hms.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            return null;
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    public static Long getCurrentUserId() {
        UserPrincipal p = getCurrentUser();
        return p == null ? null : p.getId();
    }
}
