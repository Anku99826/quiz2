
/* =========================================================
   TIMER + AUTO SUBMIT LOGIC
   ========================================================= */
document.addEventListener("DOMContentLoaded", function () {

    const timerEl = document.getElementById("timer");
    const examForm = document.getElementById("examForm");

    // `time` must be provided from backend (in seconds)
    if (!timerEl || typeof time === "undefined" || !examForm) return;

    const interval = setInterval(() => {

        if (time <= 0) {
            clearInterval(interval);

            // Do NOT force any answer selection
            const radios = examForm.querySelectorAll('input[name="answer"]');
            // unanswered stays unanswered

            // Mark as final submit
            const actionInput = document.createElement('input');
            actionInput.type = 'hidden';
            actionInput.name = 'action';
            actionInput.value = 'submit';
            examForm.appendChild(actionInput);

            examForm.submit();
            return;
        }

        const min = Math.floor(time / 60);
        const sec = time % 60;

        timerEl.innerText = min + ":" + (sec < 10 ? "0" + sec : sec);
        time--;

    }, 1000);
});


/* =========================================================
   DISABLE RIGHT CLICK
   ========================================================= */
document.addEventListener("contextmenu", function (e) {
    e.preventDefault();
});


/* =========================================================
   COMPLETELY DISABLE KEYBOARD
   ========================================================= */
document.addEventListener("keydown", function (e) {
    e.preventDefault();
});



/* =========================================================
DEV TOOLS DETECTION
*/

let devtoolsWarnings = 0;
const maxWarnings = 2;
let alertActive = false;



// Function to detect DevTools
let devtoolsAlertsClosed = 0;
const maxAlerts = 30;

// Function to detect DevTools and show continuous alerts
function detectDevTools() {
    const threshold = 160; // difference threshold
    const widthDiff = window.outerWidth - window.innerWidth > threshold;
    const heightDiff = window.outerHeight - window.innerHeight > threshold;

    if ((widthDiff || heightDiff) && devtoolsAlertsClosed < maxAlerts) {
        // Show alert recursively
        showAlert();
    }
}

// Recursive alert function
function showAlert() {
    if (devtoolsAlertsClosed >= maxAlerts) return;

    alert(`Warning: Developer tools are not allowed during the quiz!`);
    devtoolsAlertsClosed++;

    // After user closes alert, check if DevTools still open
    const threshold = 160;
    const widthDiff = window.outerWidth - window.innerWidth > threshold;
    const heightDiff = window.outerHeight - window.innerHeight > threshold;

    if ((widthDiff || heightDiff) && devtoolsAlertsClosed < maxAlerts) {
        // Call again immediately
        showAlert();
    }
}

// Check every 500ms
setInterval(detectDevTools, 500);

// Optional: block common DevTools shortcuts
document.addEventListener("keydown", function(e) {
    if (
        e.key === "F12" ||
        (e.ctrlKey && e.shiftKey && ["I","J","C"].includes(e.key)) ||
        (e.ctrlKey && e.key === "U")
    ) {
        e.preventDefault();
        alert("This action is disabled during the quiz.");
    }
});