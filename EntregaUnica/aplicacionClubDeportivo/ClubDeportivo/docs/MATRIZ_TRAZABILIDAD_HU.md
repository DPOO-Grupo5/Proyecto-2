# Matriz de trazabilidad: banco oficial de historias de usuario → código

Proyecto 2 · Club Deportivo Séneca (DPOO). Base: código de la Entrega 2 del Proyecto 1, commit `6c22a14`.

> **Método:** revisión estática del código (modelo y `club.consola.Consola`), siguiendo el flujo de cada historia. Las historias marcadas como «Cumple» tienen pruebas JUnit en `test/club/historias`; para el resto, la columna *Prueba sugerida* indica qué prueba las confirmará.

**Resumen:** 34 cumplen · 19 parciales · 0 no cumplen (de 53).

| Rol | Cumple | Parcial | No cumple |
|---|---:|---:|---:|
| Socio | 10 | 7 | 0 |
| Empleado | 5 | 1 | 0 |
| Entrenador | 10 | 0 | 0 |
| Administrador | 9 | 11 | 0 |

## Detalle por historia

### HU-S01 · Iniciar sesión como socio — **Cumple** (prioridad media)

- **Implementación:** SistemaClub.autenticar · Usuario.validarPassword (PBKDF2)
- **Interfaz:** MainSocio (login restringido al rol)
- **Brecha:** Cumple. Proyecto 2: MainSocio solo admite usuarios Socio y responde «Credenciales incorrectas» para cualquier otro caso.
- **Acción:** Hecho. Verificado en HistoriasSocioTest.
- **Prueba sugerida:** autenticar() devuelve el socio con credenciales correctas; null con contraseña errada y con login inexistente.

### HU-S02 · Inscribirme en una academia individual — **Cumple** (prioridad alta)

- **Implementación:** Socio.inscribirseEnDisciplina(disciplina, nivelInicial)
- **Interfaz:** MainSocio 2
- **Brecha:** Proyecto 2: el socio elige su nivel inicial (Principiante, Intermedio o Avanzado). Cumple éxito, ELO por disciplina, duplicado y nivel o disciplina inválidos.
- **Acción:** Hecho (cambio frente a la Entrega 2: antes el nivel era siempre Principiante).
- **Prueba sugerida:** Inscribe con nivel Intermedio y ELO 1000; inscripción en tenis y pádel con ELO separados; rechazo por duplicado; rechazo por nivel inválido.

### HU-S03 · Reservar una instalación — **Cumple** (prioridad media)

- **Implementación:** Socio.reservarInstalacion · Reserva.validar · DisciplinaIndividual.hayCupoSimultaneo · Cancha*.admiteJugadores
- **Interfaz:** Menú socio 3
- **Brecha:** Cumple los cinco criterios: cupo simultáneo, capacidad por modalidad, traslape, inscripción y éxito asociado a la cuenta.
- **Acción:** Solo pruebas.
- **Prueba sugerida:** Éxito; rechazo por cupo simultáneo; pádel con 2 jugadores; tenis dobles con 3; traslape; socio no inscrito.

### HU-S04 · Programar un partido de práctica — **Cumple** (prioridad alta)

- **Implementación:** Socio.programarPartidoPractica(reserva, otros, entrenador) · PartidoPractica · Socio.tieneCompromisoEntre
- **Interfaz:** MainSocio 4
- **Brecha:** Proyecto 2: el partido se programa sobre una reserva activa del socio, sin otro partido; los participantes son tantos socios como jugadores tenga la reserva (en dobles, dos lados de dos), distintos e inscritos en la disciplina. Niveles iguales o adyacentes, salvo si participa un entrenador (acompaña, no ocupa cupo). Se rechazan choques de agenda con otros partidos, reservas o sesiones.
- **Acción:** Hecho.
- **Prueba sugerida:** Éxito con reserva; dobles con 4; rechazo Principiante–Avanzado; excepción con entrenador; sin reserva; participante repetido; choque de agenda.

### HU-S05 · Iniciar el uso de mi reserva sin bebida caliente — **Parcial** (prioridad media)

