package club.consola;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import club.modelo.competencias.*;
import club.modelo.entrenamiento.*;
import club.modelo.instalaciones.*;
import club.modelo.tienda.*;
import club.modelo.usuarios.*;

/** Aplicación de consola para el administrador del club. */
public class MainAdministrador extends ConsolaBase {

    public static void main(String[] args) {
        new MainAdministrador().iniciar();
    }

    @Override protected String nombreAplicacion() { return "administración"; }

    @Override protected boolean puedeIngresar(Usuario usuario) { return usuario instanceof Administrador; }

    @Override
    protected Menu construirMenu(Usuario usuario) {
        Administrador admin = (Administrador) usuario;
        return new Menu("Menú del administrador")
                .seccion("Usuarios")
                .opcion("1", "Crear usuario", () -> crearUsuario(admin))
                .opcion("2", "Dar de baja usuario", () -> darDeBajaUsuario(admin))
                .opcion("3", "Listar usuarios", this::listarUsuarios)
                .seccion("Equipos")
                .opcion("4", "Crear equipo", () -> crearEquipo(admin))
                .opcion("5", "Dar de baja equipo", () -> darDeBajaEquipo(admin))
                .opcion("6", "Inscribir jugador en equipo", () -> inscribirJugador(admin))
                .opcion("7", "Dar de baja a un jugador", () -> darDeBajaJugador(admin))
                .opcion("8", "Listar equipos", this::listarEquipos)
                .seccion("Competencias")
                .opcion("9", "Registrar competencia", () -> crearCompetencia(admin))
                .opcion("10", "Inscribir equipo y nómina", this::inscribirEnCompetencia)
                .opcion("11", "Calendario y fase de eliminación", this::gestionarCalendario)
                .opcion("12", "Programar partido oficial (visto bueno)", () -> programarPartidoOficial(admin))
                .opcion("13", "Tabla de posiciones y clasificaciones", this::verCompetencia)
                .seccion("Inventario")
                .opcion("14", "Consultar inventario e historial", () -> consultarInventario(admin))
                .opcion("15", "Mover o reabastecer artículos", () -> gestionarInventario(admin))
                .opcion("16", "Resolver sugerencias de productos", () -> resolverSugerencias(admin))
                .seccion("Turnos")
                .opcion("17", "Programar turnos semanales", () -> programarTurnos(admin))
                .opcion("18", "Resolver solicitudes de cambio de turno", () -> resolverSolicitudes(admin))
                .opcion("19", "Ver turnos", this::listarTurnos)
                .seccion("Pagos y reportes")
                .opcion("20", "Registrar pago de mensualidad", () -> registrarPagoMensualidad(admin))
                .opcion("21", "Reporte de ventas", () -> reporteVentas(admin))
                .opcion("22", "Reporte deportivo", () -> reporteDeportivo(admin));
    }

    private void crearUsuario(Administrador admin) {
        String tipo = texto("Tipo (socio/entrenador/fisioterapeuta/administrador): ").toLowerCase();
        String login = texto("Login: ");
        String nombre = texto("Nombre: ");
        String password = texto("Contraseña: ");
        Usuario nuevo;
        switch (tipo) {
            case "socio": {
                Socio socio = new Socio(login, password, nombre, fecha("Fecha de nacimiento (AAAA-MM-DD): "));
                socio.setDocumento(texto("Documento: "));
                nuevo = socio;
                break;
            }
            case "entrenador": nuevo = new Entrenador(login, password, nombre, texto("Código de descuento: ")); break;
            case "fisioterapeuta": nuevo = new Fisioterapeuta(login, password, nombre, texto("Código de descuento: ")); break;
            case "administrador": nuevo = new Administrador(login, password, nombre); break;
            default: throw new IllegalArgumentException("Tipo de usuario no reconocido.");
        }
        admin.crearUsuario(nuevo, sistema);
        System.out.println("Usuario creado.");
    }

