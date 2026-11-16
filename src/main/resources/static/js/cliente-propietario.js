document.addEventListener("DOMContentLoaded", () => {
 document.querySelectorAll(".cliente-propietario").forEach(async root => {
  const select=root.querySelector(".seleccionar-cliente"), search=root.querySelector(".buscar-cliente"), panel=root.querySelector(".nuevo-cliente"), error=root.querySelector(".error-cliente");
  const field=name=>root.querySelector(`[data-campo="${name}"]`);
  let catalogo;
  root.querySelector(".guardar-cliente").disabled=true;
  const renderClientes=()=>{
   const current=select.value||root.dataset.actual||"";
   const query=search.value.trim().toLocaleLowerCase("es");
   select.replaceChildren(new Option("Seleccione cliente",""), ...catalogo.clientes.filter(c=>String(c.id)===current||`${c.nombre} ${c.id}`.toLocaleLowerCase("es").includes(query)).map(c=>new Option(`${c.nombre} (#${c.id})`,c.id)));
   select.value=current;
  };
  const opciones=(name,rows)=>field(name).replaceChildren(new Option("Seleccione?",""),...rows.map(o=>new Option(o.nombre,o.id)));
  field("idProvincia").addEventListener("change",()=>{opciones("idCanton",catalogo.cantones.filter(c=>String(c.idPadre)===field("idProvincia").value));opciones("idDistrito",[]);});
  field("idCanton").addEventListener("change",()=>opciones("idDistrito",catalogo.distritos.filter(d=>String(d.idPadre)===field("idCanton").value)));
  search.addEventListener("input",()=>{if(catalogo)renderClientes();});
  select.addEventListener("change",()=>{root.dataset.actual=select.value;});
  root.querySelector(".abrir-cliente").addEventListener("click",()=>panel.hidden=false);
  root.querySelector(".cancelar-cliente").addEventListener("click",()=>panel.hidden=true);
  try {
   const response=await fetch("/incidentes/clientes",{headers:{Accept:"application/json"}});
   if(!response.ok)throw new Error("No se pudieron cargar los clientes. Recargue la p?gina.");
   catalogo=await response.json(); renderClientes(); opciones("idProvincia",catalogo.provincias);opciones("idCanton",[]);opciones("idDistrito",[]);
   root.querySelector(".guardar-cliente").disabled=false;
  } catch(e) {error.textContent=e.message;}
  root.querySelector(".guardar-cliente").addEventListener("click",async event=>{
   error.textContent="";
   for(const name of ["nombre","primerApellido","idProvincia","idCanton","idDistrito"]){
    if(!field(name).value.trim()){error.textContent="Complete nombre, primer apellido, provincia, cant?n y distrito.";field(name).focus();return;}
   }
   const body=new URLSearchParams();root.querySelectorAll("[data-campo]").forEach(input=>body.set(input.dataset.campo,input.value));
   const csrf=root.closest("form").querySelector('input[name="_csrf"]');if(csrf)body.set(csrf.name,csrf.value);
   event.target.disabled=true;
   try {
    const response=await fetch("/incidentes/clientes",{method:"POST",body,headers:{Accept:"application/json"}});
    const result=await response.json();if(!response.ok)throw new Error(result.error||"No se pudo guardar el cliente.");
    catalogo.clientes.push(result);search.value="";root.dataset.actual=String(result.id);select.value="";renderClientes();panel.hidden=true;
    root.querySelectorAll("[data-campo]").forEach(input=>input.value="");
   }catch(e){error.textContent=e.message;}finally{event.target.disabled=false;}
  });
 });
});
