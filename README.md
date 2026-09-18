# MediCare — Hospital Appointment & Patient Management System

A production-grade, full-stack Hospital Appointment and Patient Management System built with **Spring Boot 3.3.4**, **Spring Security 6**, **Spring Data JPA / Hibernate**, **Thymeleaf**, and **Bootstrap 5**.

---

## 🌟 Key Features

### 👤 Patient Portal
- **Self-Registration & Authentication**: Secure sign-up and login with BCrypt password hashing.
- **Doctor Directory**: Search doctors by name, medical specialization, or clinical department.
- **Dynamic Slot Booking**: Interactive AJAX-powered slot selector (`booking.js`) fetching real-time available consultation windows and excluding already-booked slots.
- **Appointment Management**: View status of upcoming/past appointments with real-time status badges and cancellation with reason.
- **Medical History**: Access comprehensive records of past completed consultations, including clinical diagnosis, doctor notes, prescriptions, and follow-up advice.
- **Patient Profile**: Update personal details, contact information, date of birth, blood group, allergies, and emergency contacts.

### 🩺 Doctor Portal
- **Physician Dashboard**: Today's appointment counts, pending requests, and schedule overview.
- **Consultation Management**: Review appointment requests, accept (`CONFIRMED`), reject (`REJECTED`) with reasons, or flag as `NO_SHOW`.
- **Clinical Notes & Prescriptions**: Record diagnosis, medications, observations, and follow-up instructions directly from the visit page. Completing notes automatically advances appointment status to `COMPLETED`.
- **Patient Roster**: Access patient history and previous consultation records.
- **Weekly Availability**: Configure recurring weekly working hours (e.g. Mondays 09:00–13:00) with customizable slot durations (15–60 mins).
- **Physician Profile**: Professional credential overview (specialization, experience, fees, qualifications, bio).

### 🛡️ Administrative Portal
- **Executive Dashboard**: Real-time metrics on doctors, registered patients, total consultations, and daily schedule oversight.
- **Department Management**: Full CRUD for hospital clinical departments (Cardiology, Orthopedics, Pediatrics, General Medicine, etc.).
- **Doctor Administration**: Register new physicians, edit qualifications, toggle active/inactive status, and configure consultation fees.
- **Patient Directory**: Searchable directory of all registered patients with medical profile inspects.
- **Global Appointment Oversight**: Filter all hospital appointments by date or status with administrative status override controls.

### 🌐 Public REST API
- Read-only JSON endpoints for external client integrations:
  - `GET /api/public/departments`
  - `GET /api/public/doctors`
  - `GET /api/public/doctors/{id}`
  - `GET /api/public/doctors/{id}/slots?date=YYYY-MM-DD`

---

## 🛠️ Tech Stack

- **Backend**: Java 17, Spring Boot 3.3.4, Spring MVC, Spring Data JPA, Spring Security 6, Hibernate, Bean Validation (Jakarta Validation), Lombok
- **Frontend**: Thymeleaf, Bootstrap 5.3.3, Bootstrap Icons 1.11.3, Vanilla JavaScript
- **Database**:
  - **Dev profile**: H2 in-memory database (zero installation, instant startup)
  - **MySQL / prod profiles**: MySQL 8.x with schema managed by **Flyway** migrations
- **Build Tool**: Apache Maven (includes Maven Wrapper `mvnw` / `mvnw.cmd`)
- **Deployment**: Multi-stage `Dockerfile` (Render/Railway/Fly.io/VPS-ready), `Procfile` for buildpack platforms, `/actuator/health` for platform health checks

---

## 🚀 Quickstart Guide

### Prerequisites
- **JDK 17** installed (e.g. Eclipse Temurin, Oracle JDK 17, Amazon Corretto 17). Ensure `JAVA_HOME` points to your JDK 17 directory.

### Running with In-Memory H2 (Zero Setup — Recommended for Testing)
The application defaults to the `dev` profile with pre-seeded demo accounts and departments:

```bash
# Windows (PowerShell / Command Prompt)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17" # Adjust to your JDK 17 path if needed
.\mvnw.cmd spring-boot:run

# Linux / macOS
export JAVA_HOME=/path/to/jdk-17
./mvnw spring-boot:run
```

