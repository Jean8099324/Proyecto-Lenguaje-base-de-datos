document.addEventListener("DOMContentLoaded", function () {
    // Elementos del formulario
    const passwordInput = document.getElementById("password");
    const togglePassword = document.getElementById("togglePassword");

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
