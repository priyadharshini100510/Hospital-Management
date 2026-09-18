package com.hospital.hms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.hms.security.CustomUserDetailsService;
import com.hospital.hms.security.RoleBasedAuthSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RoleBasedAuthSuccessHandler roleBasedAuthSuccessHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    private boolean isH2ConsoleEnabled() {
        return activeProfiles.contains("dev");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(auth -> {
                auth
                    // Public pages & static assets
                    .requestMatchers("/", "/home", "/about", "/contact").permitAll()
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/error/**").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                    // Static frontend (HTML/JS/CSS)
                    .requestMatchers("/frontend/**").permitAll()
                    // Public REST
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                if (isH2ConsoleEnabled()) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                auth
                    .requestMatchers("/patient/**", "/api/patient/**").hasRole("PATIENT")
                    .requestMatchers("/doctor/**", "/api/doctor/**").hasRole("DOCTOR")
                    .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated();
            })
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(roleBasedAuthSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    String accept = request.getHeader("Accept");
                    if ((accept != null && accept.contains("application/json")) || request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        objectMapper.writeValue(response.getWriter(), Map.of("success", false, "message", "Invalid email or password"));
                    } else {
                        response.sendRedirect(request.getContextPath() + "/auth/login?error=true");
                    }
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider())
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .csrf(csrf -> {
                // Disable CSRF for REST API paths and login — protected by CORS + SameSite cookie
                csrf.ignoringRequestMatchers("/api/**", "/auth/login");
                if (isH2ConsoleEnabled()) {
                    csrf.ignoringRequestMatchers("/h2-console/**");
                }
            })
            .exceptionHandling(ex -> ex
                // Return JSON 401 for unauthenticated API requests
                .authenticationEntryPoint((request, response, e) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        objectMapper.writeValue(response.getWriter(),
                                Map.of("error", "Unauthorized", "message", "Please log in"));
                    } else {
                        response.sendRedirect("/auth/login");
                    }
                })
                // Return JSON 403 for forbidden API requests
                .accessDeniedHandler((request, response, e) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        objectMapper.writeValue(response.getWriter(),
                                Map.of("error", "Forbidden", "message", "Access denied"));
                    } else {
                        response.sendRedirect("/error/403");
                    }
                })
            );

        return http.build();
    }
}