- **Implementación:** Reserva.validar (bebida caliente en los últimos 30 min, al reservar) · Socio.estaEnZonaRestringidaParaBebidasCalientes
- **Interfaz:** No hay opción «iniciar uso»
- **Brecha:** La regla se aplica al crear la reserva, no al iniciar el uso. No existe la operación de inicio de uso.
- **Acción:** Crear Reserva.iniciarUso(socio, instante) con la regla (pádel o mesa techada) y agregar la opción al menú. Documentar cómo se modela «tener una bebida caliente» (compra reciente).
- **Prueba sugerida:** Pádel sin bebida → permite; pádel con bebida reciente → rechaza; tenis con bebida → permite.

### HU-S06 · Consultar mi ranking por disciplina — **Parcial** (prioridad media)

- **Implementación:** InscripcionDisciplina.getRanking
- **Interfaz:** Menú socio 6 (dentro de «Mi desempeño»)
- **Brecha:** Muestra el ELO pero no la posición en el ranking, ni el aviso «sin partidos», ni el rechazo por disciplina no inscrita (solo lista las inscritas).
- **Acción:** Método de ranking ordenado por disciplina (posición del socio) y opción de menú propia con esos tres casos.
- **Prueba sugerida:** Posición correcta tras partidos; ELO 1000 sin partidos; rechazo por no inscrito.

### HU-S07 · Consultar mi historial de partidos de práctica — **Cumple** (prioridad alta)

- **Implementación:** Socio.historialPracticas · PartidoPractica.oponentesDe/resultadoPara
- **Interfaz:** MainSocio 5
- **Brecha:** Proyecto 2: lista cronológica de partidos con resultado por disciplina, con fecha, oponentes, resultado («Ganó 2-1»), estadísticas y ELO actual. Rango de fechas opcional; lista vacía sin error; se rechazan una disciplina no inscrita y un rango invertido.
- **Acción:** Hecho.
- **Prueba sugerida:** Lista cronológica; lista vacía sin error; rechazo por rango invertido; rechazo por disciplina no inscrita.

### HU-S08 · Consultar mis convocatorias a entrenamiento — **Cumple** (prioridad media)

- **Implementación:** Socio.consultarConvocatorias
- **Interfaz:** MainSocio 7
- **Brecha:** Proyecto 2: opción nueva que lista las sesiones convocadas (próximas y pasadas) con fecha, hora, duración, instalación y entrenador, y un mensaje si no hay.
- **Acción:** Hecho.
- **Prueba sugerida:** Socio convocado ve la sesión; socio sin convocatorias recibe lista vacía.

### HU-S09 · Consultar mi asistencia y evaluaciones — **Parcial** (prioridad baja)

- **Implementación:** Socio.getAsistencias · Asistencia
- **Interfaz:** Menú socio 6
- **Brecha:** Muestra estado, nota y comentario, y el mensaje vacío. No muestra como «pendiente» las sesiones convocadas sin asistencia registrada.
- **Acción:** Recorrer sesionesConvocado y marcar pendientes las que no tengan Asistencia.
- **Prueba sugerida:** Sesión con asistencia; sesión sin asistencia → pendiente; sin registros.

### HU-S10 · Consultar las estadísticas de mi equipo y las mías — **Parcial** (prioridad media)

- **Implementación:** Equipo.getPartidosInternos · Socio.getEstadisticas
- **Interfaz:** Menú socio 1 y 6
- **Brecha:** Lista partidos y suma estadísticas, pero no calcula PJ, G, E, P ni tantos a favor y en contra del equipo. El rechazo de equipo ajeno no aplica porque solo se muestran los propios (documentarlo).
- **Acción:** Método Equipo.resumenResultados() (PJ/G/E/P/GF/GC) y presentación con ceros si no hay partidos.
- **Prueba sugerida:** Resumen correcto con victoria, empate y derrota; equipo sin partidos → ceros.

### HU-S11 · Consultar la tabla de posiciones de una competencia — **Parcial** (prioridad media)

- **Implementación:** FormatoLiga.actualizarPosiciones · Grupo · PosicionEquipo
- **Interfaz:** Solo menú admin 11
- **Brecha:** El socio no tiene acceso. El desempate en puntos usa diferencia de goles, pero el banco pide enfrentamiento directo. Sin filtros por deporte o categoría ni mensaje para competencias sin resultados.
- **Acción:** Opción en el menú socio, desempate por enfrentamiento directo en el comparador, filtros y mensaje «no hay información».
- **Prueba sugerida:** Orden por puntos; desempate por enfrentamiento directo; tabla de grupo; competencia sin resultados.

