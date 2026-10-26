package com.rescatevial;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacyCompatiblePasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/img/**", "/fav/**",
                        "/webjars/**", "/favicon.ico")
                .permitAll()
                .requestMatchers("/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/dashboard").authenticated()
                .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false));
        return http.build();
    }

    /**
     * Los datos iniciales guardan SHA-256 sin prefijo; los registros nuevos usan BCrypt.
     * Este encoder permite autenticar ambos formatos durante la transición.
     */
    private static final class LegacyCompatiblePasswordEncoder implements PasswordEncoder {
        private static final String SHA256_HEX = "(?i)^[0-9a-f]{64}$";
        private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

        @Override
        public String encode(CharSequence rawPassword) {
            return bcrypt.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            if (encodedPassword == null) return false;
            if (encodedPassword.matches(SHA256_HEX)) {
                try {
                    byte[] digest = MessageDigest.getInstance("SHA-256")
                            .digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
                    String candidate = HexFormat.of().withUpperCase().formatHex(digest);
                    return MessageDigest.isEqual(candidate.getBytes(StandardCharsets.US_ASCII),
                            encodedPassword.toUpperCase().getBytes(StandardCharsets.US_ASCII));
                } catch (NoSuchAlgorithmException ex) {
                    throw new IllegalStateException("SHA-256 no está disponible.", ex);
                }
            }
            return bcrypt.matches(rawPassword, encodedPassword);
        }
    }
}
