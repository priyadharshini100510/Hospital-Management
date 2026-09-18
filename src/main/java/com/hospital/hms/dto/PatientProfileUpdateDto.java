package com.hospital.hms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientProfileUpdateDto {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String address;

    private String bloodGroup;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String allergies;
}
