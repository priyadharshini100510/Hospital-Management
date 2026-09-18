package com.hospital.hms.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentBookingDto {

    @NotNull(message = "Please select a doctor")
    private Long doctorId;

    @NotNull(message = "Please select a date")
    @Future(message = "Appointment date must be in the future")
    private LocalDate appointmentDate;

    @NotNull(message = "Please select a time slot")
    private LocalTime appointmentTime;

    private String reasonForVisit;
}
