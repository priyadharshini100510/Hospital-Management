package com.hospital.hms.controller.api;

import com.hospital.hms.dto.DoctorCreateDto;
import com.hospital.hms.entity.*;
import com.hospital.hms.repository.UserRepository;
import com.hospital.hms.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final DoctorService doctorService;
    private final PatientService patientService;
    private final DepartmentService departmentService;
    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    // ===== Dashboard =====

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(Map.of(
                "doctorCount", doctorService.findAll().size(),
                "patientCount", patientService.countAll(),
                "appointmentCount", appointmentService.findAll().size(),
                "departmentCount", departmentService.findAll().size(),
                "pendingCount", appointmentService.countByStatus(AppointmentStatus.PENDING),
                "confirmedCount", appointmentService.countByStatus(AppointmentStatus.CONFIRMED),
                "completedCount", appointmentService.countByStatus(AppointmentStatus.COMPLETED),
                "todayAppointments", appointmentService.findAllByDate(LocalDate.now())
                        .stream().map(this::toApptDto).toList()
        ));
    }

    // ===== Departments =====

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> departments() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    @PostMapping("/departments")
    public ResponseEntity<?> createDepartment(@Valid @RequestBody Department department) {
        departmentService.create(department);
        return ResponseEntity.ok(Map.of("success", true, "message", "Department added."));
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        departmentService.update(id, department);
        return ResponseEntity.ok(Map.of("success", true, "message", "Department updated."));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Department deleted."));
    }

    // ===== Doctors =====

    @GetMapping("/doctors")
    public ResponseEntity<List<Map<String, Object>>> doctors(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(
                ((q == null || q.isBlank()) ? doctorService.findAll() : doctorService.search(q))
                        .stream().map(this::toDoctorDto).toList());
    }

    @PostMapping("/doctors")
    public ResponseEntity<?> createDoctor(@Valid @RequestBody DoctorCreateDto dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Password is required."));
        }
        if (dto.getPassword().trim().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Password must be at least 6 characters."));
        }
        try {
            doctorService.create(dto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Doctor added successfully."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PutMapping("/doctors/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Long id, @Valid @RequestBody DoctorCreateDto dto) {
        if (dto.getPassword() != null && !dto.getPassword().isBlank() && dto.getPassword().trim().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Password must be at least 6 characters."));
        }
        doctorService.update(id, dto);
        return ResponseEntity.ok(Map.of("success", true, "message", "Doctor updated."));
    }

    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        doctorService.delete(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Doctor deactivated."));
    }

    @PutMapping("/doctors/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        doctorService.setActive(id, active);
        return ResponseEntity.ok(Map.of("success", true, "message", "Doctor status updated."));
    }

    // ===== Patients =====

    @GetMapping("/patients")
    public ResponseEntity<List<Map<String, Object>>> patients(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(patientService.search(q).stream().map(this::toPatientDto).toList());
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<?> patientDetail(@PathVariable Long id) {
        Patient patient = patientService.findById(id);
        List<Appointment> appointments = appointmentService.findByPatient(id);
        return ResponseEntity.ok(Map.of(
                "patient", toPatientDto(patient),
                "appointments", appointments.stream().map(this::toApptDto).toList()
        ));
    }

    // ===== Appointments =====

    @GetMapping("/appointments")
    public ResponseEntity<List<Map<String, Object>>> appointments(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status) {
        List<Appointment> appts;
        if (date != null && !date.isBlank()) {
            appts = appointmentService.findAllByDate(LocalDate.parse(date));
        } else {
            appts = appointmentService.findAll();
        }
        if (status != null && !status.isBlank()) {
            AppointmentStatus filter = AppointmentStatus.valueOf(status.toUpperCase());
            appts = appts.stream().filter(a -> a.getStatus() == filter).toList();
        }
        return ResponseEntity.ok(appts.stream().map(this::toApptDto).toList());
    }

    @PutMapping("/appointments/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AppointmentStatus status = AppointmentStatus.valueOf(body.get("status").toUpperCase());
        appointmentService.adminUpdateStatus(id, status);
        return ResponseEntity.ok(Map.of("success", true, "message", "Appointment status updated."));
    }

    // ===== DTOs =====

    private Map<String, Object> toDoctorDto(Doctor d) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", d.getId());
        dto.put("name", d.getUser().getFullName());
        dto.put("email", d.getUser().getEmail());
        dto.put("phone", d.getUser().getPhone() != null ? d.getUser().getPhone() : "");
        dto.put("specialization", d.getSpecialization() != null ? d.getSpecialization() : "");
        dto.put("qualification", d.getQualification() != null ? d.getQualification() : "");
        dto.put("experienceYears", d.getExperienceYears() != null ? d.getExperienceYears() : 0);
        dto.put("consultationFee", d.getConsultationFee() != null ? d.getConsultationFee() : 0);
        dto.put("department", d.getDepartment() != null ? d.getDepartment().getName() : "");
        dto.put("departmentId", d.getDepartment() != null ? d.getDepartment().getId() : null);
        dto.put("active", d.isActive());
        return dto;
    }

    private Map<String, Object> toPatientDto(Patient p) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", p.getId());
        dto.put("fullName", p.getUser().getFullName());
        dto.put("email", p.getUser().getEmail());
        dto.put("phone", p.getUser().getPhone() != null ? p.getUser().getPhone() : "");
        dto.put("gender", p.getGender() != null ? p.getGender().name() : "");
        dto.put("bloodGroup", p.getBloodGroup() != null ? p.getBloodGroup() : "");
        dto.put("address", p.getAddress() != null ? p.getAddress() : "");
        return dto;
    }

    private Map<String, Object> toApptDto(Appointment a) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", a.getId());
        dto.put("date", a.getAppointmentDate().toString());
        dto.put("time", a.getAppointmentTime().toString());
        dto.put("status", a.getStatus().name());
        dto.put("patientName", a.getPatient().getUser().getFullName());
        dto.put("doctorName", a.getDoctor().getUser().getFullName());
        dto.put("department", a.getDoctor().getDepartment() != null ? a.getDoctor().getDepartment().getName() : "");
        dto.put("reason", a.getReasonForVisit() != null ? a.getReasonForVisit() : "");
        return dto;
    }
}