package com.hospital.hms.service;

import com.hospital.hms.dto.AvailabilityDto;
import com.hospital.hms.dto.DoctorCreateDto;
import com.hospital.hms.entity.Doctor;
import com.hospital.hms.entity.DoctorAvailability;

import java.util.List;

public interface DoctorService {
    List<Doctor> findAllActive();
    List<Doctor> findAll();
    List<Doctor> search(String query);
    Doctor findById(Long id);
    Doctor findByUserId(Long userId);
    Doctor create(DoctorCreateDto dto);
    Doctor update(Long id, DoctorCreateDto dto);
    void delete(Long id);
    void setActive(Long id, boolean active);

    // Availability
    DoctorAvailability addAvailability(Long doctorId, AvailabilityDto dto);
    void removeAvailability(Long doctorId, Long availabilityId);
    List<DoctorAvailability> getAvailabilities(Long doctorId);
    List<java.time.LocalTime> getAvailableSlots(Long doctorId, java.time.LocalDate date);
}
