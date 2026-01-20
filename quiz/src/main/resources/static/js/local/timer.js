document.addEventListener("DOMContentLoaded", function () {

    const timerEl = document.getElementById("timer");
    const examForm = document.getElementById("examForm");

    if (!timerEl || typeof time === "undefined" || !examForm) return;

    const interval = setInterval(() => {

        if (time <= 0) {
            clearInterval(interval);

            // Automatically select no answer if nothing is selected
            const radios = examForm.querySelectorAll('input[name="answer"]');
            let anyChecked = Array.from(radios).some(r => r.checked);
            if (!anyChecked && radios.length) {
                radios[0].checked = true; // optional: select first option
            }

            // Set action=submit for auto-submit
            const actionInput = document.createElement('input');
            actionInput.type = 'hidden';
            actionInput.name = 'action';
            actionInput.value = 'submit';
            examForm.appendChild(actionInput);

            // Submit the form
            examForm.submit();

            return;
        }

        let min = Math.floor(time / 60);
        let sec = time % 60;

        timerEl.innerText =
            min + ":" + (sec < 10 ? "0" + sec : sec);

        time--;
    }, 1000);
});



// ===============================
// Disable Right Click
// ===============================
document.addEventListener('contextmenu', function (e) {
    e.preventDefault();
});

// ===============================
// Disable Keyboard Shortcuts
// ===============================
document.addEventListener('keydown', function (e) {

    // F12
    if (e.key === "F12") {
        e.preventDefault();
    }

    // Ctrl + key combinations
    if (e.ctrlKey && (
		
        e.key === 'u' || // View source
        e.key === 's' || // Save
        e.key === 'c' || // Copy
        e.key === 'v' || // Paste
        e.key === 'x' || // Cut
        e.key === 'a' || // Select all
        e.key === 'i' || // Dev tools
        e.key === 'j' ||
		e.key ==='e'  ||
		e.key ==='d'  ||
		e.key === 'r' ||
		e.key === 'q'
    )) {
        e.preventDefault();
    }

    // Ctrl + Shift combinations
    if (e.ctrlKey && e.shiftKey && (
        e.key === 'I' ||
        e.key === 'J' ||
        e.key === 'C'
    )) {
        e.preventDefault();
    }

    // Optional: block Alt / Esc / Tab
    if (e.key === 'Alt' || e.key === 'Escape' || e.key === 'Tab') {
        e.preventDefault();
    }
});

