-- V1__init.sql
-- Initial schema for the Hospital Management System, hand-written to match
-- the JPA entities in com.hospital.hms.entity as of this migration.
--
-- Column names follow Hibernate's default CamelCase -> snake_case mapping
-- (Spring Boot's default physical naming strategy), so no @Column(name=...)
-- overrides are needed on the entities for this to line up.
--
-- IMPORTANT: if you add/rename a field on an entity, add a new V2__..., V3__...
-- migration alongside it rather than editing this file — Flyway checksums
-- applied migrations and will refuse to start if V1 changes after being run.

-- ===== users =====
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(255),
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== departments =====
CREATE TABLE departments (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(255),
    CONSTRAINT uk_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== patients =====
CREATE TABLE patients (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                  BIGINT NOT NULL,
    date_of_birth            DATE,
    gender                   VARCHAR(20),
    address                  VARCHAR(255),
    blood_group              VARCHAR(255),
    emergency_contact_name   VARCHAR(255),
    emergency_contact_phone  VARCHAR(255),
    allergies                VARCHAR(2000),
    CONSTRAINT uk_patients_user_id UNIQUE (user_id),
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== doctors =====
CREATE TABLE doctors (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    department_id       BIGINT,
    specialization      VARCHAR(255) NOT NULL,
    qualification       VARCHAR(255),
    experience_years    INT,
    consultation_fee    DECIMAL(19,2) NOT NULL DEFAULT 0,
    bio                 VARCHAR(1000),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_doctors_user_id UNIQUE (user_id),
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_doctors_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== doctor_availabilities =====
CREATE TABLE doctor_availabilities (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id              BIGINT NOT NULL,
    day_of_week            VARCHAR(20) NOT NULL,
    start_time             TIME NOT NULL,
    end_time               TIME NOT NULL,
    slot_duration_minutes  INT NOT NULL DEFAULT 30,
    CONSTRAINT fk_availability_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_availability_doctor_day ON doctor_availabilities(doctor_id, day_of_week);

-- ===== appointments =====
CREATE TABLE appointments (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id            BIGINT NOT NULL,
    doctor_id             BIGINT NOT NULL,
    appointment_date      DATE NOT NULL,
    appointment_time      TIME NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason_for_visit      VARCHAR(1000),
    cancellation_reason   VARCHAR(1000),
    created_at            DATETIME NOT NULL,
    updated_at            DATETIME NOT NULL,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_appointments_doctor_date ON appointments(doctor_id, appointment_date);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_status ON appointments(status);

-- ===== consultation_notes =====
CREATE TABLE consultation_notes (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id     BIGINT NOT NULL,
    diagnosis          VARCHAR(2000),
    notes              VARCHAR(3000),
    prescription       VARCHAR(3000),
    follow_up_advice   VARCHAR(255),
    created_at         DATETIME NOT NULL,
    updated_at         DATETIME NOT NULL,
    CONSTRAINT uk_consultation_notes_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_consultation_notes_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
