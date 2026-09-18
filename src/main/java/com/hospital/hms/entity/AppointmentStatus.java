package com.hospital.hms.entity;

public enum AppointmentStatus {
    PENDING,      // booked by patient, awaiting doctor confirmation
    CONFIRMED,    // accepted by doctor
    REJECTED,     // rejected by doctor
    CANCELLED,    // cancelled by patient
    COMPLETED,    // consultation done
    NO_SHOW       // patient did not show up
}