### HU-S12 · Consultar goleadores y jugadores destacados — **Parcial** (prioridad media)

- **Implementación:** Competencia.clasificacionGoleadores · clasificacionDestacados
- **Interfaz:** Solo menú admin 11
- **Brecha:** Goleadores cumple. «Destacados» suma goles+asistencias, puntos+rebotes y aces+bloqueos, pero el banco pide asistencias (fútbol), puntos (baloncesto) y aces (voleibol). Solo es accesible para el admin y no avisa cuando no hay estadísticas.
- **Acción:** Corregir el criterio por deporte, agregar la opción al menú socio y el mensaje vacío.
- **Prueba sugerida:** Orden de goleadores; destacados por asistencias, puntos y aces; competencia sin estadísticas.

### HU-S13 · Comprar en la tienda — **Cumple** (prioridad media)

- **Implementación:** Transaccion.agregarDetalle/confirmar · validarExistencias · descontarExistencias
- **Interfaz:** MainSocio 8
- **Brecha:** Cumple: IVA 19 %, desglose, compra atómica que no altera el inventario si falta existencia e indica el artículo, y cantidad ≤ 0 rechazada.
- **Acción:** Solo pruebas.
- **Prueba sugerida:** Venta con IVA y descuento de inventario; rechazo total si falta un artículo (inventario intacto); cantidad 0.

### HU-S14 · Comprar en la cafetería — **Parcial** (prioridad baja)

- **Implementación:** Transaccion (rubro cafeteria) · consultarAlergenos · Socio.estaEnZonaRestringidaParaBebidasCalientes
- **Interfaz:** MainSocio 9
- **Brecha:** 8 % de impuesto, propina del 10 % modificable (incluso 0) y bloqueo de bebida caliente con reserva activa sí cumplen. No rechaza snacks sin alérgenos registrados, porque la lista vacía se trata como «sin alérgenos».
- **Acción:** Distinguir «alérgenos no registrados» de «ninguno» en Snack y rechazar el primer caso al confirmar.
- **Prueba sugerida:** Impuesto 8 % y propina 10 %; propina 0; snack sin alérgenos registrados; bebida caliente con reserva de pádel activa.

### HU-S15 · Acumular puntos de fidelidad — **Cumple** (prioridad alta)

- **Implementación:** Transaccion.calcularPuntosGenerados
- **Interfaz:** Automático al confirmar
- **Brecha:** Proyecto 2: los puntos son el 2 % del total pagado sin propina y se truncan (antes se redondeaban).
- **Acción:** Hecho.
- **Prueba sugerida:** Total 10.990 → 219 puntos; compra rechazada → saldo sin cambio.

### HU-S16 · Redimir puntos de fidelidad — **Cumple** (prioridad alta)

- **Implementación:** Transaccion.redimirPuntos
- **Interfaz:** MainSocio 8/9
- **Brecha:** Proyecto 2: 1 punto = $1 sobre el total (antes $100), tope en el saldo y en el total, y el rechazo informa el saldo disponible. Los puntos cuentan como promoción y no se acumulan con códigos ni bonos.
- **Acción:** Hecho. Documentar el cambio frente a la Entrega 1 (RF-TC-08).
- **Prueba sugerida:** Redimir 500 puntos reduce $500; rechazo por más puntos que el saldo; rechazo por más que el total.

### HU-S17 · Usar el código de descuento de un empleado — **Cumple** (prioridad alta)

- **Implementación:** Transaccion.aplicarCodigoDescuento · TipoPromocion · Empleado.isCodigoActivo
- **Interfaz:** MainSocio 8/9
- **Brecha:** Proyecto 2: cualquier código activo de empleado da 8 % sobre el subtotal antes de impuestos, en tienda y cafetería. Se eliminó la autorización previa del empleado, que no estaba en el banco. Se rechaza un código inexistente o inactivo y la combinación con otra promoción.
- **Acción:** Hecho.
- **Prueba sugerida:** 8 % sobre el subtotal antes de impuestos; código inexistente; código con otra promoción ya aplicada; compra en cafetería.

### HU-E01 · Iniciar sesión como empleado — **Cumple** (prioridad media)

