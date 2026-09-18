package com.hospital.hms.config;

import com.hospital.hms.entity.*;
import com.hospital.hms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Seeds a starter admin account plus a few departments/doctors on first run,
 * so the app is immediately usable (login screen isn't a dead end).
 * Controlled by app.data.seed=true (see application-dev.properties / application-mysql.properties).
 * Safe to run repeatedly: every insert is guarded by an existence check.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.data.seed:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        seedAdmin();
        Department cardiology = seedDepartment("Cardiology", "Heart and cardiovascular care");
        Department ortho = seedDepartment("Orthopedics", "Bones, joints, and muscles");
        Department general = seedDepartment("General Medicine", "General health and primary care");
        Department peds = seedDepartment("Pediatrics", "Child health care");

        seedDoctor("dr.kumar@hospital.com", "Dr. Ramesh Kumar", cardiology, "Cardiologist",
                "MBBS, MD (Cardiology)", 12, new BigDecimal("800.00"));
        seedDoctor("dr.arun@hospital.com", "Dr. Arun Nair", ortho, "Orthopedic Surgeon",
                "MBBS, MS (Ortho)", 8, new BigDecimal("700.00"));
        seedDoctor("dr.priya@hospital.com", "Dr. Priya Sharma", general, "General Physician",
                "MBBS, MD (Medicine)", 6, new BigDecimal("500.00"));
        seedDoctor("dr.meena@hospital.com", "Dr. Meena Iyer", peds, "Pediatrician",
                "MBBS, MD (Pediatrics)", 10, new BigDecimal("600.00"));

        log.info("=========================================================");
        log.info(" Hospital Management System demo data ready.");
        log.info(" Admin login   -> admin@hospital.com / admin123");
        log.info(" Doctor login  -> dr.kumar@hospital.com / doctor123 (same pattern for other doctors)");
        log.info(" Register a new Patient account from the Sign Up page.");
        log.info("=========================================================");
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@hospital.com")) {
            return;
        }
        User admin = User.builder()
                .email("admin@hospital.com")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Hospital Administrator")
                .phone("9999999999")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
    }

    private Department seedDepartment(String name, String description) {
        return departmentRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> departmentRepository.save(
                        Department.builder().name(name).description(description).build()));
    }

    private void seedDoctor(String email, String fullName, Department department, String specialization,
                             String qualification, int experienceYears, BigDecimal fee) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("doctor123"))
                .fullName(fullName)
                .phone("9000000000")
                .role(Role.DOCTOR)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        Doctor doctor = Doctor.builder()
                .user(user)
                .department(department)
                .specialization(specialization)
                .qualification(qualification)
                .experienceYears(experienceYears)
                .consultationFee(fee)
                .bio("Experienced " + specialization.toLowerCase() + " dedicated to patient-centered care.")
                .active(true)
                .build();
        doctor = doctorRepository.save(doctor);

        // Mon-Fri 09:00-13:00 and 14:00-17:00, 30-minute slots
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            availabilityRepository.save(DoctorAvailability.builder()
                    .doctor(doctor).dayOfWeek(day)
                    .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(13, 0))
                    .slotDurationMinutes(30).build());
            availabilityRepository.save(DoctorAvailability.builder()
                    .doctor(doctor).dayOfWeek(day)
                    .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(17, 0))
                    .slotDurationMinutes(30).build());
        }
    }
}
