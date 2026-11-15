package com.rescatevial.service;

import com.rescatevial.domain.OpcionVehiculo;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

@Service
public class TipoUnidadService {
    private final JdbcTemplate jdbc;
    public TipoUnidadService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @SuppressWarnings("unchecked") public List<OpcionVehiculo> listar() {
        var call = new SimpleJdbcCall(jdbc).withCatalogName("FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK").withProcedureName("FIDE_OBTENER_TIPOS_UNIDADES_SP").withoutProcedureColumnMetaDataAccess().declareParameters(new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR, (rs,row)->{ var o=new OpcionVehiculo(); o.setId(rs.getLong("ID_TIPO_UNIDAD")); o.setNombre(rs.getString("NOMBRE_TIPO_UNIDAD")); o.setEstadoId(rs.getLong("ID_ESTADO")); return o; }));
        return (List<OpcionVehiculo>) call.execute().get("P_RESULTADO");
    }
}
