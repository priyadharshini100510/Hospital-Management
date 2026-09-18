package com.hospital.hms.controller;

import com.hospital.hms.dto.DoctorCreateDto;
import com.hospital.hms.entity.*;
import com.hospital.hms.repository.UserRepository;
import com.hospital.hms.service.*;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DoctorService doctorService;
    private final PatientService patientService;
    private final DepartmentService departmentService;
    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("doctorCount", doctorService.findAll().size());
        model.addAttribute("patientCount", patientService.countAll());
        model.addAttribute("appointmentCount", appointmentService.findAll().size());
        model.addAttribute("todayAppointments", appointmentService.findAllByDate(LocalDate.now()));
        model.addAttribute("pendingCount", appointmentService.countByStatus(AppointmentStatus.PENDING));
        model.addAttribute("confirmedCount", appointmentService.countByStatus(AppointmentStatus.CONFIRMED));
        model.addAttribute("completedCount", appointmentService.countByStatus(AppointmentStatus.COMPLETED));
        model.addAttribute("departmentCount", departmentService.findAll().size());
        return "admin/dashboard";
    }

    // ===== Departments =====

    @GetMapping("/departments")
    public String departments(Model model) {
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("department", new Department());
        return "admin/departments";
    }

    @PostMapping("/departments")
    public String createDepartment(@Valid @ModelAttribute("department") Department department,
                                    BindingResult bindingResult, Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            return "admin/departments";
        }
        departmentService.create(department);
        redirectAttributes.addFlashAttribute("successMessage", "Department added.");
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/update")
    public String updateDepartment(@PathVariable Long id, @ModelAttribute Department department,
                                    RedirectAttributes redirectAttributes) {
        departmentService.update(id, department);
        redirectAttributes.addFlashAttribute("successMessage", "Department updated.");
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/delete")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        departmentService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Department deleted.");
        return "redirect:/admin/departments";
    }

    // ===== Doctors =====

    @GetMapping("/doctors")
    public String doctors(@RequestParam(value = "q", required = false) String q, Model model) {
        model.addAttribute("doctors", (q == null || q.isBlank()) ? doctorService.findAll() : doctorService.search(q));
        model.addAttribute("q", q);
        return "admin/doctors";
    }

    @GetMapping("/doctors/new")
    public String newDoctorForm(Model model) {
        model.addAttribute("doctorDto", new DoctorCreateDto());
        model.addAttribute("departments", departmentService.findAll());
        return "admin/doctor-form";
    }

    @PostMapping("/doctors")
    public String createDoctor(@Valid @ModelAttribute("doctorDto") DoctorCreateDto dto,
                                BindingResult bindingResult, Model model,
                                RedirectAttributes redirectAttributes) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            bindingResult.rejectValue("password", "error.doctorDto", "Password is required");
        } else if (dto.getPassword().trim().length() < 6) {
            bindingResult.rejectValue("password", "error.doctorDto", "Password must be at least 6 characters");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentService.findAll());
            return "admin/doctor-form";
        }
        try {
            doctorService.create(dto);
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("departments", departmentService.findAll());
            return "admin/doctor-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Doctor added successfully.");
        return "redirect:/admin/doctors";
    }

    @GetMapping("/doctors/{id}/edit")
    public String editDoctorForm(@PathVariable Long id, Model model) {
        Doctor doctor = doctorService.findById(id);
        DoctorCreateDto dto = new DoctorCreateDto();
        dto.setFullName(doctor.getUser().getFullName());
        dto.setEmail(doctor.getUser().getEmail());
        dto.setPhone(doctor.getUser().getPhone());
        dto.setDepartmentId(doctor.getDepartment() != null ? doctor.getDepartment().getId() : null);
        dto.setSpecialization(doctor.getSpecialization());
        dto.setQualification(doctor.getQualification());
        dto.setExperienceYears(doctor.getExperienceYears());
        dto.setConsultationFee(doctor.getConsultationFee());
        dto.setBio(doctor.getBio());

        model.addAttribute("doctorId", id);
        model.addAttribute("doctorDto", dto);
        model.addAttribute("departments", departmentService.findAll());
        return "admin/doctor-form";
    }

    @PostMapping("/doctors/{id}/update")
    public String updateDoctor(@PathVariable Long id, @Valid @ModelAttribute("doctorDto") DoctorCreateDto dto,
                                BindingResult bindingResult, Model model,
                                RedirectAttributes redirectAttributes) {
        if (dto.getPassword() != null && !dto.getPassword().isBlank() && dto.getPassword().trim().length() < 6) {
            bindingResult.rejectValue("password", "error.doctorDto", "Password must be at least 6 characters");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("doctorId", id);
            model.addAttribute("departments", departmentService.findAll());
            return "admin/doctor-form";
        }
        doctorService.update(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Doctor updated.");
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/{id}/delete")
    public String deleteDoctor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        doctorService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Doctor deactivated.");
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, @RequestParam boolean active,
                                RedirectAttributes redirectAttributes) {
        doctorService.setActive(id, active);
        redirectAttributes.addFlashAttribute("successMessage", "Doctor status updated.");
        return "redirect:/admin/doctors";
    }

    // ===== Patients =====

    @GetMapping("/patients")
    public String patients(@RequestParam(value = "q", required = false) String q, Model model) {
        model.addAttribute("patients", patientService.search(q));
        model.addAttribute("q", q);
        return "admin/patients";
    }

    @GetMapping("/patients/{id}")
    public String patientDetail(@PathVariable Long id, Model model) {
        Patient patient = patientService.findById(id);
        List<Appointment> appointments = appointmentService.findByPatient(id);
        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointments);
        return "admin/patient-detail";
    }

    // ===== Appointments =====

    @GetMapping("/appointments")
    public String appointments(@RequestParam(value = "date", required = false) String date,
                                @RequestParam(value = "status", required = false) String status,
                                Model model) {
        List<Appointment> appointments;
        LocalDate parsedDate = null;
        if (date != null && !date.isBlank()) {
            parsedDate = LocalDate.parse(date);
            appointments = appointmentService.findAllByDate(parsedDate);
        } else {
            appointments = appointmentService.findAll();
        }
        if (status != null && !status.isBlank()) {
            AppointmentStatus filter = AppointmentStatus.valueOf(status.toUpperCase());
            appointments = appointments.stream().filter(a -> a.getStatus() == filter).toList();
        }
        model.addAttribute("appointments", appointments);
        model.addAttribute("selectedDate", parsedDate);
        model.addAttribute("statusFilter", status);
        model.addAttribute("statuses", AppointmentStatus.values());
        return "admin/appointments";
    }

    @PostMapping("/appointments/{id}/status")
    public String updateAppointmentStatus(@PathVariable Long id, @RequestParam AppointmentStatus status,
                                           RedirectAttributes redirectAttributes) {
        appointmentService.adminUpdateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Appointment status updated.");
        return "redirect:/admin/appointments";
    }
}
