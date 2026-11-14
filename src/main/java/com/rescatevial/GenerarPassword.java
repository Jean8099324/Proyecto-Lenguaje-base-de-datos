
package com.rescatevial;

import java.io.Console;
import java.util.Arrays;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarPassword {

    public static void main(String[] args) {

        Console consola = System.console();

        if (consola == null) {
            System.out.println("ERROR: Ejecuta este programa desde CMD.");
            return;
        }

        char[] password = consola.readPassword(
            "Escribe tu nueva contraseña: "
        );

        if (password == null || password.length == 0) {
            System.out.println("ERROR: La contraseña no puede estar vacía.");
            return;
        }

        try {
            BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

            String hash = encoder.encode(new String(password));

            System.out.println();
            System.out.println("HASH BCrypt:");
            System.out.println(hash);
            System.out.println();
            System.out.println("Hash generado correctamente.");

        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
