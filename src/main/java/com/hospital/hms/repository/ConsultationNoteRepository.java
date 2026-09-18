package com.hospital.hms.repository;

import com.hospital.hms.entity.ConsultationNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultationNoteRepository extends JpaRepository<ConsultationNote, Long> {
    Optional<ConsultationNote> findByAppointment_Id(Long appointmentId);
}
