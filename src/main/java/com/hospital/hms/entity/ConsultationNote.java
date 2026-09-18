package com.hospital.hms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Holds diagnosis, consultation notes and prescription for a completed appointment.
 * This forms the patient's medical history record for that visit.
 */
@Entity
@Table(name = "consultation_notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    @ToString.Exclude
    private Appointment appointment;

    @Column(length = 2000)
    private String diagnosis;

    @Column(length = 3000)
    private String notes;

    @Column(length = 3000)
    private String prescription;

    private String followUpAdvice;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
