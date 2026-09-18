package com.hospital.hms.service;

import com.hospital.hms.dto.PatientRegistrationDto;
import com.hospital.hms.entity.Patient;

public interface AuthService {
    Patient registerPatient(PatientRegistrationDto dto);
}
