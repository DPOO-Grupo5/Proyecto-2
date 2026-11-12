# Prompts utilizados - IAG

## Prompt 1: Revisión de código

Actúa como asistente académico para apoyar la revisión del código Java de la Entrega 2. Contrasta su implementación con los requisitos y señala errores, validaciones faltantes o inconsistencias, indicando el archivo y una sugerencia. No inventes requisitos ni modifiques el código. El equipo evalúa las sugerencias y conserva las decisiones y la autoría del proyecto.

## Prompt 2: Revisión de consistencia

Compara el código, el README, los diagramas y el documento de diseño de la Entrega 2. Señala brevemente coincidencias, discrepancias y aspectos pendientes de comprobar, usando solo la información proporcionada. No presentes como implementado algo que no tenga evidencia. El equipo toma las decisiones finales sobre el proyecto.

## Prompt 3: Revisión de redacción

Revisa la redacción de los documentos de la Entrega 2 para mejorar claridad, ortografía, organización y coherencia. No cambies el sentido técnico ni inventes información. Presenta sugerencias y deja que el equipo decida qué cambios incorporar.


---

# Proyecto 2 · Uso de IAG (Claude, Anthropic)

El profesor confirmó que en el Proyecto 2 se permite usar IAG para generar funciones y código.
El equipo revisa, ejecuta y decide sobre todo lo generado.

## Sesión 1 (2026-11-08): diagnóstico y estructura de consola

**Prompts del equipo (resumidos en orden):**

1. "Tengo que comenzar a construir mi proyecto-2 de DPOO. Te comparto lo que hicimos en la entrega 2 (.md), la guía con las instrucciones del proyecto-2, el banco de historias de usuario y la rúbrica. Avísame qué más necesitas y qué podemos hacer."
2. Se compartió el repositorio `DPOO-Grupo5/Proyecto-1` y el PDF de la Entrega 2, y se pidió un diagnóstico.
3. "Sí, sigamos con la matriz completa de las 53 historias." (Se indicó: no hacer push, retirar `MainSocio`/`MainEmpleado` de Swing y enfocarse en lo que pide la rúbrica.)
4. "Sí, perfecto, vamos por ello" (estructura de tres `main` de consola, JUnit y primeras pruebas).

**Qué produjo la IA:**

- Una matriz de trazabilidad de las 53 historias del banco oficial frente al código (`docs/MATRIZ_TRAZABILIDAD_HU.md`), hecha con revisión estática del código.
- La reorganización de la consola: `ConsolaBase`, `ConsolaComprador`, `EntradaConsola`, `Menu`, `Formatos`, `VistaDesempeno`, `MainSocio`, `MainEmpleado` y `MainAdministrador`. Los cuerpos de las operaciones se trasladaron sin cambiar su comportamiento desde la antigua `Consola.java`.
- Consultas trasladadas de la consola al modelo: `SistemaClub.getSocios/getEmpleados/getEntrenadores/getEquiposActivos/obtenerDeporte/obtenerCategoria`. Los datos de demostración pasaron a `persistencia.DatosIniciales`.
- La configuración de JUnit 4 y 40 pruebas: `SistemaClubTest`, `ArchivoPlanoTest` e `Historias{Socio,Empleado,Administrador}Test`.

**Verificación:** compilación, las 40 pruebas pasan y se probaron las tres aplicaciones con entradas simuladas, incluido el ciclo guardar → reiniciar → cargar.

## Sesión 2 (2026-11-08): brechas de prioridad alta (compras, turnos, mensualidades y partidos oficiales)

**Prompt del equipo (resumido):** "Probé los tests y los main y todo bien. Horario de atención: 5:00 a. m. a 11:00 p. m. Los puntos y el bono cuentan como promoción y no se acumulan. Los partidos oficiales se siguen generando automáticamente en el calendario, pero con visto bueno del administrador, sin complicar mucho la implementación."

**Qué produjo la IA:**

