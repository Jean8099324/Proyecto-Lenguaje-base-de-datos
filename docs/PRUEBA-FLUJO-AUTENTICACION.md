# Prueba del flujo de inicio y cierre de sesión

La aplicación escribe eventos `AUTH_FLOW` en el journal de `rescatevial.service`.
Cada intento tiene un `traceId` que permite seguir las solicitudes y sus
redirecciones. Se registran rutas, códigos HTTP, destino de redirección, estado
de autenticación y huellas irreversibles de sesión. No se registran credenciales,
cookies ni tokens CSRF.

## Secuencia de prueba

1. En el teléfono, abre `https://163.192.131.49/login` y anota la hora local.
2. Inicia sesión una vez y espera el resultado. Anota la URL final y si aparece
   el dashboard o vuelve a la pantalla de inicio.
3. Si el dashboard abre, cierra sesión desde la aplicación. Anota la hora y
   confirma que aparece el aviso de cierre correcto.
4. Sin borrar cookies ni abrir otra pestaña, ingresa las credenciales otra vez
   y espera el resultado. Anota la hora local y la URL/página final.
5. Comparte esas horas y el resultado. Con ellas se consultan las líneas del
   servidor por `traceId` para reconstruir cada paso.

## Consulta de eventos en la instancia

```bash
sudo journalctl -u rescatevial.service --since "15 minutes ago" --no-pager | grep 'AUTH_FLOW'
```

Para consultar por un identificador que aparezca en la salida:

```bash
sudo journalctl -u rescatevial.service --since "30 minutes ago" --no-pager | grep 'traceId=IDENTIFICADOR'
```

Los eventos `request` y `response` muestran cada ruta solicitada y el destino
de `Location`; `authentication-success`, `authentication-failure`,
`csrf-rejected`, `access-denied` y `logout-success` muestran la decisión de
Spring Security.
