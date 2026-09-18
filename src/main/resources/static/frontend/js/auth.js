/**
 * MediCare HMS - Authentication & Navigation Service
 */

const auth = {
    getUser() {
        const stored = sessionStorage.getItem('hms_user');
        return stored ? JSON.parse(stored) : null;
    },

    setUser(user) {
        if (user) {
            sessionStorage.setItem('hms_user', JSON.stringify(user));
        } else {
            sessionStorage.removeItem('hms_user');
        }
    },

    async checkAuth() {
        try {
            const data = await api.get('/api/auth/me');
            if (data && data.email) {
                this.setUser(data);
                return data;
            }
            this.setUser(null);
            return null;
        } catch (err) {
            this.setUser(null);
            return null;
        }
    },

    async requireAuth(expectedRole) {
        const user = await this.checkAuth();
        if (!user) {
            window.location.href = '/login.html';
            return null;
        }

        const norm = (r) => (r && r.startsWith('ROLE_') ? r : 'ROLE_' + (r || ''));
        if (expectedRole && norm(user.role) !== norm(expectedRole)) {
            const r = norm(user.role);
            if (r === 'ROLE_ADMIN') {
                window.location.href = '/admin/dashboard.html';
            } else if (r === 'ROLE_DOCTOR') {
                window.location.href = '/doctor/dashboard.html';
            } else {
                window.location.href = '/patient/dashboard.html';
            }
            return null;
        }

        return user;
    },

    async login(email, password) {
        const params = new URLSearchParams();
        params.append('username', email);
        params.append('password', password);

        const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : (window.API_BASE || '');
        const response = await fetch(baseUrl + '/auth/login', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: params
        });

        const contentType = response.headers.get('content-type');
        let data = {};
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        }

        if (response.ok && data.success) {
            this.setUser({
                name: data.name,
                role: data.role
            });
            return data;
        } else {
            throw new Error(data.message || 'Invalid email or password');
        }
    },

    async register(patientData) {
        return await api.post('/api/auth/register', patientData);
    },

    async logout() {
        try {
            await api.post('/api/auth/logout', {});
        } catch (e) {
            // ignore error
        }
        this.setUser(null);
        window.location.href = '/login.html?logout=true';
    }
};

/**
 * Dynamically injects the responsive navbar matching the user's role
 */
function renderNavbar(activeNav = '') {
    const navContainer = document.getElementById('navbar-container');
    if (!navContainer) return;

    const norm = (r) => (r && r.startsWith('ROLE_') ? r : 'ROLE_' + (r || ''));
    const user = auth.getUser();
    const role = user ? norm(user.role) : null;
    const userName = user ? (user.name || user.email || 'User') : '';

    let navLinks = '';

    if (!user) {
        // Guest links
        navLinks = `
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'home' ? 'active fw-bold' : ''}" href="/index.html">Home</a>
            </li>
        `;
    } else if (role === 'ROLE_PATIENT') {
        navLinks = `
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'dashboard' ? 'active fw-bold' : ''}" href="/patient/dashboard.html">Dashboard</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'doctors' ? 'active fw-bold' : ''}" href="/patient/doctors.html">Find a Doctor</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'appointments' ? 'active fw-bold' : ''}" href="/patient/appointments.html">My Appointments</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'history' ? 'active fw-bold' : ''}" href="/patient/history.html">Medical History</a>
            </li>
        `;
    } else if (role === 'ROLE_DOCTOR') {
        navLinks = `
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'dashboard' ? 'active fw-bold' : ''}" href="/doctor/dashboard.html">Dashboard</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'appointments' ? 'active fw-bold' : ''}" href="/doctor/appointments.html">Appointments</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'patients' ? 'active fw-bold' : ''}" href="/doctor/patients.html">Patients</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'availability' ? 'active fw-bold' : ''}" href="/doctor/availability.html">Availability</a>
            </li>
        `;
    } else if (role === 'ROLE_ADMIN') {
        navLinks = `
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'dashboard' ? 'active fw-bold' : ''}" href="/admin/dashboard.html">Dashboard</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'doctors' ? 'active fw-bold' : ''}" href="/admin/doctors.html">Doctors</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'patients' ? 'active fw-bold' : ''}" href="/admin/patients.html">Patients</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'departments' ? 'active fw-bold' : ''}" href="/admin/departments.html">Departments</a>
            </li>
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'appointments' ? 'active fw-bold' : ''}" href="/admin/appointments.html">Appointments</a>
            </li>
        `;
    }

    let authSection = '';
    if (!user) {
        authSection = `
            <li class="nav-item">
                <a class="nav-link ${activeNav === 'login' ? 'active fw-bold' : ''}" href="/login.html">Login</a>
            </li>
            <li class="nav-item">
                <a class="btn btn-light btn-sm ms-lg-2 px-3 fw-medium text-dark shadow-sm" href="/register.html">Sign Up</a>
            </li>
        `;
    } else {
        const profileUrl = role === 'ROLE_PATIENT' ? '/patient/profile.html' :
                           role === 'ROLE_DOCTOR' ? '/doctor/profile.html' : '#';
        const profileItem = (role === 'ROLE_PATIENT' || role === 'ROLE_DOCTOR') ? `
            <li><a class="dropdown-item" href="${profileUrl}"><i class="bi bi-person me-2"></i>My Profile</a></li>
            <li><hr class="dropdown-divider"></li>
        ` : '';

        authSection = `
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle d-flex align-items-center gap-1" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="bi bi-person-circle fs-5"></i>
                    <span>${userName}</span>
                </a>
                <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0">
                    ${profileItem}
                    <li>
                        <button class="dropdown-item text-danger d-flex align-items-center" onclick="auth.logout()">
                            <i class="bi bi-box-arrow-right me-2"></i> Logout
                        </button>
                    </li>
                </ul>
            </li>
        `;
    }

    navContainer.innerHTML = `
        <nav class="navbar navbar-expand-lg navbar-dark app-navbar sticky-top shadow-sm">
            <div class="container-fluid px-4">
                <a class="navbar-brand fw-bold d-flex align-items-center gap-2" href="/index.html">
                    <i class="bi bi-heart-pulse-fill fs-4 text-warning"></i>
                    <span>MediCare HMS</span>
                </a>
                <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navMain">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="navMain">
                    <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                        ${navLinks}
                    </ul>
                    <ul class="navbar-nav ms-auto align-items-lg-center">
                        ${authSection}
                    </ul>
                </div>
            </div>
        </nav>
    `;
}

function renderFooter() {
    const footerContainer = document.getElementById('footer-container');
    if (!footerContainer) return;
    footerContainer.innerHTML = `
        <footer class="text-white py-4 mt-5">
            <div class="container text-center">
                <div class="d-flex justify-content-center align-items-center gap-2 mb-2">
                    <i class="bi bi-heart-pulse-fill text-warning"></i>
                    <span class="fw-bold fs-5">MediCare Hospital Management System</span>
                </div>
                <p class="text-white-50 small mb-2">Providing high quality healthcare management solutions with secure REST architecture.</p>
                <p class="text-white-50 small mb-0">&copy; ${new Date().getFullYear()} MediCare HMS. All rights reserved.</p>
            </div>
        </footer>
    `;
}

window.auth = auth;
window.renderNavbar = renderNavbar;
window.renderFooter = renderFooter;
