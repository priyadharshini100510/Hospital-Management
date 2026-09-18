package com.hospital.hms.service;

import com.hospital.hms.dto.PatientProfileUpdateDto;
import com.hospital.hms.entity.Patient;

import java.util.List;

public interface PatientService {
    List<Patient> findAll();
    List<Patient> search(String query);
    Patient findById(Long id);
    Patient findByUserId(Long userId);
    Patient updateProfile(Long patientId, PatientProfileUpdateDto dto);
    long countAll();
}
