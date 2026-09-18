package com.hospital.hms.service;

import com.hospital.hms.dto.AppointmentBookingDto;
import com.hospital.hms.dto.ConsultationNoteDto;
import com.hospital.hms.entity.Appointment;
import com.hospital.hms.entity.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    Appointment book(Long patientId, AppointmentBookingDto dto);

    Appointment findById(Long id);

    List<Appointment> findByPatient(Long patientId);

    List<Appointment> findByDoctor(Long doctorId);

    List<Appointment> findByDoctorAndDate(Long doctorId, LocalDate date);

    List<Appointment> findAllByDate(LocalDate date);

    List<Appointment> findAll();

    Appointment cancelByPatient(Long appointmentId, Long patientId, String reason);

    Appointment updateStatus(Long appointmentId, Long doctorId, AppointmentStatus newStatus, String reason);

    Appointment addConsultationNote(Long appointmentId, Long doctorId, ConsultationNoteDto dto);

    // Admin-level operations (no ownership restriction)
    Appointment adminUpdateStatus(Long appointmentId, AppointmentStatus newStatus);

    // Stats
    long countByStatus(AppointmentStatus status);
    long countToday();
    long countTodayByDoctorAndStatus(Long doctorId, AppointmentStatus status);
    long countTodayByDoctor(Long doctorId);
}
