package com.hospital.hms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DoctorCreateDto {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    private String password;

    private String phone;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    private String qualification;

    private Integer experienceYears;

    private BigDecimal consultationFee;

    private String bio;
}
