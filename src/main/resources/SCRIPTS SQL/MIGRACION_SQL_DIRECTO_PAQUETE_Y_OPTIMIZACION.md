# Migración de SQL directo al paquete Oracle y plan de optimización

## 1. Propósito

Este documento registra la corrección de emergencia realizada para cumplir el
requisito de que la aplicación Java no ejecute consultas SQL directas sobre las
tablas del sistema.

La corrección reemplazó consultas `SELECT`, `INSERT`, `UPDATE` y operaciones
de generación de identificadores realizadas desde Java por llamadas a
procedimientos del paquete:

`FIDE_PROYECTO_FINAL_RESCATE_VIAL_CR_PCK`

La migración elimina el SQL directo, pero algunas consultas que antes
resolvían `JOIN`, `COUNT`, `GROUP BY`, filtros y ordenamientos dentro de Oracle
ahora reciben cursores más amplios y procesan los datos en Java. Por eso esta
entrega debe considerarse una solución de cumplimiento urgente y no la versión
final optimizada.

## 2. Archivos Java modificados

- `src/main/java/com/rescatevial/repository/DashboardRepository.java`
- `src/main/java/com/rescatevial/repository/DespachoRepository.java`
- `src/main/java/com/rescatevial/repository/IncidenteRepository.java`
- `src/main/java/com/rescatevial/repository/UnidadRepository.java`
- `src/main/java/com/rescatevial/repository/OraclePackageSupport.java`

También se actualizaron las pruebas relacionadas con el portal y el reporte de
incidentes.

## 3. Soporte común agregado

Se agregó `OraclePackageSupport`, que centraliza:

- el nombre del paquete Oracle;
- la creación de `SimpleJdbcCall`;
- la declaración de cursores `REF_CURSOR`;
- la lectura de `P_RESULTADO`;
- el uso de `withoutProcedureColumnMetaDataAccess()`.

Esto evita que cada repositorio implemente una integración distinta con el
paquete y evita consultas automáticas de metadatos de procedimientos.

## 4. Consultas directas retiradas y reemplazos utilizados

### 4.1 Dashboard

Se retiraron consultas directas para:

- contar incidentes;
- contar incidentes en estado `EN ATENCION`;
- obtener los cinco incidentes recientes con provincia, tipo y estado;
- agrupar unidades por estado.

Se utilizaron:

| Necesidad | Procedimientos utilizados |
|---|---|
| Incidentes | `FIDE_OBTENER_INCIDENTES_SP` |
| Unidades | `FIDE_OBTENER_UNIDADES_SP` |
| Estados | `FIDE_OBTENER_ESTADOS_SP` |
| Provincias | `FIDE_OBTENER_PROVINCIAS_SP` |
| Tipos de incidente | `FIDE_OBTENER_TIPOS_INCIDENTES_SP` |

El código actual obtiene los cursores y hace en Java los conteos, filtros,
ordenamiento y agrupación.

### 4.2 Despacho

Se retiraron consultas directas para:

- incidentes pendientes;
- unidades disponibles;
- datos de ubicación;
- historial de unidades;
- generación del siguiente `ID_HISTORIAL`;
- inserción del historial;
- cambio de estado de unidad;
- cambio de estado de incidente;
- bloqueo y validación de registros.

Se utilizaron:

| Necesidad | Procedimiento |
|---|---|
| Incidentes | `FIDE_OBTENER_INCIDENTES_SP`, `FIDE_OBTENER_INCIDENTE_POR_ID_SP` |
| Vehículos | `FIDE_OBTENER_VEHICULOS_SP` |
| Unidades | `FIDE_OBTENER_UNIDADES_SP`, `FIDE_OBTENER_UNIDAD_POR_ID_SP` |
| Tipos de unidad | `FIDE_OBTENER_TIPOS_UNIDADES_SP` |
| Tipos de incidente | `FIDE_OBTENER_TIPOS_INCIDENTES_SP` |
| Prioridades | `FIDE_OBTENER_PRIORIDADES_SP` |
| Ubicaciones | `FIDE_OBTENER_UBICACION_INCIDENTES_SP` |
| Ubicación por incidente | `FIDE_OBTENER_UBICACION_POR_ID_SP` |
| Provincias, cantones y distritos | `FIDE_OBTENER_PROVINCIAS_SP`, `FIDE_OBTENER_CANTONES_SP`, `FIDE_OBTENER_DISTRITOS_SP` |
| Historiales | `FIDE_OBTENER_HISTORIALES_UNIDADES_SP`, `FIDE_INSERTAR_HISTORIALES_UNIDADES_SP` |
| Actualización de unidad | `FIDE_MODIFICAR_UNIDADES_SP` |
| Actualización de incidente | `FIDE_MODIFICAR_INCIDENTES_SP` |

