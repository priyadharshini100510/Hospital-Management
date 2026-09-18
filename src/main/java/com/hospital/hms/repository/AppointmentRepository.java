package com.hospital.hms.repository;

import com.hospital.hms.entity.Appointment;
import com.hospital.hms.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatient_IdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    List<Appointment> findByDoctor_IdOrderByAppointmentDateDescAppointmentTimeDesc(Long doctorId);

    List<Appointment> findByDoctor_IdAndAppointmentDateOrderByAppointmentTimeAsc(Long doctorId, LocalDate date);

    List<Appointment> findByDoctor_IdAndAppointmentDateAndStatusIn(
            Long doctorId, LocalDate date, List<AppointmentStatus> statuses);

    List<Appointment> findByPatient_IdAndStatusIn(Long patientId, List<AppointmentStatus> statuses);

    long countByDoctor_IdAndAppointmentDate(Long doctorId, LocalDate date);

    long countByDoctor_IdAndAppointmentDateAndStatus(Long doctorId, LocalDate date, AppointmentStatus status);

    long countByStatus(AppointmentStatus status);

    long countByAppointmentDate(LocalDate date);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date ORDER BY a.appointmentTime ASC")
    List<Appointment> findAllByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDate = :date AND a.appointmentTime = :time " +
           "AND a.status IN ('PENDING','CONFIRMED')")
    boolean existsActiveBooking(@Param("doctorId") Long doctorId,
                                 @Param("date") LocalDate date,
                                 @Param("time") LocalTime time);

    List<Appointment> findTop5ByPatient_IdAndStatusOrderByAppointmentDateAscAppointmentTimeAsc(
            Long patientId, AppointmentStatus status);
}