Once started, open your browser at:
👉 **[http://localhost:8080](http://localhost:8080)**

H2 database console (optional):
👉 **[http://localhost:8080/h2-console](http://localhost:8080/h2-console)**
- JDBC URL: `jdbc:h2:mem:hospitaldb`
- Username: `sa`
- Password: *(empty)*

---

### Running with Production MySQL Database

1. Ensure MySQL server is running and create the database:
   ```sql
   CREATE DATABASE IF NOT EXISTS hospitaldb;
   ```
2. Run with the `mysql` profile and your database credentials:
   ```bash
   # Windows (PowerShell)
   $env:DB_HOST = "localhost"
   $env:DB_PORT = "3306"
   $env:DB_NAME = "hospitaldb"
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "yourpassword"
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=mysql

   # Linux / macOS
   export DB_HOST=localhost
   export DB_PORT=3306
   export DB_NAME=hospitaldb
   export DB_USERNAME=root
   export DB_PASSWORD=yourpassword
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

---

## 🚢 Deploying to production

The app ships with a `prod` Spring profile, a multi-stage `Dockerfile`, and Flyway-managed database migrations, so it can be deployed to any platform that runs a container or a Java process.

### What changes in `prod`

| Concern | dev / mysql (local) | prod |
|---|---|---|
| Database | H2 in-memory, or local MySQL | MySQL via `DATABASE_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars — no credentials in source |
| Schema | Hibernate auto-creates (`create-drop`/`update`) | **Flyway** runs versioned migrations (`src/main/resources/db/migration/V1__init.sql`) on startup; Hibernate only validates the mapping |
| Demo data seeding | On (`app.data.seed=true`) | **Off** (`app.data.seed=false`) — no demo accounts get created |
| H2 console | Enabled at `/h2-console` | Disabled outright, and blocked in `SecurityConfig` even if re-enabled by mistake |
| Thymeleaf template cache | Off (live reload) | On (faster, matches deployed jar) |
| Error responses | Full messages/stack traces | Generic — no internal details leaked to clients |
| Session cookies | Plain | `Secure` + `HttpOnly` (requires the app to sit behind HTTPS) |
| Health check | — | `GET /actuator/health` (public, no other actuator endpoints exposed) |

### Environment variables to set

| Variable | Required | Example | Notes |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ✅ | `prod` | Activates the production profile |
| `DATABASE_URL` | ✅ | `jdbc:mysql://host:3306/hospital_db?useSSL=true&serverTimezone=UTC` | Full JDBC URL to a real MySQL 8.x instance |
| `DB_USERNAME` | ✅ | `hms_app` | Use a dedicated app user, not `root` |
| `DB_PASSWORD` | ✅ | *(secret)* | Store as a platform secret, never commit it |
| `PORT` | usually auto-set | `8080` | Most platforms (Render, Railway, Fly.io) inject this automatically |
| `DB_POOL_SIZE` | optional | `10` | HikariCP max pool size, defaults to 10 |

On first deploy, Flyway will create the schema for you from `V1__init.sql` — you don't need to run any SQL by hand, just make sure the database itself exists and the credentials above can connect to it.

### Option A — Deploy with Docker (recommended)

Works on Render, Railway, Fly.io, a plain VPS, or any container host.

```bash
docker build -t hms .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL="jdbc:mysql://your-db-host:3306/hospital_db?useSSL=true&serverTimezone=UTC" \
  -e DB_USERNAME=hms_app \
  -e DB_PASSWORD=yourpassword \
  hms
```

For Render/Railway/Fly.io: connect the repo, let the platform detect the `Dockerfile`, and set the four environment variables above in the platform's dashboard. No other configuration is needed — `PORT` is provided by the platform and the app already reads it.

### Option B — Deploy without Docker (buildpack platforms)

A `Procfile` is included for platforms that build directly from source (e.g. Heroku-style buildpacks):

```
web: java -jar target/hms-1.0.0.jar
```

Build first with `./mvnw clean package -DskipTests`, then set the same environment variables as above plus `SPRING_PROFILES_ACTIVE=prod`.

### Option C — Plain VPS / systemd

```bash
git clone <your-repo-url>
cd Hospital-management
./mvnw clean package -DskipTests

export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL="jdbc:mysql://localhost:3306/hospital_db?useSSL=true&serverTimezone=UTC"
export DB_USERNAME=hms_app
export DB_PASSWORD=yourpassword

java -jar target/hms-1.0.0.jar
```

Put this behind Nginx or Caddy as a reverse proxy for HTTPS termination (required — the prod profile marks cookies `Secure`, so the app must be served over HTTPS or logins will silently fail to persist).

### Before you go live — a short checklist

- [ ] Create a dedicated MySQL user for the app with only the privileges it needs (not `root`)
- [ ] Put the app behind HTTPS (a platform load balancer, or Nginx/Caddy on a VPS) — required for secure cookies to work
- [ ] Confirm `GET /actuator/health` returns `{"status":"UP"}` after deploy as a smoke test
- [ ] If you add or change an entity later, add a new `V2__..._description.sql` file in `src/main/resources/db/migration/` rather than editing `V1__init.sql` — Flyway checksums applied migrations and will refuse to start if an already-run file changes

---

## 🔐 Seeded Demo Credentials

When launched in `dev` mode (or when `app.data.seed=true`), the system automatically seeds ready-to-use demo accounts:

| Role | Email / Username | Password | Notes |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin@hospital.com` | `admin123` | Full administrative control |
| **Doctor (Cardiology)** | `dr.kumar@hospital.com` | `doctor123` | Dr. Ramesh Kumar |
| **Doctor (Orthopedics)** | `dr.arun@hospital.com` | `doctor123` | Dr. Arun Nair |
| **Doctor (General Med)** | `dr.priya@hospital.com` | `doctor123` | Dr. Priya Sharma |
| **Doctor (Pediatrics)** | `dr.meena@hospital.com` | `doctor123` | Dr. Meena Iyer |
| **Patient** | Register on `/auth/register` | Self-chosen | Create any new patient account |

---

## 📁 Project Architecture

```
Hospital-management/
├── src/main/java/com/hospital/hms/
│   ├── config/            # SecurityConfig, DataSeeder, PasswordEncoder
│   ├── controller/        # HomeController, PatientController, DoctorController, AdminController
│   │   └── api/           # PublicApiController (REST endpoints)
│   ├── dto/               # Form backing objects & Bean Validation DTOs
│   ├── entity/            # User, Patient, Doctor, Department, Appointment, ConsultationNote, DoctorAvailability
│   ├── enums/             # Role, AppointmentStatus
│   ├── exception/         # GlobalMvcExceptionHandler, GlobalRestExceptionHandler
│   ├── repository/        # Spring Data JPA repositories
│   ├── security/          # CustomUserDetailsService, UserPrincipal, SecurityUtil
│   ├── service/           # Service interfaces & implementations (business logic, slot engine)
│   └── util/              # Utility helpers
├── src/main/resources/
│   ├── static/
│   │   ├── css/style.css  # Healthcare design system & status badges
│   │   └── js/booking.js  # AJAX dynamic slot engine
│   ├── templates/
│   │   ├── admin/         # 7 Admin views (dashboard, departments, doctors, doctor-form, patients, patient-detail, appointments)
│   │   ├── auth/          # Login & Register views
│   │   ├── doctor/        # 6 Doctor views (dashboard, appointments, appointment-detail, patients, availability, profile)
│   │   ├── error/         # 403, 404, 500 error templates
│   │   ├── fragments/     # head, navbar (role-aware), alerts
│   │   ├── patient/       # 7 Patient views (dashboard, doctors, doctor-details, book-appointment, appointments, history, profile)
│   │   └── home.html      # Public landing page
│   ├── db/migration/      # Flyway versioned SQL migrations (V1__init.sql, ...)
│   ├── application.properties
│   ├── application-dev.properties
│   ├── application-mysql.properties
│   └── application-prod.properties  # env-var driven config for production deploys
├── mvnw & mvnw.cmd        # Maven wrapper scripts
├── pom.xml                # Maven project definition
├── Dockerfile             # Multi-stage build for containerized deploys
├── .dockerignore
├── .gitignore
├── Procfile               # For buildpack-based platforms (no Docker)
└── STATUS_REPORT.md       # Development history and status
```

---

## 🧪 Testing the Complete Clinical Workflow

1. **Patient Registration & Booking**:
   - Navigate to `/auth/register` and register as a new patient.
   - Go to **Find a Doctor** (`/patient/doctors`), pick Dr. Ramesh Kumar (`/patient/doctors/1`).
   - Select tomorrow's date, choose an available time slot, and submit the booking request.
2. **Doctor Approval**:
   - Log out, then log in as `dr.kumar@hospital.com` / `doctor123`.
   - On the **Doctor Dashboard**, click into the appointment request.
   - Click **Accept & Confirm**.
3. **Doctor Consultation & Diagnosis**:
   - Fill in the Consultation & Clinical Notes (Diagnosis, Prescription, Observations) and submit.
   - Notice the appointment automatically transitions to `COMPLETED`.
4. **Patient Medical History**:
   - Log out and log back in as your Patient.
   - Click **Medical History** (`/patient/history`) to review the saved diagnosis, prescriptions, and advice.
5. **Administrative Oversight**:
   - Log in as `admin@hospital.com` / `admin123`.
   - Inspect the live metrics on the Admin Dashboard, modify hospital departments, or search patient profiles.
