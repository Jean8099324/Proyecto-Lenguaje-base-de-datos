package com.rescatevial.service;

import com.rescatevial.domain.Estado;
import com.rescatevial.domain.OpcionVehiculo;
import com.rescatevial.domain.Unidad;
import com.rescatevial.repository.UnidadRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadService {
    private final UnidadRepository repo; private final EstadoService estados; private final TipoUnidadService tipos;
    public UnidadService(UnidadRepository repo, EstadoService estados, TipoUnidadService tipos) { this.repo=repo; this.estados=estados; this.tipos=tipos; }
    @Transactional(readOnly=true) public List<Unidad> listar() { var tipo=tipos.listar().stream().collect(Collectors.toMap(opcion -> opcion.getId(), opcion -> opcion, (a,b)->a)); var estado=estados.listar().stream().collect(Collectors.toMap(e -> e.getId(), e -> e,(a,b)->a)); var list=repo.listar(); list.forEach(u->{u.setTipoNombre(tipo.containsKey(u.getIdTipo())?tipo.get(u.getIdTipo()).getNombre():"Sin tipo");u.setEstadoNombre(estado.containsKey(u.getIdEstado())?estado.get(u.getIdEstado()).getNombreEstado():"SIN ESTADO");}); cargarIncidentesAsignados(list); return list; }
    private void cargarIncidentesAsignados(List<Unidad> unidades) {
        unidades.forEach(u -> u.setIncidenteAsignadoId(null));
        var asignadas = unidades.stream()
                .filter(u -> "ASIGNADA".equalsIgnoreCase(u.getEstadoNombre()))
                .toList();
        if (asignadas.isEmpty()) return;
        Map<Long, Map<String, Object>> ultimos = new java.util.HashMap<>();
        for (var historial : repo.historiales()) {
            Long unidadId = ((Number) historial.get("ID_UNIDAD")).longValue();
            ultimos.merge(unidadId, historial, (a, b) ->
                    ((Number) a.get("ID_HISTORIAL")).longValue()
                            >= ((Number) b.get("ID_HISTORIAL")).longValue() ? a : b);
        }
        for (var unidad : asignadas) {
            var historial = ultimos.get(unidad.getId());
            if (historial != null && historial.get("FECHA_REGRESO_UNIDAD") == null
                    && historial.get("ID_INCIDENTE") instanceof Number incidente) {
                unidad.setIncidenteAsignadoId(incidente.longValue());
            }
        }
    }

    public Optional<Unidad> buscar(Long id){return repo.buscar(id);} public List<OpcionVehiculo> tipos(){return tipos.listar();} public List<Estado> estados(){return estados.listar();}
    public Long activo(){return estados.listar().stream().filter(e->"ACTIVO".equalsIgnoreCase(e.getNombreEstado())).map(estado -> estado.getId()).findFirst().orElse(1L);}
    @Transactional public void guardar(Unidad u){u.setNombre(u.getNombre().trim());u.setPlaca(u.getPlaca()==null||u.getPlaca().isBlank()?null:u.getPlaca().trim().toUpperCase());if(u.getId()==null){u.setIdEstado(activo());repo.insertar(u);}else repo.modificar(u);}
    @Transactional public void desactivar(Long id){repo.desactivar(id);}
    public List<Map<String,Object>> historial(Long id){return repo.historial(id);}
}
