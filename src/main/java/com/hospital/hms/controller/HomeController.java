package com.hospital.hms.controller;

import com.hospital.hms.dto.PatientRegistrationDto;
import com.hospital.hms.service.AuthService;
import com.hospital.hms.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DoctorService doctorService;
    private final AuthService authService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("doctors", doctorService.findAllActive());
        return "home";
    }

    @GetMapping("/auth/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("patientRegistrationDto")) {
            model.addAttribute("patientRegistrationDto", new PatientRegistrationDto());
        }
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@Valid @ModelAttribute("patientRegistrationDto") PatientRegistrationDto dto,
                            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.registerPatient(dto);
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
        model.addAttribute("registered", true);
        return "auth/login";
    }

    @GetMapping("/error/403")
    public String forbidden() {
        return "error/403";
    }
}
