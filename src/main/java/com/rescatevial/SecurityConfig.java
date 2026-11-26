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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import com.rescatevial.config.RequestTimingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacyCompatiblePasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RequestTimingFilter requestTimingFilter) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login", "/error", "/api/certificate-status", "/api/deployment-info", "/css/**", "/js/**", "/img/**", "/fav/**",
                        "/webjars/**", "/favicon.ico", "/error/**")
                .permitAll()
                .requestMatchers("/incidentes/clientes/asociar/**").hasRole("ADMIN")
                .requestMatchers("/acerca-de").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/cliente/**").hasAnyRole("ADMIN", "CLIENT")
                .requestMatchers("/incidentes/clientes/**", "/incidentes/clientes").hasRole("ADMIN")
                .requestMatchers("/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/clientes/**").hasRole("ADMIN")
                .requestMatchers("/vehiculos/**").hasRole("ADMIN")
                .requestMatchers("/personal/**").hasRole("ADMIN")
                .requestMatchers("/contactos/**").hasRole("ADMIN")
                .requestMatchers("/datos-contacto/**").hasRole("ADMIN")
                .requestMatchers("/configuracion/**").hasRole("ADMIN")
                .requestMatchers("/dashboard",
                        "/incidentes/**",
                        "/despacho/**",
                        "/detalle-reparaciones/**",
                        "/facturas/**",
                        "/reparaciones/**",
                        "/repuestos/**",
                        "/unidades/**").hasRole("ADMIN")
                .requestMatchers("/menu/estado").authenticated()
                .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureUrl("/login?error=true")
                        .permitAll())
                .addFilterBefore(requestTimingFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/error/403"))
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false));
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean administrador = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            boolean cliente = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_CLIENT".equals(authority.getAuthority()));
            if (administrador) {
                response.sendRedirect("/dashboard");
            } else if (cliente) {
                response.sendRedirect("/cliente/inicio");
            } else {
                response.sendRedirect("/error/403");
            }
        };
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
