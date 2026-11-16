package com.rescatevial.service;
import com.rescatevial.domain.*;
import com.rescatevial.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ClienteIncidenteService {
 private final ClienteService clientes;
 private final UsuarioRepository usuarios;
 private final DatoContactoService datos;
 public ClienteIncidenteService(ClienteService clientes, UsuarioRepository usuarios, DatoContactoService datos) {
  this.clientes=clientes; this.usuarios=usuarios; this.datos=datos;
 }
 @Transactional
 public Usuario crear(ClienteIncidenteForm f) {
  Usuario u=new Usuario();
  u.setNombre(f.getNombre().trim()); u.setPrimerApellido(f.getPrimerApellido().trim());
  u.setSegundoApellido(f.getSegundoApellido()==null || f.getSegundoApellido().isBlank()?null:f.getSegundoApellido().trim());
  u.setTipoUsuario(clientes.tipoClienteId()); u.setEstado(clientes.estadoActivoId());
  // Identificador interno para recuperar el ID generado por el procedimiento, sin credenciales ni roles.
  u.setUsername("cliente-"+java.util.UUID.randomUUID());
  usuarios.insertUsingProcedure(u, null);
  Usuario creado=usuarios.findByUsername(u.getUsername()).orElseThrow(()->new IllegalStateException("No se pudo registrar el cliente."));
  DatoContacto direccion=new DatoContacto(); direccion.setIdUsuario(creado.getId());
  direccion.setIdProvincia(f.getIdProvincia()); direccion.setIdCanton(f.getIdCanton());
  direccion.setIdDistrito(f.getIdDistrito()); direccion.setOtrasSenas(f.getOtrasSenas());
  datos.guardar("direccion", direccion, false);
  contacto(creado.getId(),"telefono",f.getTelefono()); contacto(creado.getId(),"correo",f.getCorreo());
  return creado;
 }
 private void contacto(Long id,String tipo,String valor) {
  if(valor==null || valor.isBlank()) return;
  DatoContacto d=new DatoContacto(); d.setIdUsuario(id); d.setValor(valor.trim()); datos.guardar(tipo,d,false);
 }
}
