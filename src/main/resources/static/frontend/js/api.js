/**
 * MediCare HMS - REST API Client
 * Supports session cookies (credentials: include) and JSON request/response handling.
 */

const API_BASE = window.API_BASE || '';

const api = {
    async request(url, options = {}) {
        const fullUrl = url.startsWith('http') ? url : API_BASE + url;
        const defaultHeaders = {
            'Accept': 'application/json'
        };

        if (options.body && !(options.body instanceof FormData) && !(options.body instanceof URLSearchParams)) {
            defaultHeaders['Content-Type'] = 'application/json';
        }

        const config = {
            credentials: 'include',
            ...options,
            headers: {
                ...defaultHeaders,
                ...(options.headers || {})
            }
        };

        try {
            const response = await fetch(fullUrl, config);

            // If session expired or unauthorized on protected routes
            if (response.status === 401) {
                const currentPath = window.location.pathname;
                const isAuthPage = currentPath.endsWith('login.html') || currentPath.endsWith('register.html') || currentPath.endsWith('index.html') || currentPath === '/' || currentPath.endsWith('/frontend/');
                if (!isAuthPage) {
                    sessionStorage.removeItem('hms_user');
                    window.location.href = '/frontend/login.html?expired=true';
                    return null;
                }
            }

            const contentType = response.headers.get('content-type');
            let data = null;
            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                const text = await response.text();
                data = text ? { text } : {};
            }

            if (!response.ok) {
                const errorMessage = (data && (data.message || data.error)) || `HTTP Error ${response.status}: ${response.statusText}`;
                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = data;
                throw error;
            }

            return data;
        } catch (err) {
            console.error('API Request failed:', err);
            throw err;
        }
    },

    get(url, options = {}) {
        return this.request(url, { method: 'GET', ...options });
    },

    post(url, data, options = {}) {
        const body = (data instanceof FormData || data instanceof URLSearchParams) ? data : JSON.stringify(data);
        return this.request(url, { method: 'POST', body, ...options });
    },

    put(url, data, options = {}) {
        const body = (data instanceof FormData || data instanceof URLSearchParams) ? data : JSON.stringify(data);
        return this.request(url, { method: 'PUT', body, ...options });
    },

    delete(url, options = {}) {
        return this.request(url, { method: 'DELETE', ...options });
    }
};

/**
 * Helper to display toast/alert banner
 */
function showAlert(type, message, containerId = 'alert-container') {
    const container = document.getElementById(containerId);
    if (!container) return;

    const alertEl = document.createElement('div');
    alertEl.className = `alert alert-${type} alert-dismissible fade show shadow-sm`;
    alertEl.role = 'alert';
    alertEl.innerHTML = `
        <span>${message}</span>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    container.innerHTML = '';
    container.appendChild(alertEl);

    setTimeout(() => {
        if (alertEl && alertEl.parentNode) {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alertEl);
            if (bsAlert) bsAlert.close();
        }
    }, 6000);
}

// Global exposure
window.api = api;
window.showAlert = showAlert;
