package com.hospital.hms.controller;

import com.hospital.hms.dto.AvailabilityDto;
import com.hospital.hms.dto.ConsultationNoteDto;
import com.hospital.hms.entity.Appointment;
import com.hospital.hms.entity.AppointmentStatus;
import com.hospital.hms.entity.Doctor;
import com.hospital.hms.security.SecurityUtil;
import com.hospital.hms.service.AppointmentService;
import com.hospital.hms.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;

    private Doctor currentDoctor() {
        return doctorService.findByUserId(SecurityUtil.getCurrentUserId());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Doctor doctor = currentDoctor();
        List<Appointment> todaysAppointments = appointmentService.findByDoctorAndDate(doctor.getId(), LocalDate.now());

        model.addAttribute("doctor", doctor);
        model.addAttribute("todaysAppointments", todaysAppointments);
        model.addAttribute("todayTotal", appointmentService.countTodayByDoctor(doctor.getId()));
        model.addAttribute("todayPending", appointmentService.countTodayByDoctorAndStatus(doctor.getId(), AppointmentStatus.PENDING));
        model.addAttribute("todayCompleted", appointmentService.countTodayByDoctorAndStatus(doctor.getId(), AppointmentStatus.COMPLETED));
        return "doctor/dashboard";
    }

    @GetMapping("/appointments")
    public String appointments(@RequestParam(required = false) String status, Model model) {
        Doctor doctor = currentDoctor();
        List<Appointment> appointments = appointmentService.findByDoctor(doctor.getId());
        if (status != null && !status.isBlank()) {
            AppointmentStatus filter = AppointmentStatus.valueOf(status.toUpperCase());
            appointments = appointments.stream().filter(a -> a.getStatus() == filter).toList();
        }
        model.addAttribute("appointments", appointments);
        model.addAttribute("statusFilter", status);
        model.addAttribute("statuses", AppointmentStatus.values());
        return "doctor/appointments";
    }

    @GetMapping("/appointments/{id}")
    public String appointmentDetail(@PathVariable Long id, Model model) {
        Appointment appointment = appointmentService.findById(id);
        model.addAttribute("appointment", appointment);
        model.addAttribute("noteDto", new ConsultationNoteDto());
        return "doctor/appointment-detail";
    }

    @PostMapping("/appointments/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam AppointmentStatus status,
                                @RequestParam(required = false) String reason,
                                RedirectAttributes redirectAttributes) {
        Doctor doctor = currentDoctor();
        appointmentService.updateStatus(id, doctor.getId(), status, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Appointment marked as " + status.name().toLowerCase() + ".");
        return "redirect:/doctor/appointments/" + id;
    }

    @PostMapping("/appointments/{id}/notes")
    public String addNotes(@PathVariable Long id, @Valid @ModelAttribute("noteDto") ConsultationNoteDto dto,
                            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("appointment", appointmentService.findById(id));
            return "doctor/appointment-detail";
        }
        Doctor doctor = currentDoctor();
        appointmentService.addConsultationNote(id, doctor.getId(), dto);
        redirectAttributes.addFlashAttribute("successMessage", "Consultation notes saved. Appointment marked completed.");
        return "redirect:/doctor/appointments/" + id;
    }

    @GetMapping("/patients")
    public String patients(Model model) {
        Doctor doctor = currentDoctor();
        List<Appointment> all = appointmentService.findByDoctor(doctor.getId());
        model.addAttribute("appointments", all);
        return "doctor/patients";
    }

    @GetMapping("/availability")
    public String availability(Model model) {
        Doctor doctor = currentDoctor();
        model.addAttribute("doctor", doctor);
        model.addAttribute("availabilities", doctorService.getAvailabilities(doctor.getId()));
        model.addAttribute("availabilityDto", new AvailabilityDto());
        return "doctor/availability";
    }

    @PostMapping("/availability")
    public String addAvailability(@Valid @ModelAttribute("availabilityDto") AvailabilityDto dto,
                                   BindingResult bindingResult, Model model,
                                   RedirectAttributes redirectAttributes) {
        Doctor doctor = currentDoctor();
        if (bindingResult.hasErrors()) {
            model.addAttribute("doctor", doctor);
            model.addAttribute("availabilities", doctorService.getAvailabilities(doctor.getId()));
            return "doctor/availability";
        }
        doctorService.addAvailability(doctor.getId(), dto);
        redirectAttributes.addFlashAttribute("successMessage", "Availability slot added.");
        return "redirect:/doctor/availability";
    }

    @PostMapping("/availability/{availabilityId}/delete")
    public String deleteAvailability(@PathVariable Long availabilityId, RedirectAttributes redirectAttributes) {
        Doctor doctor = currentDoctor();
        doctorService.removeAvailability(doctor.getId(), availabilityId);
        redirectAttributes.addFlashAttribute("successMessage", "Availability slot removed.");
        return "redirect:/doctor/availability";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("doctor", currentDoctor());
        return "doctor/profile";
    }
}
