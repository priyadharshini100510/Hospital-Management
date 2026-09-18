package com.hospital.hms.repository;

import com.hospital.hms.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUser_Id(Long userId);

    Optional<Patient> findByUser_Email(String email);

    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.user.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.user.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Patient> search(@Param("q") String query);
}
