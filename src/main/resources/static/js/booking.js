// Dynamically loads a doctor's available time slots for a chosen date
// via the /patient/doctors/{id}/slots endpoint, and lets the user pick one.
document.addEventListener("DOMContentLoaded", function () {
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

  function loadSlots() {
    const doctorId = doctorSelect.value;
    const date = dateInput.value;
    slotsContainer.innerHTML = "";
    if (timeInput) timeInput.value = "";

    if (!doctorId || !date) {
      slotHint.textContent = "Select a doctor and date to see available slots.";
      return;
    }

    slotHint.textContent = "Loading available slots...";

    fetch(`/patient/doctors/${doctorId}/slots?date=${date}`)
      .then((res) => res.json())
      .then((slots) => {
        slotsContainer.innerHTML = "";
        if (!slots || slots.length === 0) {
          slotHint.textContent = "No available slots for this doctor on the selected date. Try another date.";
          return;
        }
        slotHint.textContent = "Click a slot to select it:";
        slots.forEach((slot) => {
          const btn = document.createElement("button");
          btn.type = "button";
          btn.className = "btn btn-outline-primary btn-sm slot-btn m-1";
          btn.textContent = slot;
          btn.addEventListener("click", function () {
            document.querySelectorAll(".slot-btn").forEach((b) => b.classList.remove("active", "btn-primary"));
            document.querySelectorAll(".slot-btn").forEach((b) => b.classList.add("btn-outline-primary"));
            btn.classList.remove("btn-outline-primary");
            btn.classList.add("active", "btn-primary");
            if (timeInput) timeInput.value = slot;
          });
          slotsContainer.appendChild(btn);
        });
      })
      .catch(() => {
        slotHint.textContent = "Could not load slots. Please try again.";
      });
  }

  doctorSelect.addEventListener("change", loadSlots);
  dateInput.addEventListener("change", loadSlots);

  // If both are pre-filled (e.g. editing), load immediately
  if (doctorSelect.value && dateInput.value) {
    loadSlots();
  }
});
