document.addEventListener("DOMContentLoaded", function () {

    const passwordInput = document.getElementById("password");
    const togglePassword = document.getElementById("togglePassword");
    const loginForm = document.getElementById("loginForm");

    // Mostrar / ocultar contraseña
    togglePassword.addEventListener("click", function () {

        const isPassword = passwordInput.type === "password";

        passwordInput.type = isPassword ? "text" : "password";

        const icon = togglePassword.querySelector("i");

        icon.classList.toggle("bi-eye", !isPassword);
        icon.classList.toggle("bi-eye-slash", isPassword);
    });

    // Navegación temporal mientras no existe autenticación real
    loginForm.addEventListener("submit", function (event) {
        event.preventDefault();

        const usuario = document.getElementById("usuario").value.trim();
        const password = passwordInput.value.trim();

        if (usuario === "" || password === "") {
            return;
        }

        // Posteriormente será reemplazado por Spring Security
        // y validación de usuarios.
        window.location.href = "/dashboard";
    });

});