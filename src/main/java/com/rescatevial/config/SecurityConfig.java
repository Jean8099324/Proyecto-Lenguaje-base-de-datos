package com.rescatevial.config;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.rescatevial.repository.UsuarioRepository;

@Configuration
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public UserDetailsService userDetailsService(
                        UsuarioRepository usuarioRepository) {

                return username -> {

                        List<Map<String, Object>> usuarios = usuarioRepository.findUsuarioParaLogin(username);

                        if (usuarios.isEmpty()) {
                                throw new UsernameNotFoundException(
                                                "Usuario no encontrado o inactivo");
                        }

                        Map<String, Object> usuario = usuarios.get(0);

                        String nombreUsuario = usuario.get("USERNAME").toString();

                        String passwordHash = usuario.get("PASSWORD").toString();

                        return User.withUsername(nombreUsuario)
                                        .password(passwordHash)
                                        .roles("USUARIO")
                                        .build();
                };
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http
                                .authorizeHttpRequests(auth -> auth

                                                // Pantalla de login y recursos públicos
                                                .requestMatchers(
                                                                "/",
                                                                "/login",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**",
                                                                "/favicon.ico")
                                                .permitAll()

                                                // Las demás rutas requieren autenticación
                                                .anyRequest().authenticated())

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .failureUrl("/login?error")
                                                .permitAll())

                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll());

                return http.build();
        }
}
