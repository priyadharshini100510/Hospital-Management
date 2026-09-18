package com.hospital.hms.service.impl;

import com.hospital.hms.dto.PatientProfileUpdateDto;
import com.hospital.hms.entity.Patient;
import com.hospital.hms.exception.ResourceNotFoundException;
import com.hospital.hms.repository.PatientRepository;
import com.hospital.hms.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        return patientRepository.search(query.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Patient findById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Patient findByUserId(Long userId) {
        return patientRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for current user"));
    }

    @Override
    @Transactional
    public Patient updateProfile(Long patientId, PatientProfileUpdateDto dto) {
        Patient patient = findById(patientId);

        patient.getUser().setFullName(dto.getFullName().trim());
        patient.getUser().setPhone(dto.getPhone());

        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setAddress(dto.getAddress());
        patient.setBloodGroup(dto.getBloodGroup());
        patient.setEmergencyContactName(dto.getEmergencyContactName());
        patient.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        patient.setAllergies(dto.getAllergies());

        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            try {
                patient.setGender(Patient.Gender.valueOf(dto.getGender().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // keep existing value if invalid
            }
        }

        return patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return patientRepository.count();
    }
}
