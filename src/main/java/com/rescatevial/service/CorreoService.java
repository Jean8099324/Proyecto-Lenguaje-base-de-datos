package com.rescatevial.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {
    private final JavaMailSender mailSender;
    private final String remitente;
    private final String clave;

    public CorreoService(JavaMailSender mailSender,
                         @Value("${app.mail.from}") String remitente,
                         @Value("${spring.mail.password:}") String clave) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.clave = clave;
    }

    public void enviarCredenciales(String destinatario, String username, String password) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("Configure MAIL_PASSWORD con una clave de aplicación de Gmail.");
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Credenciales de acceso - Rescate Vial CR");
        mensaje.setText("""
                Hola,

                Se creó su usuario para Rescate Vial CR.

                Usuario: %s
                Contraseña temporal: %s

                Por seguridad, cambie esta contraseña después de iniciar sesión.
                """.formatted(username, password));
        mailSender.send(mensaje);
    }
}
