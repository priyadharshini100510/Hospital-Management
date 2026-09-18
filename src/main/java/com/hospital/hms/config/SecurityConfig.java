package com.hospital.hms.config;

import com.hospital.hms.security.CustomUserDetailsService;
import com.hospital.hms.security.RoleBasedAuthSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RoleBasedAuthSuccessHandler roleBasedAuthSuccessHandler;

    // True only when the active profile is 'dev' (see application-dev.properties).
    // Defaults to empty string (=false) so a misconfigured/missing profile fails
    // closed, not open.
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
            .authorizeHttpRequests(auth -> {
                auth
                    // Public
                    .requestMatchers("/", "/home", "/about", "/contact").permitAll()
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/error/**").permitAll()
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                // H2 console: dev-only. In any other profile this matcher is never
                // registered as permitAll, so it falls through to .anyRequest().authenticated()
                // and is further blocked below by disabling the H2 servlet entirely in prod.
                if (isH2ConsoleEnabled()) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                auth
                    // Role-scoped areas
                    .requestMatchers("/patient/**", "/api/patient/**").hasRole("PATIENT")
                    .requestMatchers("/doctor/**", "/api/doctor/**").hasRole("DOCTOR")
                    .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated();
            })
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(roleBasedAuthSuccessHandler)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider())
            // Frame options relaxed only for the H2 console (dev). Harmless to leave
            // as sameOrigin in prod since nothing else on this app needs framing.
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .csrf(csrf -> {
                if (isH2ConsoleEnabled()) {
                    csrf.ignoringRequestMatchers("/h2-console/**");
                }
            })
            .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"));

        return http.build();
    }
}
