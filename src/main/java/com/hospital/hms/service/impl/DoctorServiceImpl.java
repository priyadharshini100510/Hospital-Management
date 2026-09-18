package com.hospital.hms.service.impl;

import com.hospital.hms.dto.AvailabilityDto;
import com.hospital.hms.dto.DoctorCreateDto;
import com.hospital.hms.entity.*;
import com.hospital.hms.exception.BusinessRuleException;
import com.hospital.hms.exception.DuplicateResourceException;
import com.hospital.hms.exception.ResourceNotFoundException;
import com.hospital.hms.repository.*;
import com.hospital.hms.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> findAllActive() {
        return doctorRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> search(String query) {
        if (query == null || query.isBlank()) {
            return findAllActive();
        }
        return doctorRepository.search(query.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Doctor findById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Doctor findByUserId(Long userId) {
        return doctorRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for current user"));
    }

    @Override
    @Transactional
    public Doctor create(DoctorCreateDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        User user = User.builder()
                .email(dto.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName().trim())
                .phone(dto.getPhone())
                .role(Role.DOCTOR)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        Doctor doctor = Doctor.builder()
                .user(user)
                .department(department)
                .specialization(dto.getSpecialization())
                .qualification(dto.getQualification())
                .experienceYears(dto.getExperienceYears())
                .consultationFee(dto.getConsultationFee() == null ? java.math.BigDecimal.ZERO : dto.getConsultationFee())
                .bio(dto.getBio())
                .active(true)
                .build();

        return doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public Doctor update(Long id, DoctorCreateDto dto) {
        Doctor doctor = findById(id);
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        doctor.setDepartment(department);
        doctor.setSpecialization(dto.getSpecialization());
        doctor.setQualification(dto.getQualification());
        doctor.setExperienceYears(dto.getExperienceYears());
        if (dto.getConsultationFee() != null) {
            doctor.setConsultationFee(dto.getConsultationFee());
        }
        doctor.setBio(dto.getBio());

        // Update linked user's basic info too
        User user = doctor.getUser();
        user.setFullName(dto.getFullName().trim());
        user.setPhone(dto.getPhone());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userRepository.save(user);

        return doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Doctor doctor = findById(id);
        // Soft-guard: prevent hard delete if doctor has active appointments; deactivate instead.
        doctor.setActive(false);
        doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public void setActive(Long id, boolean active) {
        Doctor doctor = findById(id);
        doctor.setActive(active);
        doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public DoctorAvailability addAvailability(Long doctorId, AvailabilityDto dto) {
        Doctor doctor = findById(doctorId);
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessRuleException("End time must be after start time.");
        }
        DoctorAvailability availability = DoctorAvailability.builder()
                .doctor(doctor)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .slotDurationMinutes(dto.getSlotDurationMinutes() == null ? 30 : dto.getSlotDurationMinutes())
                .build();
        return availabilityRepository.save(availability);
    }

    @Override
    @Transactional
    public void removeAvailability(Long doctorId, Long availabilityId) {
        availabilityRepository.deleteByDoctor_IdAndId(doctorId, availabilityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorAvailability> getAvailabilities(Long doctorId) {
        return availabilityRepository.findByDoctor_Id(doctorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalTime> getAvailableSlots(Long doctorId, LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        List<DoctorAvailability> availabilities = availabilityRepository.findByDoctor_IdAndDayOfWeek(doctorId, day);
        if (availabilities.isEmpty()) {
            return List.of();
        }

        // Build all candidate slots from availability windows
        List<LocalTime> candidateSlots = new ArrayList<>();
        for (DoctorAvailability a : availabilities) {
            LocalTime cursor = a.getStartTime();
            while (cursor.plusMinutes(a.getSlotDurationMinutes()).compareTo(a.getEndTime()) <= 0) {
                candidateSlots.add(cursor);
                cursor = cursor.plusMinutes(a.getSlotDurationMinutes());
            }
        }

        // Remove already-booked slots (PENDING or CONFIRMED)
        List<Appointment> bookedAppointments = appointmentRepository
                .findByDoctor_IdAndAppointmentDateAndStatusIn(doctorId, date,
                        List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED));
        Set<LocalTime> bookedTimes = bookedAppointments.stream()
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        return candidateSlots.stream()
                .filter(slot -> !bookedTimes.contains(slot))
                .collect(Collectors.toList());
    }
}