- **Implementación:** SistemaClub.autenticar
- **Interfaz:** MainEmpleado
- **Brecha:** Cumple. MainEmpleado admite entrenadores y fisioterapeutas; el menú del entrenador agrega sus opciones.
- **Acción:** Hecho. Verificado en HistoriasEmpleadoTest.
- **Prueba sugerida:** Entrenador y fisioterapeuta se autentican; rechazo con contraseña errada.

### HU-E02 · Consultar mis turnos semanales — **Cumple** (prioridad baja)

- **Implementación:** Empleado.consultarTurnos
- **Interfaz:** MainEmpleado 1
- **Brecha:** Cumple: día y franja de cada turno, y mensaje si no tiene turnos.
- **Acción:** Solo pruebas.
- **Prueba sugerida:** Turnos asignados listados; lista vacía.

### HU-E03 · Solicitar un cambio de turno — **Cumple** (prioridad media)

- **Implementación:** Empleado.solicitarCambioTurno · Turno.estaDentroDelHorarioDeAtencion
- **Interfaz:** MainEmpleado 2
- **Brecha:** Proyecto 2: horario de atención de lunes a sábado, de 05:00 a 23:00 (decisión del grupo). Valida turno propio, horario distinto del actual, dentro del horario de atención (el domingo queda fuera) y sin otra solicitud pendiente sobre el mismo turno.
- **Acción:** Hecho.
- **Prueba sugerida:** Solicitud pendiente con turno original vigente; turno ajeno; horario igual; fuera del horario; segunda solicitud pendiente.

### HU-E04 · Consultar el estado de mis solicitudes de cambio — **Cumple** (prioridad baja)

- **Implementación:** SolicitudCambioTurno.getEstado/getMotivoRechazo
- **Interfaz:** MainEmpleado 1
- **Brecha:** Proyecto 2: cada solicitud muestra su estado y, si fue rechazada, el motivo. Hay mensaje cuando no hay solicitudes.
- **Acción:** Hecho.
- **Prueba sugerida:** Solicitud rechazada muestra el motivo; sin solicitudes.

### HU-E05 · Sugerir un nuevo artículo o snack — **Parcial** (prioridad media)

- **Implementación:** Empleado.sugerirProducto · SugerenciaProducto
- **Interfaz:** Menú empleado 5
- **Brecha:** La sugerencia no guarda descripción ni autor, y la categoría solo se pide para la tienda. Los mensajes de dato faltante dependen de los setters de Producto.
- **Acción:** Agregar descripción y autor a SugerenciaProducto y validar nombre y descripción indicando el dato faltante.
- **Prueba sugerida:** Sugerencia pendiente con autor; rechazo por nombre vacío; rechazo por descripción vacía.

### HU-E06 · Comprar con mi código de empleado — **Cumple** (prioridad alta)

- **Implementación:** Transaccion.aplicarCodigoDescuento
- **Interfaz:** MainEmpleado 3/4
- **Brecha:** Proyecto 2: código propio → 15 %; código de otro empleado → 8 %; inexistente → rechazo. Los empleados ya pueden comprar en la cafetería. La compra genera puntos.
- **Acción:** Hecho.
- **Prueba sugerida:** Código propio 15 %; código ajeno 8 %; código inexistente; compra en cafetería.

### HU-T01 · Planear una sesión de entrenamiento — **Cumple** (prioridad alta)

- **Implementación:** Entrenador.planearSesionEquipo/planearSesionAcademia
- **Interfaz:** MainEmpleado 8
- **Brecha:** Proyecto 2: sesión de equipo en cancha de conjunto (convoca a cada jugador activo) o de academia en una instalación de la disciplina (uno o varios inscritos). Se rechazan ocupación, incompatibilidad, convocados ajenos, duración ≤ 0, fecha pasada y traslape con otra sesión del mismo entrenador.
- **Acción:** Hecho.
- **Prueba sugerida:** Sesión de equipo con convocatoria por jugador; instalación incompatible; fecha pasada; traslape propio; convocado ajeno.

### HU-T02 · Registrar la asistencia de los convocados — **Cumple** (prioridad media)

- **Implementación:** Entrenador.registrarAsistencia · SesionEntrenamiento.haFinalizado
- **Interfaz:** MainEmpleado 9
- **Brecha:** Proyecto 2: solo en sesiones propias ya finalizadas; valida convocado y estado.
- **Acción:** Hecho.
- **Prueba sugerida:** Sesión finalizada OK; sesión futura rechazada; no convocado; estado inválido; sesión de otro entrenador.

