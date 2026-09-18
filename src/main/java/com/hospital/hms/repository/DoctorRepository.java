package com.hospital.hms.repository;

import com.hospital.hms.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUser_Id(Long userId);

    Optional<Doctor> findByUser_Email(String email);

    List<Doctor> findByActiveTrue();

    List<Doctor> findByDepartment_IdAndActiveTrue(Long departmentId);

    @Query("SELECT d FROM Doctor d WHERE d.active = true AND (" +
           "LOWER(d.user.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(d.department.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Doctor> search(@Param("q") String query);

    long countByActiveTrue();
}
