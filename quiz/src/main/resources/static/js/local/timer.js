
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

