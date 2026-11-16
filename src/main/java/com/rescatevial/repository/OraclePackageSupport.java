package com.rescatevial.repository;

import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

final class OraclePackageSupport {
    static final String PACKAGE = "FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK";

    private OraclePackageSupport() {
    }

    static SimpleJdbcCall call(JdbcTemplate jdbc, String procedure, SqlParameter... parameters) {
        return new SimpleJdbcCall(jdbc)
                .withCatalogName(PACKAGE)
                .withProcedureName(procedure)
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(parameters);
    }

    static SqlOutParameter cursor() {
        return new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, new ColumnMapRowMapper());
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> rows(Map<String, Object> result) {
        Object cursor = result.get("P_RESULTADO");
        return cursor instanceof List<?> ? (List<Map<String, Object>>) cursor : Collections.emptyList();
    }

    static MapSqlParameterSource parameters() {
        return new MapSqlParameterSource();
    }
}