- Primero las pruebas JUnit, a partir de los criterios del banco (fase roja), y después la implementación de HU-S02, S15, S16, S17, E03, E06, A16, A19 y T09.
- Cambios en el modelo: `TipoPromocion` (una sola promoción por compra), `Transaccion.aplicarCodigoDescuento` y `redimirPuntos` (1 punto = $1), puntos truncados, `Empleado.isCodigoActivo`, `Turno.APERTURA/CIERRE` (05:00–23:00), `PagoMensualidad`, `PartidoCompetencia.programar` (visto bueno) y `Entrenador.registrarResultadoOficial`.
- Correcciones en `ArchivoPlano`: los `enum` se guardan por nombre, y las colecciones ausentes en CSV antiguos se cargan vacías.
- Ajustes en la consola: código de descuento en tienda y cafetería para todos los usuarios, compra en cafetería para empleados, opciones de visto bueno y de pago de mensualidad para el administrador.

**Verificación:** 75 pruebas JUnit pasan. Se introdujeron tres errores a propósito (redondear puntos, omitir el control del entrenador responsable y permitir solicitudes duplicadas) y cada uno fue detectado por las pruebas. También se probaron las aplicaciones de consola con entradas simuladas sobre los datos existentes.

## Sesión 3 (2026-11-08): programación semanal de turnos

**Prompt del equipo:** "Para A11, el club solo funciona de lunes a sábado."

**Qué produjo la IA:** primero las pruebas (HU-E03 sin domingo, E04, A11 y A12) y después `ProgramacionSemanal` (borrador, bloques sin cobertura y publicación), el motivo de rechazo en `SolicitudCambioTurno`, el horario de atención de lunes a sábado en `Turno` y un editor de la programación en `MainAdministrador`, que reemplaza a "Asignar turno" porque esa opción no validaba la cobertura.

**Verificación:** 86 pruebas JUnit pasan. Se introdujeron dos errores a propósito (aceptar un solo entrenador y abrir los domingos) y ambos fueron detectados.

## Sesión 4 (2026-11-08): datos de demostración coherentes

**Prompt del equipo:** "Sí, adelante. Me parece una decisión viable contar con datos que sean congruentes con nuestras implementaciones."

**Qué produjo la IA:** una versión nueva de `DatosIniciales`: siete empleados con una programación que cumple la cobertura, dieciséis socios, dos equipos de fútbol completos, academias, productos y una liga con un partido programado y dos propuestos. También `DatosInicialesTest`, que verifica que esos datos cumplen las reglas, y la carpeta `datos_seneca`, regenerada con la propia aplicación.

**Verificación:** 91 pruebas JUnit pasan. Además se recorrieron en consola el registro de un resultado oficial, el visto bueno de un partido propuesto con la tabla de posiciones y la aprobación de un cambio de turno.

## Sesión 5 (2026-11-08): práctica, partidos internos, sesiones y bajas

**Prompt del equipo:** "Vale, hagámoslo" (brechas de prioridad alta: S04, S07, T10, T01–T08, A05).

**Qué produjo la IA:** primero las pruebas (`HistoriasPracticaTest`, `HistoriasPartidoInternoTest` y `HistoriasSesionesTest`, más casos en las de administrador y entrenador) y después la implementación:
- `Reloj`, para las reglas que dependen de la hora actual y para fijarla en las pruebas.
- Partido de práctica sobre una reserva, con lados, validación de agenda y sets por disciplina.
- Historial de prácticas.
- Partido interno en pasos: programar, alinear y registrar el resultado.
- Alineación en partidos internos y oficiales.
- Sesiones de equipo y de academia con sus validaciones, asistencia solo en sesiones finalizadas y evaluación por separado.
- Baja de jugadores y consulta de convocatorias (S08).
- Ajustes de los menús del socio, del entrenador y del administrador.

**Verificación:** 143 pruebas JUnit pasan. Se introdujeron cinco errores a propósito (agenda, sets en tenis de mesa, estadísticas sin alineación, asistencia antes de terminar la sesión y baja con nómina en curso) y todos fueron detectados. En consola se recorrieron: reserva, partido de práctica, resultado e historial; y partido interno programado, alineado, rechazado antes de jugarse y registrado después con una tarjeta roja que impide alinear a la jugadora en el siguiente partido.