La asignación actual ejecuta varios procedimientos separados. La operación
completa todavía debe convertirse en un procedimiento transaccional único.

### 4.3 Incidentes

Se retiraron consultas directas para:

- vehículos ocupados;
- detalle del vehículo y propietario;
- dirección del propietario;
- historial de asistencia;
- reparaciones;
- ventas, facturas y pagos;
- validación posterior de ubicación y contacto.

Se utilizaron:

| Necesidad | Procedimientos |
|---|---|
| Incidentes | `FIDE_OBTENER_INCIDENTES_SP`, `FIDE_OBTENER_INCIDENTE_POR_ID_SP` |
| Vehículos | `FIDE_OBTENER_VEHICULOS_SP`, `FIDE_OBTENER_VEHICULO_POR_ID_SP` |
| Usuarios | `FIDE_OBTENER_USUARIO_POR_ID_USUARIO_SP`, `FIDE_OBTENER_USUARIOS_SP` |
| Direcciones | `FIDE_OBTENER_DIRECCIONES_SP` |
| Catálogos | `FIDE_OBTENER_MARCAS_SP`, `FIDE_OBTENER_MODELOS_SP`, `FIDE_OBTENER_ANIOS_SP` |
| Contactos | `FIDE_INSERTAR_CONTACTOS_SP`, `FIDE_INSERTAR_INCIDENTES_CONTACTOS_SP`, `FIDE_OBTENER_CONTACTOS_POR_INCIDENTE_SP` |
| Historiales | `FIDE_OBTENER_HISTORIALES_UNIDADES_SP` |
| Reparaciones | `FIDE_OBTENER_REPARACIONES_SP` |
| Ventas | `FIDE_OBTENER_VENTAS_SP` |
| Facturas | `FIDE_OBTENER_FACTURAS_SP` |
| Pagos | `FIDE_OBTENER_PAGOS_SP` |

### 4.4 Unidades

El historial que antes usaba un `SELECT` directo ahora utiliza:

`FIDE_OBTENER_HISTORIALES_UNIDADES_SP`

El CRUD de unidades utiliza:

- `FIDE_OBTENER_UNIDADES_SP`
- `FIDE_OBTENER_UNIDAD_POR_ID_SP`
- `FIDE_INSERTAR_UNIDADES_SP`
- `FIDE_MODIFICAR_UNIDADES_SP`
- `FIDE_ELIMINAR_UNIDADES_SP`

## 5. Pérdida de eficiencia introducida por la solución de emergencia

La migración cumple la restricción arquitectónica, pero tiene estos costos:

1. Se descargan tablas o catálogos completos y se filtran en Java.
2. Se repiten llamadas a `FIDE_OBTENER_INCIDENTES_SP` y
   `FIDE_OBTENER_UNIDADES_SP` dentro de una misma vista.
3. El dashboard ya no ejecuta directamente en Oracle los `COUNT`, `GROUP BY`,
   `JOIN`, `ORDER BY` y `FETCH FIRST 5 ROWS ONLY`.
4. El detalle de un vehículo obtiene todos los catálogos y direcciones antes
   de filtrar el registro requerido.
5. El historial de un incidente obtiene historiales, reparaciones, facturas,
   pagos y catálogos completos antes de relacionarlos en Java.
6. El despacho calcula temporalmente el siguiente identificador de historial
   en Java. Esto puede tener problemas de concurrencia.
7. El cambio de unidad, incidente e historial ocurre mediante varias llamadas,
   por lo que la atomicidad depende de la transacción Java y no de una única
   operación Oracle.

## 6. Procedimientos recomendados para la siguiente entrega

Los siguientes procedimientos deben agregarse al encabezado y cuerpo del
paquete. Son propuestas de diseño; no fueron instalados durante esta
corrección de emergencia.

