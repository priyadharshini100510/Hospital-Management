package com.hospital.hms.service.impl;

import com.hospital.hms.dto.AppointmentBookingDto;
import com.hospital.hms.dto.ConsultationNoteDto;
import com.hospital.hms.entity.*;
import com.hospital.hms.exception.BusinessRuleException;
import com.hospital.hms.exception.ResourceNotFoundException;
import com.hospital.hms.repository.*;
import com.hospital.hms.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private static final Set<AppointmentStatus> ACTIVE_STATUSES =
            Set.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final ConsultationNoteRepository consultationNoteRepository;

    @Override
    @Transactional
    public Appointment book(Long patientId, AppointmentBookingDto dto) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!doctor.isActive()) {
            throw new BusinessRuleException("This doctor is not currently accepting appointments.");
        }

        // Validate the requested slot actually falls within one of the doctor's
        // recurring availability windows for that day of week.
        boolean withinAvailability = availabilityRepository
                .findByDoctor_IdAndDayOfWeek(doctor.getId(), dto.getAppointmentDate().getDayOfWeek())
                .stream()
                .anyMatch(a -> !dto.getAppointmentTime().isBefore(a.getStartTime())
                        && dto.getAppointmentTime().isBefore(a.getEndTime()));
        if (!withinAvailability) {
            throw new BusinessRuleException("Doctor is not available at the selected date/time.");
        }

        // Prevent double-booking the same doctor slot
        if (appointmentRepository.existsActiveBooking(doctor.getId(), dto.getAppointmentDate(), dto.getAppointmentTime())) {
            throw new BusinessRuleException("This time slot has just been booked. Please choose another.");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(dto.getAppointmentDate())
                .appointmentTime(dto.getAppointmentTime())
                .status(AppointmentStatus.PENDING)
                .reasonForVisit(dto.getReasonForVisit())
                .build();

        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByPatient(Long patientId) {
        return appointmentRepository.findByPatient_IdOrderByAppointmentDateDescAppointmentTimeDesc(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctor_IdOrderByAppointmentDateDescAppointmentTimeDesc(doctorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findByDoctorAndDate(Long doctorId, LocalDate date) {
        return appointmentRepository.findByDoctor_IdAndAppointmentDateOrderByAppointmentTimeAsc(doctorId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findAllByDate(LocalDate date) {
        return appointmentRepository.findAllByDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    @Override
    @Transactional
    public Appointment cancelByPatient(Long appointmentId, Long patientId, String reason) {
        Appointment appointment = findById(appointmentId);

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new AccessDeniedException("You may only cancel your own appointments.");
        }
        if (!ACTIVE_STATUSES.contains(appointment.getStatus())) {
            throw new BusinessRuleException("Only pending or confirmed appointments can be cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment updateStatus(Long appointmentId, Long doctorId, AppointmentStatus newStatus, String reason) {
        Appointment appointment = findById(appointmentId);

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new AccessDeniedException("You may only manage your own appointments.");
        }

        validateTransition(appointment.getStatus(), newStatus);

        appointment.setStatus(newStatus);
        if (newStatus == AppointmentStatus.REJECTED || newStatus == AppointmentStatus.CANCELLED) {
            appointment.setCancellationReason(reason);
        }
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment addConsultationNote(Long appointmentId, Long doctorId, ConsultationNoteDto dto) {
        Appointment appointment = findById(appointmentId);

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new AccessDeniedException("You may only add notes to your own appointments.");
        }
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessRuleException("Only confirmed appointments can be completed with consultation notes.");
        }

        ConsultationNote note = consultationNoteRepository.findByAppointment_Id(appointmentId)
                .orElse(ConsultationNote.builder().appointment(appointment).build());

        note.setDiagnosis(dto.getDiagnosis());
        note.setNotes(dto.getNotes());
        note.setPrescription(dto.getPrescription());
        note.setFollowUpAdvice(dto.getFollowUpAdvice());
        consultationNoteRepository.save(note);

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment adminUpdateStatus(Long appointmentId, AppointmentStatus newStatus) {
        Appointment appointment = findById(appointmentId);
        validateTransition(appointment.getStatus(), newStatus);
        appointment.setStatus(newStatus);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countToday() {
        return appointmentRepository.countByAppointmentDate(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countTodayByDoctorAndStatus(Long doctorId, AppointmentStatus status) {
        return appointmentRepository.countByDoctor_IdAndAppointmentDateAndStatus(doctorId, LocalDate.now(), status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTodayByDoctor(Long doctorId) {
        return appointmentRepository.countByDoctor_IdAndAppointmentDate(doctorId, LocalDate.now());
    }

    /**
     * Enforces a sane appointment status state-machine so, e.g., a COMPLETED
     * appointment can never be silently flipped back to PENDING.
     */
    private void validateTransition(AppointmentStatus from, AppointmentStatus to) {
        boolean allowed = switch (from) {
            case PENDING -> to == AppointmentStatus.CONFIRMED || to == AppointmentStatus.REJECTED
                    || to == AppointmentStatus.CANCELLED;
            case CONFIRMED -> to == AppointmentStatus.COMPLETED || to == AppointmentStatus.CANCELLED
                    || to == AppointmentStatus.NO_SHOW;
            case REJECTED, CANCELLED, COMPLETED, NO_SHOW -> false;
        };
        if (!allowed) {
            throw new BusinessRuleException("Cannot change appointment status from " + from + " to " + to + ".");
        }
    }
}