### HU-T03 · Registrar una evaluación de desempeño — **Cumple** (prioridad media)

- **Implementación:** Entrenador.evaluar · Asistencia.evaluar
- **Interfaz:** MainEmpleado 9/10
- **Brecha:** Proyecto 2: nota entera de 1 a 5 con comentario, opcional. Se puede dar al registrar la asistencia o después; se rechaza para quien faltó o no tiene asistencia.
- **Acción:** Hecho.
- **Prueba sugerida:** Nota 4 a quien asistió; nota 0 y 6 rechazadas; evaluar a quien faltó → rechazo.

### HU-T04 · Programar un partido interno — **Cumple** (prioridad alta)

- **Implementación:** Entrenador.programarPartidoInterno · PartidoInterno.programar
- **Interfaz:** MainEmpleado 13
- **Brecha:** Proyecto 2: programar es un paso propio (fecha futura, equipos distintos del mismo deporte y categoría). Si falta el mínimo de habilitados, el mensaje indica el equipo y cuántos faltan.
- **Acción:** Hecho.
- **Prueba sugerida:** Programar OK; categorías distintas; mismo equipo; fecha pasada; equipo con habilitados insuficientes (mensaje con faltantes).

### HU-T05 · Definir la alineación de un partido — **Cumple** (prioridad alta)

- **Implementación:** PartidoInterno.definirAlineacion · PartidoCompetencia.definirAlineacion
- **Interfaz:** MainEmpleado 14
- **Brecha:** Proyecto 2: alineación por equipo con jugadores activos y sin suspensión (el mensaje informa la suspensión); en partidos oficiales, además, de la nómina. Una amarilla sola no impide jugar; cumplida la suspensión, el jugador vuelve a ser alineable.
- **Acción:** Hecho.
- **Prueba sugerida:** Alinear habilitados; jugador con una amarilla OK; suspendido rechazado; jugador ajeno o fuera de nómina rechazado; suspensión ya cumplida OK.

### HU-T06 · Registrar el resultado de un partido interno — **Cumple** (prioridad alta)

- **Implementación:** Entrenador.registrarResultadoInterno · PartidoInterno.registrarResultado
- **Interfaz:** MainEmpleado 15
- **Brecha:** Proyecto 2: solo el entrenador que programó el partido, una vez disputado, con las dos alineaciones y sin resultado previo. Valida empates en baloncesto y voleibol, sets de voleibol y negativos. Todo se valida antes de modificar el estado.
- **Acción:** Hecho.
- **Prueba sugerida:** Resultado válido finaliza el partido; empate en baloncesto; voleibol 3-3; partido de otro entrenador; doble registro.

### HU-T07 · Registrar las estadísticas individuales de un partido — **Cumple** (prioridad media)

- **Implementación:** PartidoInterno.validarEstadistica · PartidoCompetencia.agregarEstadistica
- **Interfaz:** MainEmpleado 15/16
- **Brecha:** Proyecto 2: estadísticas solo para jugadores alineados, del tipo del deporte y no negativas (también en partidos oficiales).
- **Acción:** Hecho.
- **Prueba sugerida:** Estadísticas de fútbol en partido de fútbol; estadística de otro deporte; valor negativo; jugador no alineado.

### HU-T08 · Registrar las tarjetas de un partido de fútbol — **Cumple** (prioridad media)

- **Implementación:** PartidoInterno/PartidoCompetencia.registrarSancionDisciplinaria · Sancion
- **Interfaz:** MainEmpleado 15/16
- **Brecha:** Roja → suspensión; dos amarillas en un partido interno → suspensión; amarillas acumuladas en competencia → suspensión; una sola amarilla → sin sanción. Proyecto 2: rechaza tarjetas a jugadores no alineados.
- **Acción:** Hecho.
- **Prueba sugerida:** Roja → suspendido; 2 amarillas interno → suspendido; 1+1 amarillas en competencia → suspendido; 1 amarilla → sin sanción.

### HU-T09 · Registrar el resultado de un partido oficial — **Cumple** (prioridad alta)

- **Implementación:** Entrenador.registrarResultadoOficial · PartidoCompetencia.registrarMarcador
- **Interfaz:** MainEmpleado 16
- **Brecha:** Proyecto 2: solo el entrenador responsable registra el resultado, y solo sobre un partido programado. Cumple 3/1/0 en fútbol, prohibición de empates en baloncesto y voleibol, negativos y doble registro.
- **Acción:** Hecho.
- **Prueba sugerida:** Victoria suma 3; empate en fútbol 1 y 1; empate en baloncesto rechazado; entrenador no responsable rechazado.

