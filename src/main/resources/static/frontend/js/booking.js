/**
 * MediCare HMS - Dynamic Slot Picker for Appointment Booking
 */

function initBookingSlots() {
    const doctorSelect = document.getElementById("doctorId");
    const dateInput = document.getElementById("appointmentDate");
    const slotsContainer = document.getElementById("slotsContainer");
    const timeInput = document.getElementById("appointmentTime");
    const slotHint = document.getElementById("slotHint");

    if (!doctorSelect || !dateInput || !slotsContainer) {
        return;
    }

    // Prevent picking a past date
    const today = new Date().toISOString().split("T")[0];
    dateInput.setAttribute("min", today);

    async function loadSlots() {
        const doctorId = doctorSelect.value;
        const date = dateInput.value;
        slotsContainer.innerHTML = "";
        if (timeInput) timeInput.value = "";

        if (!doctorId || !date) {
            if (slotHint) slotHint.textContent = "Select a doctor and date to view available slots.";
            return;
        }

        if (slotHint) slotHint.textContent = "Loading available slots...";

        try {
            const slots = await api.get(`/api/patient/doctors/${doctorId}/slots?date=${date}`);
            slotsContainer.innerHTML = "";

            if (!slots || slots.length === 0) {
                if (slotHint) slotHint.textContent = "No available slots for this doctor on the selected date. Please pick another date.";
                return;
            }

            if (slotHint) slotHint.textContent = "Click an available slot to select:";
            slots.forEach((slot) => {
                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "btn btn-outline-primary btn-sm slot-btn m-1";
                btn.textContent = slot;
                btn.addEventListener("click", function () {
                    document.querySelectorAll(".slot-btn").forEach((b) => {
                        b.classList.remove("active", "btn-primary");
                        b.classList.add("btn-outline-primary");
                    });
                    btn.classList.remove("btn-outline-primary");
                    btn.classList.add("active", "btn-primary");
                    if (timeInput) timeInput.value = slot;
                });
                slotsContainer.appendChild(btn);
            });
        } catch (err) {
            if (slotHint) slotHint.textContent = "Could not load slots. Doctor may not be scheduled on this day.";
        }
    }

    doctorSelect.addEventListener("change", loadSlots);
    dateInput.addEventListener("change", loadSlots);

    if (doctorSelect.value && dateInput.value) {
        loadSlots();
    }
}

window.initBookingSlots = initBookingSlots;
