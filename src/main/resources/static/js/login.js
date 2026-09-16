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

    // Evitar que el formulario se envíe por ahora.
    // La autenticación real se agregará posteriormente.
    loginForm.addEventListener("submit", function (event) {
        event.preventDefault();

        const usuario = document.getElementById("usuario").value.trim();
        const password = passwordInput.value.trim();

        if (usuario === "" || password === "") {
            return;
        }

        console.log("Formulario listo para futura autenticación.");
    });

});