### 6.1 Dashboard: conteos y agregaciones

```sql
PROCEDURE FIDE_CONTAR_INCIDENTES_SP(
    P_RESULTADO OUT SYS_REFCURSOR
);

PROCEDURE FIDE_CONTAR_INCIDENTES_ATENCION_SP(
    P_RESULTADO OUT SYS_REFCURSOR
);

PROCEDURE FIDE_OBTENER_INCIDENTES_RECIENTES_SP(
    P_LIMITE IN NUMBER,
    P_RESULTADO OUT SYS_REFCURSOR
);

PROCEDURE FIDE_OBTENER_ESTADOS_UNIDADES_SP(
    P_RESULTADO OUT SYS_REFCURSOR
);
```

Implementación propuesta:

```sql
PROCEDURE FIDE_CONTAR_INCIDENTES_SP(
    P_RESULTADO OUT SYS_REFCURSOR
) IS
BEGIN
    OPEN P_RESULTADO FOR
        SELECT COUNT(*) AS TOTAL
        FROM FIDE_INCIDENTES_TB;
END;

PROCEDURE FIDE_CONTAR_INCIDENTES_ATENCION_SP(
    P_RESULTADO OUT SYS_REFCURSOR
) IS
BEGIN
    OPEN P_RESULTADO FOR
        SELECT COUNT(*) AS TOTAL
        FROM FIDE_INCIDENTES_TB I
        JOIN FIDE_ESTADOS_TB E ON E.ID_ESTADO = I.ID_ESTADO
        WHERE UPPER(TRIM(E.NOMBRE_ESTADO)) = 'EN ATENCION';
END;

PROCEDURE FIDE_OBTENER_INCIDENTES_RECIENTES_SP(
    P_LIMITE IN NUMBER,
    P_RESULTADO OUT SYS_REFCURSOR
) IS
BEGIN
    OPEN P_RESULTADO FOR
        SELECT I.ID_INCIDENTE,
               P.NOMBRE_PROVINCIA AS UBICACION,
               T.NOMBRE_TIPO_INCIDENTE AS TIPO,
               E.NOMBRE_ESTADO AS ESTADO,
               I.FECHA_REPORTE,
               I.HORA_REPORTE
        FROM FIDE_INCIDENTES_TB I
        LEFT JOIN FIDE_UBICACION_INCIDENTES_TB U
               ON U.ID_INCIDENTE = I.ID_INCIDENTE
        LEFT JOIN FIDE_PROVINCIAS_TB P
               ON P.ID_PROVINCIA = U.ID_PROVINCIA
        LEFT JOIN FIDE_TIPOS_INCIDENTES_TB T
               ON T.ID_TIPO_INCIDENTE = I.ID_TIPO_INCIDENTE
        LEFT JOIN FIDE_ESTADOS_TB E
               ON E.ID_ESTADO = I.ID_ESTADO
        ORDER BY I.FECHA_REPORTE DESC NULLS LAST,
                 I.ID_INCIDENTE DESC
        FETCH FIRST 5 ROWS ONLY;
END;

PROCEDURE FIDE_OBTENER_ESTADOS_UNIDADES_SP(
    P_RESULTADO OUT SYS_REFCURSOR
) IS
BEGIN
    OPEN P_RESULTADO FOR
        SELECT E.NOMBRE_ESTADO, COUNT(*) AS TOTAL
        FROM FIDE_UNIDADES_TB U
        JOIN FIDE_ESTADOS_TB E ON E.ID_ESTADO = U.ID_ESTADO
        GROUP BY E.NOMBRE_ESTADO;
END;
```

El valor fijo `5` del procedimiento de incidentes recientes puede cambiarse
por `FETCH FIRST P_LIMITE ROWS ONLY` si la versión Oracle lo permite en ese
contexto. Si no lo permite, se debe usar una subconsulta con `ROWNUM`.

### 6.2 Despacho: consultas filtradas y asignación atómica

