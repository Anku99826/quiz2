// Generate a colorful, rotated alphanumeric CAPTCHA
function generateCaptcha() {
  const chars = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
  const captchaDisplay = document.getElementById('captchaCode');
  captchaDisplay.innerHTML = ''; // clear old one

  let code = '';
  for (let i = 0; i < 5; i++) {
    const char = chars.charAt(Math.floor(Math.random() * chars.length));
    code += char;

    // random color and rotation for each char
    const color = `hsl(${Math.floor(Math.random() * 360)}, 80%, 40%)`;
    const rotation = Math.floor(Math.random() * 31) - 15; // -15° to +15°

    const span = document.createElement('span');
    span.textContent = char;
    span.classList.add('captcha-char');
    span.style.color = color;
    span.style.transform = `rotate(${rotation}deg)`;

    captchaDisplay.appendChild(span);
  }

  return code;
}

let currentCaptcha = null;

document.addEventListener('DOMContentLoaded', function () {
  const refreshButton = document.getElementById('refreshCaptcha');
  const loginForm = document.getElementById('loginForm');
  const captchaInput = document.getElementById('captchaInput');
  const captchaError = document.getElementById('captchaError');

  // Initial load
  currentCaptcha = generateCaptcha();

  // Refresh on click
  refreshButton.addEventListener('click', function () {
    currentCaptcha = generateCaptcha();
    captchaError.textContent = ''; // clear error
  });

  // Validate before form submit
  loginForm.addEventListener('submit', function (e) {
    captchaError.textContent = ''; // clear old error
    if (captchaInput.value.trim() !== currentCaptcha) {
      e.preventDefault();
      captchaError.textContent = '❌ Invalid CAPTCHA. Please try again.';
      captchaInput.value = '';
      currentCaptcha = generateCaptcha();
    }
  });
});
