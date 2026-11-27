document.addEventListener("DOMContentLoaded", function () {
    // Elementos del formulario
    const passwordInput = document.getElementById("password");
    const togglePassword = document.getElementById("togglePassword");
    const loginForm = document.getElementById("loginForm");

    // ==========================================
    // MOSTRAR / OCULTAR CONTRASEÑA
    // ==========================================

    if (passwordInput && togglePassword) {
        togglePassword.addEventListener("click", function () {
            const isPassword = passwordInput.type === "password";

            passwordInput.type = isPassword ? "text" : "password";

            const icon = togglePassword.querySelector("i");

            if (icon) {
                icon.classList.toggle("bi-eye", !isPassword);
                icon.classList.toggle("bi-eye-slash", isPassword);
            }
        });
    }

    // Evita que un doble toque o Enter repetido envíe dos autenticaciones
    // concurrentes con el mismo identificador de sesión.
    if (loginForm) {
        loginForm.addEventListener("submit", function (event) {
            if (loginForm.dataset.submitting === "true") {
                event.preventDefault();
                return;
            }

            loginForm.dataset.submitting = "true";
            const submitButton = loginForm.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
                submitButton.setAttribute("aria-busy", "true");
                const label = submitButton.querySelector("span");
                if (label) label.textContent = "Ingresando...";
            }
        });
    }

    // ==========================================
    // AUTENTICACIÓN
    // ==========================================

    // El formulario se envía directamente a Spring Security.
    // Spring Security verifica el usuario y la contraseña
    // utilizando los registros almacenados en Oracle.
    //
    // No se utiliza preventDefault().
    // No se redirige manualmente al Dashboard.
});
