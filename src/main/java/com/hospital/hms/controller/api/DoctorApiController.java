package com.hospital.hms.controller.api;

import com.hospital.hms.dto.AvailabilityDto;
import com.hospital.hms.dto.ConsultationNoteDto;
import com.hospital.hms.entity.*;
import com.hospital.hms.security.SecurityUtil;
import com.hospital.hms.service.AppointmentService;
import com.hospital.hms.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorApiController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    private Doctor currentDoctor() {
        return doctorService.findByUserId(SecurityUtil.getCurrentUserId());
    }

    // ===== Dashboard =====

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        Doctor doctor = currentDoctor();
        List<Appointment> todays = appointmentService.findByDoctorAndDate(doctor.getId(), LocalDate.now());
        return ResponseEntity.ok(Map.of(
                "doctorName", doctor.getUser().getFullName(),
                "specialization", doctor.getSpecialization() != null ? doctor.getSpecialization() : "",
                "todaysAppointments", todays.stream().map(this::toApptDto).toList(),
                "todayTotal", appointmentService.countTodayByDoctor(doctor.getId()),
                "todayPending", appointmentService.countTodayByDoctorAndStatus(doctor.getId(), AppointmentStatus.PENDING),
                "todayCompleted", appointmentService.countTodayByDoctorAndStatus(doctor.getId(), AppointmentStatus.COMPLETED)
        ));
    }

    // ===== Appointments =====

    @GetMapping("/appointments")
    public ResponseEntity<List<Map<String, Object>>> appointments(@RequestParam(required = false) String status) {
        Doctor doctor = currentDoctor();
        List<Appointment> appts = appointmentService.findByDoctor(doctor.getId());
        if (status != null && !status.isBlank()) {
            AppointmentStatus filter = AppointmentStatus.valueOf(status.toUpperCase());
            appts = appts.stream().filter(a -> a.getStatus() == filter).toList();
        }
        return ResponseEntity.ok(appts.stream().map(this::toApptDto).toList());
    }

    @GetMapping("/appointments/{id}")
    public ResponseEntity<?> appointmentDetail(@PathVariable Long id) {
        Appointment a = appointmentService.findById(id);
        return ResponseEntity.ok(toApptDetailDto(a));
    }

    @PutMapping("/appointments/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        Doctor doctor = currentDoctor();
        AppointmentStatus status = AppointmentStatus.valueOf(body.get("status").toUpperCase());
        String reason = body.get("reason");
        appointmentService.updateStatus(id, doctor.getId(), status, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Appointment marked as " + status.name().toLowerCase()));
    }

    @PostMapping("/appointments/{id}/notes")
    public ResponseEntity<?> addNotes(@PathVariable Long id, @Valid @RequestBody ConsultationNoteDto dto) {
        Doctor doctor = currentDoctor();
        appointmentService.addConsultationNote(id, doctor.getId(), dto);
        return ResponseEntity.ok(Map.of("success", true, "message", "Consultation notes saved. Appointment marked completed."));
    }

    // ===== Patients =====

    @GetMapping("/patients")
    public ResponseEntity<List<Map<String, Object>>> patients() {
        Doctor doctor = currentDoctor();
        return ResponseEntity.ok(appointmentService.findByDoctor(doctor.getId())
                .stream().map(this::toApptDto).toList());
    }

    // ===== Availability =====

    @GetMapping("/availability")
    public ResponseEntity<?> availability() {
        Doctor doctor = currentDoctor();
        return ResponseEntity.ok(Map.of(
                "doctor", toDoctorDto(doctor),
                "availabilities", doctorService.getAvailabilities(doctor.getId()).stream()
                        .map(this::toAvailDto).toList()
        ));
    }

    @PostMapping("/availability")
    public ResponseEntity<?> addAvailability(@Valid @RequestBody AvailabilityDto dto) {
        Doctor doctor = currentDoctor();
        doctorService.addAvailability(doctor.getId(), dto);
        return ResponseEntity.ok(Map.of("success", true, "message", "Availability slot added."));
    }

    @DeleteMapping("/availability/{availabilityId}")
    public ResponseEntity<?> deleteAvailability(@PathVariable Long availabilityId) {
        Doctor doctor = currentDoctor();
        doctorService.removeAvailability(doctor.getId(), availabilityId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Availability slot removed."));
    }

    // ===== Profile =====

    @GetMapping("/profile")
    public ResponseEntity<?> profile() {
        return ResponseEntity.ok(toDoctorDto(currentDoctor()));
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
        dto.put("bio", d.getBio() != null ? d.getBio() : "");
        dto.put("department", d.getDepartment() != null ? d.getDepartment().getName() : "");
        dto.put("active", d.isActive());
        return dto;
    }

    private Map<String, Object> toApptDto(Appointment a) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", a.getId());
        dto.put("date", a.getAppointmentDate().toString());
        dto.put("time", a.getAppointmentTime().toString());
        dto.put("status", a.getStatus().name());
        dto.put("patientName", a.getPatient().getUser().getFullName());
        dto.put("patientId", a.getPatient().getId());
        dto.put("reason", a.getReasonForVisit() != null ? a.getReasonForVisit() : "");
        return dto;
    }

    private Map<String, Object> toApptDetailDto(Appointment a) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", a.getId());
        dto.put("date", a.getAppointmentDate().toString());
        dto.put("time", a.getAppointmentTime().toString());
        dto.put("status", a.getStatus().name());
        dto.put("reason", a.getReasonForVisit() != null ? a.getReasonForVisit() : "");
        dto.put("cancelReason", a.getCancellationReason() != null ? a.getCancellationReason() : "");
        dto.put("patientName", a.getPatient().getUser().getFullName());
        dto.put("patientId", a.getPatient().getId());
        dto.put("patientPhone", a.getPatient().getUser().getPhone() != null ? a.getPatient().getUser().getPhone() : "");
        if (a.getConsultationNote() != null) {
            dto.put("notes", Map.of(
                    "diagnosis", a.getConsultationNote().getDiagnosis() != null ? a.getConsultationNote().getDiagnosis() : "",
                    "prescription", a.getConsultationNote().getPrescription() != null ? a.getConsultationNote().getPrescription() : "",
                    "followUpAdvice", a.getConsultationNote().getFollowUpAdvice() != null ? a.getConsultationNote().getFollowUpAdvice() : ""
            ));
        }
        return dto;
    }

    private Map<String, Object> toAvailDto(DoctorAvailability av) {
        return Map.of(
                "id", av.getId(),
                "dayOfWeek", av.getDayOfWeek().name(),
                "startTime", av.getStartTime().toString(),
                "endTime", av.getEndTime().toString(),
                "slotDurationMinutes", av.getSlotDurationMinutes()
        );
    }
}