    private void darDeBajaUsuario(Administrador admin) {
        List<Usuario> candidatos = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values()) if (u != admin) candidatos.add(u);
        Usuario u = elegir("Usuario a dar de baja:", candidatos, x -> x.getLogin() + " - " + x.getNombre() + " (" + Formatos.nombreRol(x) + ")");
        sistema.darDeBajaUsuario(u.getLogin());
        System.out.println("Usuario dado de baja.");
    }

    private void listarUsuarios() {
        for (Usuario u : sistema.getUsuarios().values()) System.out.printf("%-15s %-30s %s%n", u.getLogin(), u.getNombre(), Formatos.nombreRol(u));
    }

    private void crearEquipo(Administrador admin) {
        String nombre = texto("Nombre del equipo: ");
        DeporteConjunto deporte = sistema.obtenerDeporte(elegir("Deporte:", Arrays.asList("Fútbol", "Baloncesto", "Voleibol"), x -> x));
        Categoria categoria = sistema.obtenerCategoria(elegir("Categoría:", Arrays.asList("Sub-13", "Sub-15", "Sub-17", "Mayores"), x -> x));
        int cupo = entero("Cupo máximo de jugadores: ");
        Equipo e = admin.crearEquipo(nombre, deporte, categoria, cupo, sistema);
        System.out.println("Equipo creado: " + Formatos.etiquetaEquipo(e));
    }

    private void darDeBajaEquipo(Administrador admin) {
        Equipo e = elegir("Equipo a dar de baja:", sistema.getEquiposActivos(), Formatos::etiquetaEquipo);
        admin.darDeBajaEquipo(e, sistema);
        System.out.println("Equipo dado de baja (se conserva su historial).");
    }

    private void inscribirJugador(Administrador admin) {
        Equipo e = elegir("Equipo:", sistema.getEquiposActivos(), Formatos::etiquetaEquipo);
        Socio s = elegir("Socio:", sistema.getSocios(), x -> x.getNombre() + " (" + x.getLogin() + ", " + x.calcularEdad(LocalDate.now()) + " años)");
        String posicion = texto("Posición: ");
        int camiseta = entero("Número de camiseta: ");
        admin.inscribirJugadorEnEquipo(s, e, posicion, camiseta);
        System.out.println("Jugador inscrito.");
    }

    private void listarEquipos() {
        for (Equipo e : sistema.getEquipos()) {
            System.out.println(Formatos.etiquetaEquipo(e) + (e.isActivo() ? "" : " [de baja]"));
            for (InscripcionEquipo i : e.getInscripciones())
                System.out.printf("     #%d %s (%s)%s%n", i.getNumeroCamiseta(), i.getSocio().getNombre(), i.getPosicion(), i.isActiva() ? "" : " [retirado]");
        }
    }

    private void crearCompetencia(Administrador admin) {
        String nombre = texto("Nombre (ej. ASCUN, Torneo de los Cerros): ");
        String organizador = texto("Entidad organizadora: ");
        String temporada = texto("Año o temporada: ");
        DeporteConjunto deporte = sistema.obtenerDeporte(elegir("Deporte:", Arrays.asList("Fútbol", "Baloncesto", "Voleibol"), x -> x));
        Categoria categoria = sistema.obtenerCategoria(elegir("Categoría habilitada:", Arrays.asList("Sub-13", "Sub-15", "Sub-17", "Mayores"), x -> x));
        ReglaPuntuacion regla = deporte instanceof Futbol ? new ReglaFutbol(3, 0)
                : deporte instanceof Baloncesto ? new ReglaBaloncesto(2, 0) : new ReglaVoleibol(2, 0);
        Formato formato;
        if (entero("Formato: 1) Liga todos contra todos   2) Fase de grupos + eliminación directa: ") == 1) {
            formato = new FormatoLiga(nombre, regla);
        } else {
            int grupos = entero("Número de grupos: ");
            int porGrupo = entero("Equipos por grupo: ");
            FormatoPorFases fases = new FormatoPorFases(nombre);
            FaseGrupos fg = new FaseGrupos(regla);
            for (int i = 0; i < grupos; i++) fg.getGrupos().add(new Grupo("Grupo " + (char) ('A' + i), porGrupo));
            fases.setFaseGrupos(fg);
            fases.setFaseEliminacion(new FaseEliminacion());
            formato = fases;
        }
        admin.crearCompetencia(nombre, organizador, temporada, deporte, categoria, formato, sistema);
        System.out.println("Competencia creada.");
    }

    private void inscribirEnCompetencia() {
        Competencia c = elegir("Competencia:", sistema.getCompetencias(), x -> x.getNombre() + " " + x.getTemporada());
        if (entero("1) Equipo del club   2) Institución rival: ") == 2) {
            String nombre = texto("Nombre de la institución: ");
            EquipoCompetencia rival = null;
            for (EquipoCompetencia e : c.getEquipos()) if (e instanceof InstitucionRival && e.getNombre().equalsIgnoreCase(nombre)) rival = e;
            c.agregarEquipo(rival != null ? rival : new InstitucionRival(nombre));
            System.out.println("Institución inscrita.");
            return;
        }
        List<Equipo> elegibles = new ArrayList<>();
        for (Equipo e : sistema.getEquiposActivos())
            if (e.getDeporte() == c.getDeporte() && e.getCategoria() == c.getCategoria() && !c.getEquipos().contains(e)) elegibles.add(e);
        Equipo equipo = elegir("Equipos elegibles (mismo deporte y categoría):", elegibles, Formatos::etiquetaEquipo);
        List<Socio> disponibles = new ArrayList<>();
        for (InscripcionEquipo i : equipo.getInscripcionesActivas()) disponibles.add(i.getSocio());
        for (Socio s : disponibles) System.out.println("  - " + s.getLogin() + " (" + s.getNombre() + ")");
        String respuesta = texto("Nómina: logins separados por coma, o 'todos': ");
        List<Socio> nomina = new ArrayList<>();
        if (respuesta.equalsIgnoreCase("todos")) nomina.addAll(disponibles);
        else for (String login : respuesta.split(",")) {
            Socio s = null;
            for (Socio x : disponibles) if (x.getLogin().equalsIgnoreCase(login.trim())) s = x;
            if (s == null) throw new IllegalArgumentException("El socio " + login.trim() + " no pertenece al equipo.");
            nomina.add(s);
        }
        c.inscribirEquipoConNomina(equipo, nomina);
        System.out.println("Equipo inscrito con una nómina de " + nomina.size() + " jugadores.");
    }

    private void gestionarCalendario() {
        Competencia c = elegir("Competencia:", sistema.getCompetencias(), x -> x.getNombre() + " " + x.getTemporada());
        int op = entero("1) Generar calendario   2) Generar fase de eliminación   3) Avanzar ronda de eliminación: ");
        if (op == 1) {
            c.generarCalendario();
            System.out.println("Calendario generado: " + c.getPartidos().size() + " partidos.");
        } else if (op == 2) {
            List<PartidoCompetencia> nuevos = c.generarFaseEliminacion(entero("Clasificados por grupo: "));
            System.out.println("Fase de eliminación generada: " + nuevos.size() + " partidos.");
        } else if (op == 3) {
            RondaEliminacion ronda = c.avanzarFaseEliminacion();
            if (ronda == null) System.out.println("Campeón: " + ((FormatoPorFases) c.getFormato()).getFaseEliminacion().getCampeon().getNombre());
            else System.out.println("Nueva ronda generada: " + ronda.getNombre() + " con " + ronda.getPartidos().size() + " partidos.");
        } else throw new IllegalArgumentException("Opción no válida.");
    }

    private void verCompetencia() {
        Competencia c = elegir("Competencia:", sistema.getCompetencias(), x -> x.getNombre() + " " + x.getTemporada());
        System.out.printf("%s (%s) | %s | %s | formato: %s%n", c.getNombre(), c.getOrganizador(), c.getDeporte().getNombre(),
                c.getCategoria().getNombre(), c.getFormato().getNombreFormato());
        if (c.getFormato() instanceof FormatoLiga) {
            FormatoLiga liga = (FormatoLiga) c.getFormato();
            liga.actualizarPosiciones();
            imprimirTabla("Tabla de posiciones", liga.getPosiciones());
        } else if (c.getFormato() instanceof FormatoPorFases) {
            FormatoPorFases fases = (FormatoPorFases) c.getFormato();
            if (fases.getFaseGrupos() != null) for (Grupo g : fases.getFaseGrupos().getGrupos()) {
                g.actualizarPosiciones();
                imprimirTabla(g.getNombre(), g.getPosiciones());
            }
            if (fases.getFaseEliminacion() != null) {
                for (RondaEliminacion r : fases.getFaseEliminacion().getRondas()) {
                    System.out.println(r.getNombre() + ":");
                    for (PartidoCompetencia p : r.getPartidos()) System.out.println("   " + Formatos.nombresEquipos(p) + " | " + p.getMarcador());
                }
                if (fases.getFaseEliminacion().getCampeon() != null) System.out.println("Campeón: " + fases.getFaseEliminacion().getCampeon().getNombre());
            }
        }
        System.out.println("--- Partidos ---");
        for (PartidoCompetencia p : c.getPartidos())
            System.out.printf("%s | %s | %s | escenario: %s%n", p.getFecha() == null ? "sin fecha" : p.getFecha().toString(), Formatos.nombresEquipos(p), p.getMarcador(), p.getEscenario());
        if (c.getDeporte() instanceof Futbol) {
            System.out.println("--- Goleadores ---");
            for (Map.Entry<Socio, Integer> e : c.clasificacionGoleadores()) System.out.printf("%s: %d%n", e.getKey().getNombre(), e.getValue());
        }
        System.out.println("--- Jugadores más destacados ---");
        for (Map.Entry<Socio, Integer> e : c.clasificacionDestacados()) System.out.printf("%s: %d%n", e.getKey().getNombre(), e.getValue());
    }

    private void imprimirTabla(String titulo, List<PosicionEquipo> posiciones) {
        System.out.println("--- " + titulo + " ---");
        System.out.println("Pos Equipo                         PJ  G  E  P  GF  GC  Pts");
        for (PosicionEquipo p : posiciones)
            System.out.printf("%-3d %-30s %2d %2d %2d %2d %3d %3d %4d%n", p.getPosicion(), p.getEquipo().getNombre(), p.getPartidosJugados(),
                    p.getVictorias(), p.getEmpates(), p.getDerrotas(), p.getTantosFavor(), p.getTantosContra(), p.getPuntuacion());
    }

    private void consultarInventario(Administrador admin) {
        int op = entero("1) Inventario   2) Reservas   3) Partidos   4) Sesiones de entrenamiento   5) Partidos de práctica: ");
        if (op == 1) {
            System.out.println(admin.estadoInventario(sistema, textoOpcional("Ubicación (tienda/cafeteria/bodega, Enter = todas): ")));
            return;
        }
        if (op < 2 || op > 5) throw new IllegalArgumentException("Opción no válida.");
        LocalDate desde = fecha("Desde (AAAA-MM-DD): ");
        LocalDate hasta = fecha("Hasta (AAAA-MM-DD): ");
        boolean vacio = true;
        if (op == 2) {
            for (Reserva r : admin.historialReservas(sistema, desde, hasta, socioOpcional(), disciplinaOpcional())) {
                vacio = false;
                System.out.printf("%s a %s | %s | %s | %d jugadores%n", r.getFechaHoraInicio(), r.getFechaHoraFin(),
                        r.getInstalacion().getNombre(), r.getSocioResponsable() == null ? "-" : r.getSocioResponsable().getNombre(), r.getCantidadJugadores());
            }
        } else if (op == 3) {
            Entrenador e = entrenadorOpcional();
            for (PartidoInterno p : admin.historialPartidosInternos(sistema, desde, hasta, e)) {
                vacio = false;
                System.out.printf("[Interno] %s | %s vs %s | %s%n", p.getFechaHora(), p.getEquipoLocal().getNombre(), p.getEquipoVisitante().getNombre(), p.getMarcadorFinal());
            }
            for (PartidoCompetencia p : admin.historialPartidosCompetencia(sistema, desde, hasta, e)) {
                vacio = false;
                System.out.printf("[Competencia] %s | %s | %s%n", p.getFecha(), Formatos.nombresEquipos(p), p.getMarcador());
            }
        } else if (op == 4) {
            Entrenador e = entrenadorOpcional();
            for (SesionEntrenamiento s : admin.historialSesiones(sistema, desde, hasta, e, disciplinaOpcional())) {
                vacio = false;
                System.out.printf("%s | %s | entrenador: %s | %d convocados%n", s.getFechaHora(),
                        s.getInstalacion().getNombre(), s.getEntrenador().getNombre(), s.getConvocados().size());
            }
        } else {
            for (PartidoPractica p : admin.historialPracticas(sistema, desde, hasta, socioOpcional(), disciplinaOpcional())) {
                vacio = false;
                System.out.printf("[Práctica] %s | %s | sets %d-%d%n", p.getFechaHora(), Formatos.etiquetaPractica(p), p.getSetsLadoA(), p.getSetsLadoB());
            }
        }
        if (vacio) System.out.println("No hay registros en ese periodo.");
    }

    private Socio socioOpcional() {
        String login = textoOpcional("Filtrar por socio (login, Enter = todos): ");
        if (login.isEmpty()) return null;
        Usuario u = sistema.buscarUsuario(login);
        if (!(u instanceof Socio)) throw new IllegalArgumentException("No existe un socio con ese login.");
        return (Socio) u;
    }

    private Entrenador entrenadorOpcional() {
        String login = textoOpcional("Filtrar por entrenador (login, Enter = todos): ");
        if (login.isEmpty()) return null;
        Usuario u = sistema.buscarUsuario(login);
        if (!(u instanceof Entrenador)) throw new IllegalArgumentException("No existe un entrenador con ese login.");
        return (Entrenador) u;
    }

    private DisciplinaIndividual disciplinaOpcional() {
        String nombre = textoOpcional("Filtrar por disciplina (Enter = todas): ");
        if (nombre.isEmpty()) return null;
        for (DisciplinaIndividual d : sistema.getDisciplinas()) if (Formatos.nombreDisciplina(d).equalsIgnoreCase(nombre)) return d;
        throw new IllegalArgumentException("No existe la disciplina " + nombre + ".");
    }

    private void gestionarInventario(Administrador admin) {
        Producto p = elegir("Producto:", sistema.getProductos(), x -> x.getNombre() + " (total: " + x.getCantidadTotal() + ")");
        for (Existencia e : p.getExistencias()) System.out.printf("   %s: %d%n", e.getUbicacionInventario(), e.getCantidad());
        if (entero("1) Mover entre ubicaciones   2) Reabastecer: ") == 1) {
            admin.moverInventario(p, texto("Ubicación de origen: "), texto("Ubicación de destino: "), entero("Cantidad: "));
            System.out.println("Inventario movido.");
        } else {
            admin.reabastecer(p, texto("Ubicación (tienda/cafeteria/bodega): "), entero("Cantidad: "));
            System.out.println("Inventario reabastecido.");
        }
    }

    private void resolverSugerencias(Administrador admin) {
        List<SugerenciaProducto> pendientes = new ArrayList<>();
        for (Empleado e : sistema.getEmpleados()) for (SugerenciaProducto s : e.getSugerencias()) if ("Pendiente".equalsIgnoreCase(s.getEstado())) pendientes.add(s);
        SugerenciaProducto s = elegir("Sugerencias pendientes:", pendientes,
                x -> x.getProducto().getNombre() + " - $" + (long) x.getProducto().getPrecio() + " (" + x.getProducto().getClass().getSimpleName() + ")");
        boolean aprobar = confirmar("¿Aprobar la sugerencia?");
        if (aprobar) {
            admin.resolverSugerencia(s, true, sistema);
            System.out.println("Aprobada: el producto entró al catálogo con cantidad 0 (reabastézcalo desde la opción 15).");
        } else {
            admin.rechazarSugerencia(s, texto("Motivo del rechazo: "));
            System.out.println("Sugerencia rechazada. El empleado puede ver el motivo.");
        }
    }

    /** HU-A11: edita un borrador de la programación (parte de los turnos vigentes) y lo publica si cumple la cobertura. */
    private void programarTurnos(Administrador admin) {
        ProgramacionSemanal p = new ProgramacionSemanal(sistema.getEmpleados());
        System.out.println("Horario de atención: " + Turno.descripcionHorarioDeAtencion() + ". Cobertura mínima: "
                + ProgramacionSemanal.MIN_ENTRENADORES + " entrenadores y " + ProgramacionSemanal.MIN_FISIOTERAPEUTAS + " fisioterapeuta.");
        while (true) {
            System.out.println("\n--- Programación en edición ---");
            for (Map.Entry<Empleado, List<Turno>> e : p.getBorrador().entrySet()) {
                List<String> turnos = new ArrayList<>();
                for (Turno t : e.getValue()) turnos.add(t.toString());
                System.out.printf("  %s (%s): %s%n", e.getKey().getNombre(), Formatos.nombreRol(e.getKey()), turnos.isEmpty() ? "sin turnos" : String.join(", ", turnos));
            }
            List<String> faltantes = p.bloquesSinCobertura();
            System.out.println(faltantes.isEmpty() ? "Cobertura completa." : "Bloques sin cobertura: " + faltantes.size() + " (opción 3 para verlos).");
            System.out.println("1) Asignar turno   2) Quitar turno   3) Ver bloques sin cobertura   4) Publicar   0) Salir sin publicar");
            int op = entrada.enteroEnRango("Opción: ", 0, 4);
            if (op == 0) { System.out.println("No se publicó la programación; los turnos vigentes no cambiaron."); return; }
            try {
                if (op == 1 || op == 2) {
                    Empleado e = elegir("Empleado:", new ArrayList<>(p.getBorrador().keySet()), x -> x.getNombre() + " (" + Formatos.nombreRol(x) + ")");
                    Turno t = new Turno(dia("Día (lunes..sábado): "), hora("Hora de inicio (HH:mm): "), hora("Hora de fin (HH:mm): "));
                    if (op == 1) p.asignar(e, t); else p.quitar(e, t);
                } else if (op == 3) {
                    System.out.println(faltantes.isEmpty() ? "No hay bloques sin cobertura." : String.join("\n", faltantes));
                } else {
                    admin.publicarProgramacion(p);
                    System.out.println("Programación publicada.");
                    return;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private void resolverSolicitudes(Administrador admin) {
        List<Empleado> empleados = admin.listarEmpleados(sistema);
        List<SolicitudCambioTurno> pendientes = admin.consultarSolicitudesPendientes(empleados);
        SolicitudCambioTurno s = elegir("Solicitudes pendientes:", pendientes,
                x -> x.getSolicitante().getNombre() + ": " + x.getTurnoActual() + " -> " + x.getTurnoDeseado());
        boolean aprobar = confirmar("¿Aprobar la solicitud?");
        String motivo = aprobar ? null : texto("Motivo del rechazo: ");
        admin.resolverCambioTurno(s, empleados, aprobar, motivo);
        System.out.println("Estado final de la solicitud: " + s.getEstado() + (s.getMotivoRechazo() == null ? "" : " (" + s.getMotivoRechazo() + ")"));
    }

    private void listarTurnos() {
        for (Empleado e : sistema.getEmpleados()) {
            System.out.println(e.getNombre() + " (" + Formatos.nombreRol(e) + "):");
            if (e.getTurnos().isEmpty()) System.out.println("   sin turnos");
            for (Turno t : e.getTurnos()) System.out.println("   " + Formatos.etiquetaTurno(t));
        }
    }

    private void reporteVentas(Administrador admin) {
        String rubro = elegir("Rubro:", Arrays.asList("tienda", "cafeteria", "mensualidad"), x -> x);
        String periodo = elegir("Periodo:", Arrays.asList("diario", "semanal", "mensual"), x -> x);
        String ref = textoOpcional("Fecha de referencia AAAA-MM-DD (Enter para hoy): ");
        LocalDate fecha = ref.isEmpty() ? club.model.Reloj.ahora().toLocalDate() : LocalDate.parse(ref);
        System.out.println(admin.reporteVentasDetallado(sistema, rubro, periodo, fecha));
    }

    private void reporteDeportivo(Administrador admin) {
        LocalDate desde = fecha("Desde (AAAA-MM-DD): ");
        LocalDate hasta = fecha("Hasta (AAAA-MM-DD): ");
        System.out.println(admin.reporteDeportivo(sistema, desde, hasta));
    }

    // ===================================================================== PROYECTO 2
    /** HU-A19: el calendario propone los partidos; el administrador les da el visto bueno. */
    private void programarPartidoOficial(Administrador admin) {
        List<PartidoCompetencia> propuestos = new ArrayList<>();
        for (Competencia c : sistema.getCompetencias())
            for (PartidoCompetencia p : c.getPartidos())
                if (!p.isFinalizado() && p.getEquipos().size() == 2) propuestos.add(p);
        PartidoCompetencia p = elegir("Partidos del calendario sin resultado:", propuestos,
                x -> x.getCompetencia().getNombre() + " | " + Formatos.nombresEquipos(x)
                        + (x.isProgramado() ? " | programado " + x.getFecha() + " en " + x.getEscenario() + " (" + x.getEntrenadorResponsable().getNombre() + ")" : " | propuesto"));
        LocalDate fecha = fecha("Fecha (AAAA-MM-DD): ");
        String escenario = texto("Escenario: ");
        Entrenador responsable = elegir("Entrenador responsable:", sistema.getEntrenadores(), Formatos::persona);
        admin.programarPartidoOficial(p, fecha, escenario, responsable);
        System.out.printf("Partido programado: %s el %s en %s. Responsable: %s.%n", Formatos.nombresEquipos(p), fecha, escenario, responsable.getNombre());
    }

    /** HU-A16 */
    private void registrarPagoMensualidad(Administrador admin) {
        Socio socio = elegir("Socio:", sistema.getSocios(), Formatos::persona);
        int anio = entrada.enteroEnRango("Año: ", 2000, 2100);
        int mes = entrada.enteroEnRango("Mes (1-12): ", 1, 12);
        double valor = decimal("Valor pagado: ");
        PagoMensualidad pago = admin.registrarPagoMensualidad(sistema, socio.getLogin(), java.time.YearMonth.of(anio, mes), valor);
        System.out.printf("Pago registrado: %s, %s, $%.0f.%n", socio.getNombre(), pago.getMes(), pago.getValor());
    }

    /** HU-A05 */
    private void darDeBajaJugador(Administrador admin) {
        Equipo e = elegir("Equipo:", sistema.getEquiposActivos(), Formatos::etiquetaEquipo);
        List<Socio> activos = new ArrayList<>();
        for (InscripcionEquipo i : e.getInscripcionesActivas()) activos.add(i.getSocio());
        Socio jugador = elegir("Jugador a dar de baja:", activos, Formatos::persona);
        admin.darDeBajaJugador(sistema, e, jugador);
        System.out.println(jugador.getNombre() + " fue dado de baja de " + e.getNombre() + "; su cupo y su camiseta quedan libres.");
    }
}
