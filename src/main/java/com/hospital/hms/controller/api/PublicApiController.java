package com.hospital.hms.controller.api;

import com.hospital.hms.entity.Department;
import com.hospital.hms.entity.Doctor;
import com.hospital.hms.service.DepartmentService;
import com.hospital.hms.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Read-only public REST endpoints (no auth required) consumed by the
 * landing page and by JavaScript on booking pages for dynamic slot lookup.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicApiController {

    private final DoctorService doctorService;
    private final DepartmentService departmentService;

    @GetMapping("/doctors")
    public List<Map<String, Object>> doctors(@RequestParam(required = false) String q) {
        List<Doctor> doctors = doctorService.search(q);
        return doctors.stream().map(this::toDto).toList();
    }

    @GetMapping("/departments")
    public List<Department> departments() {
        return departmentService.findAll();
    }

    @GetMapping("/doctors/{id}")
    public Map<String, Object> doctor(@PathVariable Long id) {
        return toDto(doctorService.findById(id));
    }

    @GetMapping("/doctors/{id}/slots")
    public List<LocalTime> slots(@PathVariable Long id, @RequestParam String date) {
        return doctorService.getAvailableSlots(id, LocalDate.parse(date));
    }

    private Map<String, Object> toDto(Doctor d) {
        return Map.of(
                "id", d.getId(),
                "name", d.getUser().getFullName(),
                "specialization", d.getSpecialization(),
                "department", d.getDepartment() != null ? d.getDepartment().getName() : "",
                "experienceYears", d.getExperienceYears() == null ? 0 : d.getExperienceYears(),
                "consultationFee", d.getConsultationFee()
        );
    }
}