### HU-T10 · Registrar resultado y estadísticas de partido de práctica — **Cumple** (prioridad alta)

- **Implementación:** Entrenador.registrarResultadoPractica · PartidoPractica.registrarResultado
- **Interfaz:** MainEmpleado 12
- **Brecha:** Proyecto 2: lo registra un entrenador (antes, el socio). Valida el marcador por disciplina (tenis y pádel al mejor de 3, tenis de mesa al mejor de 5), los sets iguales entre compañeros de dobles y el tipo de estadística. Actualiza el ELO de cada participante frente al promedio del lado rival.
- **Acción:** Hecho.
- **Prueba sugerida:** ELO actualizado al ganador y al perdedor; doble registro; marcador 2-2; estadística de otra disciplina.

### HU-A01 · Iniciar sesión como administrador — **Cumple** (prioridad media)

- **Implementación:** SistemaClub.autenticar
- **Interfaz:** MainAdministrador
- **Brecha:** Cumple. MainAdministrador solo admite administradores.
- **Acción:** Hecho. Verificado en HistoriasAdministradorTest.
- **Prueba sugerida:** Autenticación correcta; rechazo genérico.

### HU-A02 · Registrar un socio — **Parcial** (prioridad media)

- **Implementación:** Administrador.crearUsuario · SistemaClub.registrarUsuario · Socio(...)
- **Interfaz:** Menú admin 1
- **Brecha:** Login único, fecha futura y datos faltantes cumplen. No existe el campo documento ni su unicidad.
- **Acción:** Agregar documento a Socio y validar unicidad en SistemaClub.
- **Prueba sugerida:** Registro OK; login repetido; documento repetido; fecha futura.

### HU-A03 · Crear un equipo — **Parcial** (prioridad media)

- **Implementación:** Administrador.crearEquipo · SistemaClub.registrarEquipo
- **Interfaz:** Menú admin 4
- **Brecha:** Cupo > 0 cumple. El duplicado se valida solo por nombre (el banco dice nombre + deporte + categoría) y no se rechaza un cupo menor al mínimo de habilitados del deporte. Los deportes y categorías se crean en la consola, no en el modelo.
- **Acción:** Validar cupo ≥ mínimo del deporte, ajustar el criterio de duplicado y registrar deportes y categorías en SistemaClub.
- **Prueba sugerida:** Equipo creado activo y vacío; duplicado; cupo 0; cupo menor que el mínimo.

### HU-A04 · Inscribir a un socio en un equipo — **Cumple** (prioridad media)

- **Implementación:** Equipo.inscribirJugador
- **Interfaz:** Menú admin 6
- **Brecha:** Cumple: edad por categoría, equipo repetido en el deporte, camiseta, cupo, equipo inactivo y posición.
- **Acción:** Solo pruebas.
- **Prueba sugerida:** Éxito; edad 20 en Sub-15; segundo equipo del mismo deporte; camiseta repetida; equipo lleno; posición inválida.

### HU-A05 · Dar de baja a un jugador — **Cumple** (prioridad alta)

- **Implementación:** Administrador.darDeBajaJugador · Equipo.retirarJugador · Competencia.isEnCurso
- **Interfaz:** MainAdministrador 7
- **Brecha:** Proyecto 2: opción nueva en el menú. Libera cupo y camiseta y conserva el historial; se rechaza a un jugador en la nómina de una competencia en curso o ya dado de baja.
- **Acción:** Hecho.
- **Prueba sugerida:** Baja libera cupo y camiseta y conserva historial; jugador en nómina de competencia en curso; baja repetida.

### HU-A06 · Dar de baja un equipo — **Parcial** (prioridad media)

- **Implementación:** Equipo.darDeBaja
- **Interfaz:** Menú admin 5
- **Brecha:** Solo marca inactivo. No valida partidos pendientes ni competencia en curso, no rechaza un equipo ya inactivo y no libera a los integrantes.
- **Acción:** Validar compromisos, rechazar inactivo y retirar inscripciones conservando el historial.
- **Prueba sugerida:** Baja OK libera integrantes; equipo con partido programado; equipo inactivo.

