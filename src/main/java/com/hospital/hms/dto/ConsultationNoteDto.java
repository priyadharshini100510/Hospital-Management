package com.hospital.hms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConsultationNoteDto {

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    private String notes;

    private String prescription;

    private String followUpAdvice;
}
