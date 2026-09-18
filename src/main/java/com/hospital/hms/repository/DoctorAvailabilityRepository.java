package com.hospital.hms.repository;

import com.hospital.hms.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {
    List<DoctorAvailability> findByDoctor_Id(Long doctorId);
    List<DoctorAvailability> findByDoctor_IdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
    void deleteByDoctor_IdAndId(Long doctorId, Long availabilityId);
}