### HU-A07 · Consultar el estado del inventario — **Parcial** (prioridad baja)

- **Implementación:** Administrador.estadoInventario
- **Interfaz:** Menú admin 12 → 1
- **Brecha:** Muestra la cantidad por ubicación. No muestra el total por artículo ni disciplina, capacidad y estado de las instalaciones, y no permite filtrar por ubicación (ni rechazar una inexistente).
- **Acción:** Enriquecer el reporte y agregar el filtro con validación.
- **Prueba sugerida:** Totales correctos; filtro por ubicación; ubicación inexistente.

### HU-A08 · Mover artículos entre ubicaciones — **Parcial** (prioridad media)

- **Implementación:** Administrador.moverInventario
- **Interfaz:** Menú admin 13
- **Brecha:** Origen = destino, cantidad ≤ 0 y existencia insuficiente cumplen. El destino inexistente se crea (el banco pide rechazarlo) y el mensaje no indica la existencia disponible.
- **Acción:** Catálogo fijo de ubicaciones (tienda, cafetería, bodega) y mensaje con la disponible.
- **Prueba sugerida:** Movimiento OK; cantidad mayor que la existencia (sin cambios); origen = destino; ubicación inexistente.

### HU-A09 · Reabastecer la tienda — **Parcial** (prioridad media)

- **Implementación:** Administrador.reabastecer
- **Interfaz:** Menú admin 13
- **Brecha:** Cantidad ≤ 0 cumple. La ubicación inexistente se crea en vez de rechazarse.
- **Acción:** Usar el mismo catálogo de ubicaciones de A08.
- **Prueba sugerida:** Aumenta la existencia; cantidad 0; ubicación inexistente.

### HU-A10 · Aprobar o rechazar sugerencias — **Parcial** (prioridad media)

- **Implementación:** Administrador.resolverSugerencia
- **Interfaz:** Menú admin 14
- **Brecha:** Aprobar → catálogo con cantidad 0 y rechazo de sugerencia ya resuelta cumplen. El rechazo no pide motivo y el empleado no puede verlo.
- **Acción:** Atributo motivo obligatorio al rechazar y visible para el empleado.
- **Prueba sugerida:** Aprobación entra al catálogo; rechazo con motivo; rechazo sin motivo; sugerencia ya resuelta.

### HU-A11 · Programar los turnos semanales — **Cumple** (prioridad alta)

- **Implementación:** ProgramacionSemanal.asignar/quitar/bloquesSinCobertura/publicar · Administrador.publicarProgramacion
- **Interfaz:** MainAdministrador 17
- **Brecha:** Proyecto 2: nueva clase ProgramacionSemanal, un borrador que parte de los turnos vigentes. Al publicar exige 2 entrenadores y 1 fisioterapeuta en todo instante de lunes a sábado, de 05:00 a 23:00; si falla, indica los bloques sin cobertura y no cambia ningún turno. Rechaza traslapes y turnos fuera del horario. Reemplaza la opción «Asignar turno», que no validaba la cobertura.
- **Acción:** Hecho.
- **Prueba sugerida:** Programación con cobertura OK; bloque sin fisioterapeuta rechazado con el bloque; traslape.

### HU-A12 · Resolver solicitudes de cambio de turno — **Cumple** (prioridad baja)

- **Implementación:** Administrador.resolverCambioTurno(solicitud, empleados, aprobar, motivo) · SolicitudCambioTurno.rechazar(motivo)
- **Interfaz:** MainAdministrador 18
- **Brecha:** Proyecto 2: la cobertura, la actualización de turnos y el rechazo de solicitudes ya resueltas ya cumplían. Ahora el rechazo por cobertura guarda el motivo, el rechazo manual exige motivo y el empleado lo ve (E04).
- **Acción:** Hecho.
- **Prueba sugerida:** Aprobación actualiza turnos; rechazo por cobertura con motivo y turnos intactos; solicitud ya resuelta.

### HU-A13 · Consultar el historial de reservas, partidos y sesiones — **Parcial** (prioridad media)

- **Implementación:** Administrador.historialReservas/Sesiones/PartidosInternos/PartidosCompetencia
- **Interfaz:** Menú admin 12 → 2/3/4
- **Brecha:** Lista todo sin rango de fechas ni filtros (socio, entrenador, disciplina). No incluye partidos de práctica ni rechaza un rango invertido.
- **Acción:** Métodos con rango y filtros y mensaje de periodo vacío.
- **Prueba sugerida:** Reservas en el rango; filtro por socio; periodo vacío; rango invertido.