```sql
PROCEDURE FIDE_OBTENER_INCIDENTES_PENDIENTES_DESPACHO_SP(
    P_RESULTADO OUT SYS_REFCURSOR
);

PROCEDURE FIDE_OBTENER_UNIDADES_DISPONIBLES_DESPACHO_SP(
    P_ID_ESTADO IN NUMBER,
    P_RESULTADO OUT SYS_REFCURSOR
);

PROCEDURE FIDE_ASIGNAR_UNIDAD_INCIDENTE_SP(
    P_ID_UNIDAD IN NUMBER,
    P_ID_INCIDENTE IN NUMBER,
    P_OBSERVACIONES IN VARCHAR2,
    P_ID_RESPONSABLE IN NUMBER,
    P_RESULTADO OUT NUMBER
);
```

La implementación de `FIDE_ASIGNAR_UNIDAD_INCIDENTE_SP` debe:

1. Bloquear la unidad y el incidente con `SELECT ... FOR UPDATE` dentro del
   paquete.
2. Verificar que la unidad esté disponible.
3. Verificar que el incidente esté en estado `REPORTADO`.
4. Generar el siguiente historial de forma segura, preferiblemente mediante
   una secuencia Oracle o una estrategia protegida por el bloqueo.
5. Insertar el historial.
6. Actualizar la unidad.
7. Actualizar el incidente.
8. Confirmar todo como una única operación o hacer `ROLLBACK` ante cualquier
   error.

No se recomienda que Java calcule `MAX(ID_HISTORIAL) + 1`, porque dos
solicitudes concurrentes podrían obtener el mismo valor.

### 6.3 Incidentes: consultas por identificador

```sql
PROCEDURE FIDE_OBTENER_VEHICULO_DETALLE_INCIDENTE_SP(
    P_ID_VEHICULO IN NUMBER,
    P_RESULTADO OUT SYS_REFCURSOR
);

PROCEDURE FIDE_OBTENER_HISTORIAL_INCIDENTE_DETALLE_SP(
    P_ID_INCIDENTE IN NUMBER,
    P_RESULTADO OUT SYS_REFCURSOR
);

PROCEDURE FIDE_OBTENER_FACTURACION_INCIDENTE_SP(
    P_ID_INCIDENTE IN NUMBER,
    P_RESULTADO OUT SYS_REFCURSOR
);

PROCEDURE FIDE_OBTENER_VEHICULOS_DISPONIBLES_INCIDENTE_SP(
    P_ID_INCIDENTE_EXCLUIDO IN NUMBER,
    P_RESULTADO OUT SYS_REFCURSOR
);
```

Estos procedimientos deben hacer en Oracle los `JOIN` y filtros por
`P_ID_VEHICULO` o `P_ID_INCIDENTE`. Así Java recibe únicamente las filas
necesarias y no debe cargar las tablas completas de vehículos, direcciones,
historiales, reparaciones, facturas o pagos.

### 6.4 Generación segura de identificadores

La alternativa preferida para historiales es una secuencia Oracle:

```sql
CREATE SEQUENCE FIDE_HISTORIALES_UNIDADES_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE;
```

Si el diseño exige que el historial sea consecutivo por unidad, el cálculo
debe permanecer dentro de `FIDE_ASIGNAR_UNIDAD_INCIDENTE_SP` y ejecutarse bajo
bloqueo de la unidad. No debe calcularse en Java.

## 7. Orden recomendado de la próxima entrega

1. Agregar las declaraciones al `ENCABEZADO.SQL`.
2. Agregar las implementaciones al `CUERPO.SQL`.
3. Probar cada procedimiento directamente en Oracle.
4. Reemplazar en Java los procedimientos generales por los procedimientos
   filtrados y agregados.
5. Revisar que cada vista haga una sola carga de datos por operación.
6. Medir cantidad de llamadas, filas transferidas y tiempo de respuesta.
7. Mantener la búsqueda de SQL directo fuera de `SCRIPTS SQL`.
8. Ejecutar:

```text
mvn -q -DskipTests compile
mvn -q test
```

## 8. Estado de validación de esta entrega

- La compilación Maven terminó correctamente.
- Las pruebas Maven terminaron correctamente.
- No quedaron consultas SQL directas de negocio en `src/main/java`.
- Las llamadas `{call ...}` restantes son llamadas a procedimientos del paquete.
- Los scripts SQL de definición del paquete permanecen en
  `src/main/resources/SCRIPTS SQL` y no forman parte del código Java que se
  revisó para detectar SQL directo.

