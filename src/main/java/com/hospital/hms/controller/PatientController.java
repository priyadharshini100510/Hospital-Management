package com.hospital.hms.controller;

import com.hospital.hms.dto.AppointmentBookingDto;
import com.hospital.hms.dto.PatientProfileUpdateDto;
import com.hospital.hms.entity.Appointment;
import com.hospital.hms.entity.AppointmentStatus;
import com.hospital.hms.entity.Doctor;
import com.hospital.hms.entity.Patient;
import com.hospital.hms.security.SecurityUtil;
import com.hospital.hms.service.AppointmentService;
import com.hospital.hms.service.DoctorService;
import com.hospital.hms.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    private Patient currentPatient() {
        return patientService.findByUserId(SecurityUtil.getCurrentUserId());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Patient patient = currentPatient();
        List<Appointment> upcoming = appointmentService.findByPatient(patient.getId()).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                .filter(a -> !a.getAppointmentDate().isBefore(LocalDate.now()))
                .sorted((a, b) -> {
                    int cmp = a.getAppointmentDate().compareTo(b.getAppointmentDate());
                    return cmp != 0 ? cmp : a.getAppointmentTime().compareTo(b.getAppointmentTime());
                })
                .toList();

        model.addAttribute("patient", patient);
        model.addAttribute("nextAppointment", upcoming.isEmpty() ? null : upcoming.get(0));
        model.addAttribute("upcomingCount", upcoming.size());
        model.addAttribute("totalAppointments", appointmentService.findByPatient(patient.getId()).size());
        return "patient/dashboard";
    }

    @GetMapping("/doctors")
    public String browseDoctors(@RequestParam(value = "q", required = false) String q, Model model) {
        model.addAttribute("doctors", doctorService.search(q));
        model.addAttribute("q", q);
        return "patient/doctors";
    }

    @GetMapping("/doctors/{id}")
    public String doctorDetails(@PathVariable Long id, Model model) {
        Doctor doctor = doctorService.findById(id);
        model.addAttribute("doctor", doctor);
        model.addAttribute("bookingDto", new AppointmentBookingDto());
        return "patient/doctor-details";
    }

    @GetMapping("/doctors/{id}/slots")
    @ResponseBody
    public List<LocalTime> getSlots(@PathVariable Long id, @RequestParam String date) {
        return doctorService.getAvailableSlots(id, LocalDate.parse(date));
    }

    @GetMapping("/appointments/book")
    public String bookForm(@RequestParam(required = false) Long doctorId, Model model) {
        AppointmentBookingDto dto = new AppointmentBookingDto();
        if (doctorId != null) {
            dto.setDoctorId(doctorId);
        }
        model.addAttribute("bookingDto", dto);
        model.addAttribute("doctors", doctorService.findAllActive());
        return "patient/book-appointment";
    }

    @PostMapping("/appointments/book")
    public String book(@Valid @ModelAttribute("bookingDto") AppointmentBookingDto dto,
                        BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("doctors", doctorService.findAllActive());
            return "patient/book-appointment";
        }
        Patient patient = currentPatient();
        Appointment appointment = appointmentService.book(patient.getId(), dto);
        redirectAttributes.addFlashAttribute("successMessage",
                "Appointment requested for " + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime()
                        + ". You'll be notified once the doctor confirms it.");
        return "redirect:/patient/appointments";
    }

    @GetMapping("/appointments")
    public String myAppointments(Model model) {
        Patient patient = currentPatient();
        model.addAttribute("appointments", appointmentService.findByPatient(patient.getId()));
        return "patient/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam(required = false) String reason,
                          RedirectAttributes redirectAttributes) {
        Patient patient = currentPatient();
        appointmentService.cancelByPatient(id, patient.getId(), reason);
        redirectAttributes.addFlashAttribute("successMessage", "Appointment cancelled.");
        return "redirect:/patient/appointments";
    }

    @GetMapping("/history")
    public String medicalHistory(Model model) {
        Patient patient = currentPatient();
        List<Appointment> completed = appointmentService.findByPatient(patient.getId()).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();
        model.addAttribute("appointments", completed);
        return "patient/history";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        Patient patient = currentPatient();
        PatientProfileUpdateDto dto = new PatientProfileUpdateDto();
        dto.setFullName(patient.getUser().getFullName());
        dto.setPhone(patient.getUser().getPhone());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setGender(patient.getGender() == null ? null : patient.getGender().name());
        dto.setAddress(patient.getAddress());
        dto.setBloodGroup(patient.getBloodGroup());
        dto.setEmergencyContactName(patient.getEmergencyContactName());
        dto.setEmergencyContactPhone(patient.getEmergencyContactPhone());
        dto.setAllergies(patient.getAllergies());

        model.addAttribute("profileDto", dto);
        return "patient/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileDto") PatientProfileUpdateDto dto,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "patient/profile";
        }
        Patient patient = currentPatient();
        patientService.updateProfile(patient.getId(), dto);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/patient/profile";
    }
}