### HU-A14 · Consultar reportes de ventas — **Parcial** (prioridad media)

- **Implementación:** Administrador.generarReporteVentas
- **Interfaz:** Menú admin 18
- **Brecha:** Rubro, periodo y el mensaje sin datos cumplen. Solo muestra el total; faltan número de ventas, neto, IVA, impuesto al consumo y propinas. Mensualidades siempre está vacío (A16).
- **Acción:** Clase de reporte con el desglose.
- **Prueba sugerida:** Desglose correcto de tienda; periodo sin ventas; periodo inválido.

### HU-A15 · Consultar reportes deportivos — **Parcial** (prioridad media)

- **Implementación:** Administrador.generarReporteDeportivo
- **Interfaz:** Menú admin 19
- **Brecha:** Muestra asistencia, resultados y ranking actual. No filtra por periodo ni muestra la evolución del ELO, porque no se guarda historial.
- **Acción:** Guardar el historial de ELO por partido y filtrar el reporte por periodo.
- **Prueba sugerida:** Evolución de ELO tras dos partidos; periodo sin datos.

### HU-A16 · Registrar el pago de una mensualidad — **Cumple** (prioridad alta)

- **Implementación:** PagoMensualidad · SistemaClub.registrarPagoMensualidad · Administrador.registrarPagoMensualidad
- **Interfaz:** MainAdministrador 20
- **Brecha:** Proyecto 2: nueva clase PagoMensualidad (rubro mensualidades). Rechaza mes duplicado, valor ≤ 0 y socio inexistente. Pendiente: incluir estos pagos en el reporte de ventas (A14).
- **Acción:** Hecho.
- **Prueba sugerida:** Pago registrado; mes duplicado; valor 0; socio inexistente.

### HU-A17 · Registrar una competencia — **Cumple** (prioridad media)

- **Implementación:** Administrador.crearCompetencia · SistemaClub.registrarCompetencia
- **Interfaz:** Menú admin 8
- **Brecha:** Cumple: datos obligatorios, formato liga o grupos y duplicado por nombre + temporada.
- **Acción:** Solo pruebas.
- **Prueba sugerida:** Creación OK; nombre y temporada duplicados; dato faltante.

### HU-A18 · Inscribir un equipo en una competencia — **Parcial** (prioridad media)

- **Implementación:** Competencia.agregarEquipo · registrarNomina · InscripcionCompetencia.registrarEnNomina
- **Interfaz:** Menú admin 9
- **Brecha:** Categoría o deporte, duplicado y nómina ajena cumplen. El mínimo de la nómina no descuenta suspendidos, no indica cuántos faltan y se valida en parte en la consola.
- **Acción:** Unificar inscripción + nómina en el modelo, descontando suspendidos y con mensaje de faltantes.
- **Prueba sugerida:** Inscripción OK; otra categoría; nómina con suspendidos bajo el mínimo (mensaje con faltantes); nómina ajena; duplicado.

### HU-A19 · Programar un partido oficial — **Cumple** (prioridad alta)

- **Implementación:** PartidoCompetencia.programar · Administrador.programarPartidoOficial
- **Interfaz:** MainAdministrador 12
- **Brecha:** Proyecto 2 (decisión del grupo): el calendario sigue generándose automáticamente, pero cada partido queda «propuesto» hasta que el administrador le da el visto bueno con fecha, escenario y entrenador responsable. La institución rival es el otro equipo del partido. Se rechazan datos faltantes y partidos sin competencia o con equipos no inscritos.
- **Acción:** Hecho.
- **Prueba sugerida:** Programación OK; equipo no inscrito; falta rival, escenario o entrenador.

### HU-A20 · Restricción de uso personal del administrador — **Cumple** (prioridad baja)

- **Implementación:** Por diseño: Administrador no es Socio y no tiene reservarInstalacion ni participación en partidos
- **Interfaz:** Sin opciones en el menú
- **Brecha:** Cumple por construcción de tipos. Hay que documentarlo como decisión de diseño.
- **Acción:** Documentar y probar.
- **Prueba sugerida:** El administrador no puede ser participante ni responsable de una reserva (no compila o lanza excepción, según el diseño).
