package com.hospital.hms.service.impl;

import com.hospital.hms.dto.PatientRegistrationDto;
import com.hospital.hms.entity.Patient;
import com.hospital.hms.entity.Role;
import com.hospital.hms.entity.User;
import com.hospital.hms.exception.DuplicateResourceException;
import com.hospital.hms.repository.PatientRepository;
import com.hospital.hms.repository.UserRepository;
import com.hospital.hms.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Patient registerPatient(PatientRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        User user = User.builder()
                .email(dto.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName().trim())
                .phone(dto.getPhone())
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        Patient.Gender gender = null;
        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            try {
                gender = Patient.Gender.valueOf(dto.getGender().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // leave null if invalid value supplied
            }
        }

        Patient patient = Patient.builder()
                .user(user)
                .dateOfBirth(dto.getDateOfBirth())
                .gender(gender)
                .address(dto.getAddress())
                .build();

        return patientRepository.save(patient);
    }
}
