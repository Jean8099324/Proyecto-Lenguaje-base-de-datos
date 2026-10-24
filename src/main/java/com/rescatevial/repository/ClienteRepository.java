package com.rescatevial.repository;

import com.rescatevial.domain.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClienteRepository {
    private static final String CLIENTE_SELECT = """
            SELECT u.ID_USUARIO, u.NOMBRE, u.PRIMER_APELLIDO, u.SEGUNDO_APELLIDO, u.USERNAME,
                   NVL((SELECT MIN(t.NUMERO_TELEFONO) FROM FIDE_TELEFONOS_TB t
                        WHERE t.ID_USUARIO = u.ID_USUARIO AND t.ID_ESTADO = 1), '—') AS TELEFONO,
                   NVL((SELECT MIN(c.CORREO) FROM FIDE_CORREOS_TB c
                        WHERE c.ID_USUARIO = u.ID_USUARIO AND c.ID_ESTADO = 1), '—') AS CORREO,
                   (SELECT COUNT(*) FROM FIDE_VEHICULOS_TB v
                    WHERE v.ID_USUARIO = u.ID_USUARIO AND v.ID_ESTADO = 1) AS VEHICULOS,
                   e.NOMBRE_ESTADO AS ESTADO
            FROM FIDE_USUARIOS_TB u
            JOIN FIDE_ESTADOS_TB e ON e.ID_ESTADO = u.ID_ESTADO
            WHERE u.ID_TIPO_USUARIO = 5
            """;

    private final JdbcTemplate jdbc;

    public ClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Cliente> findAll() {
        return jdbc.query(CLIENTE_SELECT + " ORDER BY u.ID_USUARIO DESC", (rs, row) -> new Cliente(
                rs.getLong("ID_USUARIO"), rs.getString("NOMBRE"), rs.getString("PRIMER_APELLIDO"),
                rs.getString("SEGUNDO_APELLIDO"), rs.getString("USERNAME"), rs.getString("TELEFONO"),
                rs.getString("CORREO"), rs.getInt("VEHICULOS"), rs.getString("ESTADO")));
    }

    public long countByState(long stateId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM FIDE_USUARIOS_TB WHERE ID_TIPO_USUARIO = 5 AND ID_ESTADO = ?",
                Long.class, stateId);
        return count == null ? 0 : count;
    }

    public long countVehicles() {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM FIDE_VEHICULOS_TB v
                JOIN FIDE_USUARIOS_TB u ON u.ID_USUARIO = v.ID_USUARIO
                WHERE u.ID_TIPO_USUARIO = 5 AND v.ID_ESTADO = 1
                """, Long.class);
        return count == null ? 0 : count;
    }

    public Optional<Cliente> findById(long id) {
        return jdbc.query(CLIENTE_SELECT + " AND u.ID_USUARIO = ?", (rs, row) -> new Cliente(
                rs.getLong("ID_USUARIO"), rs.getString("NOMBRE"), rs.getString("PRIMER_APELLIDO"),
                rs.getString("SEGUNDO_APELLIDO"), rs.getString("USERNAME"), rs.getString("TELEFONO"),
                rs.getString("CORREO"), rs.getInt("VEHICULOS"), rs.getString("ESTADO")), id)
                .stream().findFirst();
    }

    public long nextId() {
        Long id = jdbc.queryForObject("SELECT NVL(MAX(ID_USUARIO), 0) + 1 FROM FIDE_USUARIOS_TB", Long.class);
        return id == null ? 1 : id;
    }

    public boolean usernameExists(String username) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM FIDE_USUARIOS_TB WHERE LOWER(USERNAME) = LOWER(?)", Integer.class, username);
        return count != null && count > 0;
    }

    public void save(long id, Cliente form, String passwordHash) {
        jdbc.update("""
                INSERT INTO FIDE_USUARIOS_TB
                    (ID_USUARIO, NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, USERNAME, PASSWORD,
                     ID_TIPO_USUARIO, ID_ESTADO)
                VALUES (?, ?, ?, ?, ?, ?, 5, 1)
                """, id, form.getNombre().trim(), form.getPrimerApellido().trim(),
                emptyToNull(form.getSegundoApellido()), form.getUsername().trim(), passwordHash);
        jdbc.update("INSERT INTO FIDE_TELEFONOS_TB (ID_USUARIO, NUMERO_TELEFONO, ID_ESTADO) VALUES (?, ?, 1)",
                id, form.getTelefono().trim());
        jdbc.update("INSERT INTO FIDE_CORREOS_TB (ID_USUARIO, CORREO, ID_ESTADO) VALUES (?, ?, 1)",
                id, form.getCorreo().trim().toLowerCase());
    }

    public void updateState(long id, long stateId) {
        int updated = jdbc.update("UPDATE FIDE_USUARIOS_TB SET ID_ESTADO = ? WHERE ID_USUARIO = ? AND ID_TIPO_USUARIO = 5",
                stateId, id);
        if (updated == 0) throw new IllegalArgumentException("No se encontró el cliente.");
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
