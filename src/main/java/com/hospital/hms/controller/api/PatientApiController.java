package com.hospital.hms.controller.api;

import com.hospital.hms.dto.AppointmentBookingDto;
import com.hospital.hms.dto.PatientProfileUpdateDto;
import com.hospital.hms.entity.*;
import com.hospital.hms.security.SecurityUtil;
import com.hospital.hms.service.AppointmentService;
import com.hospital.hms.service.DoctorService;
import com.hospital.hms.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientApiController {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    private Patient currentPatient() {
        return patientService.findByUserId(SecurityUtil.getCurrentUserId());
    }

    // ===== Dashboard =====

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        Patient patient = currentPatient();
        List<Appointment> all = appointmentService.findByPatient(patient.getId());
        List<Appointment> upcoming = all.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                .filter(a -> !a.getAppointmentDate().isBefore(LocalDate.now()))
                .sorted((a, b) -> {
                    int cmp = a.getAppointmentDate().compareTo(b.getAppointmentDate());
                    return cmp != 0 ? cmp : a.getAppointmentTime().compareTo(b.getAppointmentTime());
                })
                .toList();

        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("patientName", patient.getUser().getFullName());
        resp.put("nextAppointment", upcoming.isEmpty() ? null : toApptDto(upcoming.get(0)));
        resp.put("upcomingCount", upcoming.size());
        resp.put("totalAppointments", all.size());
        return ResponseEntity.ok(resp);
    }

    // ===== Doctors =====

    @GetMapping("/doctors")
    public ResponseEntity<List<Map<String, Object>>> browseDoctors(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(doctorService.search(q).stream().map(this::toDoctorDto).toList());
    }

    @GetMapping("/doctors/{id}")
    public ResponseEntity<Map<String, Object>> doctorDetail(@PathVariable Long id) {
        return ResponseEntity.ok(toDoctorDto(doctorService.findById(id)));
    }

    @GetMapping("/doctors/{id}/slots")
    public ResponseEntity<List<LocalTime>> slots(@PathVariable Long id, @RequestParam String date) {
        return ResponseEntity.ok(doctorService.getAvailableSlots(id, LocalDate.parse(date)));
    }

    // ===== Appointments =====

    @GetMapping("/appointments")
    public ResponseEntity<List<Map<String, Object>>> myAppointments() {
        Patient patient = currentPatient();
        return ResponseEntity.ok(appointmentService.findByPatient(patient.getId())
                .stream().map(this::toApptDto).toList());
    }

    @PostMapping("/appointments/book")
    public ResponseEntity<?> book(@Valid @RequestBody AppointmentBookingDto dto) {
        Patient patient = currentPatient();
        Appointment appt = appointmentService.book(patient.getId(), dto);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Appointment requested for " + appt.getAppointmentDate() + " at " + appt.getAppointmentTime(),
                "appointment", toApptDto(appt)
        ));
    }

    @PostMapping("/appointments/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        Patient patient = currentPatient();
        String reason = body != null ? body.get("reason") : null;
        appointmentService.cancelByPatient(id, patient.getId(), reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Appointment cancelled."));
    }

    // ===== History =====

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> history() {
        Patient patient = currentPatient();
        List<Map<String, Object>> completed = appointmentService.findByPatient(patient.getId()).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .map(this::toApptDto)
                .toList();
        return ResponseEntity.ok(completed);
    }

    // ===== Profile =====

    @GetMapping("/profile")
    public ResponseEntity<?> profile() {
        Patient patient = currentPatient();
        return ResponseEntity.ok(toPatientDto(patient));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody PatientProfileUpdateDto dto) {
        Patient patient = currentPatient();
        patientService.updateProfile(patient.getId(), dto);
        return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully."));
    }

    // ===== DTOs =====

    private Map<String, Object> toDoctorDto(Doctor d) {
        return Map.of(
                "id", d.getId(),
                "name", d.getUser().getFullName(),
                "specialization", d.getSpecialization() != null ? d.getSpecialization() : "",
                "department", d.getDepartment() != null ? d.getDepartment().getName() : "",
                "experienceYears", d.getExperienceYears() != null ? d.getExperienceYears() : 0,
                "consultationFee", d.getConsultationFee() != null ? d.getConsultationFee() : 0,
                "qualification", d.getQualification() != null ? d.getQualification() : "",
                "bio", d.getBio() != null ? d.getBio() : ""
        );
    }

    private Map<String, Object> toApptDto(Appointment a) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", a.getId());
        dto.put("date", a.getAppointmentDate().toString());
        dto.put("time", a.getAppointmentTime().toString());
        dto.put("status", a.getStatus().name());
        dto.put("doctorName", a.getDoctor().getUser().getFullName());
        dto.put("department", a.getDoctor().getDepartment() != null ? a.getDoctor().getDepartment().getName() : "");
        dto.put("reason", a.getReasonForVisit() != null ? a.getReasonForVisit() : "");
        dto.put("cancelReason", a.getCancellationReason() != null ? a.getCancellationReason() : "");
        if (a.getConsultationNote() != null) {
            Map<String, String> noteMap = new java.util.LinkedHashMap<>();
            noteMap.put("diagnosis", a.getConsultationNote().getDiagnosis() != null ? a.getConsultationNote().getDiagnosis() : "");
            noteMap.put("notes", a.getConsultationNote().getNotes() != null ? a.getConsultationNote().getNotes() : "");
            noteMap.put("prescription", a.getConsultationNote().getPrescription() != null ? a.getConsultationNote().getPrescription() : "");
            noteMap.put("followUpAdvice", a.getConsultationNote().getFollowUpAdvice() != null ? a.getConsultationNote().getFollowUpAdvice() : "");
            dto.put("notes", noteMap);
        }
        return dto;
    }

    private Map<String, Object> toPatientDto(Patient p) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", p.getId());
        dto.put("fullName", p.getUser().getFullName());
        dto.put("email", p.getUser().getEmail());
        dto.put("phone", p.getUser().getPhone() != null ? p.getUser().getPhone() : "");
        dto.put("dateOfBirth", p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "");
        dto.put("gender", p.getGender() != null ? p.getGender().name() : "");
        dto.put("address", p.getAddress() != null ? p.getAddress() : "");
        dto.put("bloodGroup", p.getBloodGroup() != null ? p.getBloodGroup() : "");
        dto.put("emergencyContactName", p.getEmergencyContactName() != null ? p.getEmergencyContactName() : "");
        dto.put("emergencyContactPhone", p.getEmergencyContactPhone() != null ? p.getEmergencyContactPhone() : "");
        dto.put("allergies", p.getAllergies() != null ? p.getAllergies() : "");
        return dto;
    }
}