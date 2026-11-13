package club.modelo.usuarios;

import java.time.LocalDate;
import java.time.YearMonth;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.competencias.PartidoInterno;
import club.modelo.entrenamiento.PartidoPractica;
import club.modelo.entrenamiento.SesionEntrenamiento;
import club.modelo.instalaciones.Instalacion;
import club.modelo.instalaciones.Reserva;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.modelo.competencias.Categoria;
import club.modelo.competencias.Competencia;
import club.modelo.competencias.DeporteConjunto;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.Formato;
import club.modelo.competencias.InscripcionCompetencia;
import club.modelo.competencias.InscripcionEquipo;
import club.model.SistemaClub;
import club.modelo.tienda.PagoMensualidad;
import club.modelo.tienda.Existencia;
import club.modelo.tienda.Producto;
import club.modelo.tienda.SugerenciaProducto;
import club.modelo.tienda.Transaccion;

public class Administrador extends Usuario implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private List<SugerenciaProducto> sugerenciasAprobadas = new ArrayList<>();
    private List<SolicitudCambioTurno> solicitudesAprobadas = new ArrayList<>();
    private List<Producto> productosCatalogo = new ArrayList<>();

    public Administrador(String login, String password, String nombre) { super(login, password, nombre); }

    public void crearUsuario(Usuario usuario, List<Usuario> existentes) {
        if (usuario == null || existentes == null) throw new IllegalArgumentException("El usuario y el registro son obligatorios.");
        for (Usuario existente : existentes) {
            if (existente.getLogin().equalsIgnoreCase(usuario.getLogin())) throw new IllegalArgumentException("El login ya está en uso.");
        }
        existentes.add(usuario);
    }

    public void crearUsuario(Usuario usuario, SistemaClub sistema) {
        if (sistema == null) throw new IllegalArgumentException("El sistema es obligatorio.");
        sistema.registrarUsuario(usuario);
    }

    public void darDeBajaUsuario(Usuario usuario, List<Usuario> existentes) {
        if (existentes == null || usuario == null || !existentes.remove(usuario)) throw new IllegalArgumentException("El usuario no está registrado.");
    }

    public Equipo crearEquipo(String nombre, DeporteConjunto deporte, Categoria categoria, int cupo) {
        return new Equipo(nombre, deporte, categoria, cupo);
    }

    public Equipo crearEquipo(String nombre, DeporteConjunto deporte, Categoria categoria, int cupo, SistemaClub sistema) {
        if (sistema == null) throw new IllegalArgumentException("El sistema es obligatorio.");
        Equipo equipo = crearEquipo(nombre, deporte, categoria, cupo);
        sistema.registrarEquipo(equipo);
        return equipo;
    }

    /**
     * Da de baja a un jugador de su equipo (HU-A05): libera cupo y camiseta y conserva el historial. Se rechaza si el
     * jugador está en la nómina de una competencia en curso o si ya no está activo en el equipo.
     */
    public void darDeBajaJugador(SistemaClub sistema, Equipo equipo, Socio jugador) {
        if (sistema == null || equipo == null || jugador == null) throw new IllegalArgumentException("El equipo y el jugador son obligatorios.");
        for (Competencia c : sistema.getCompetencias())
            for (InscripcionCompetencia i : c.getInscripciones())
                if (i.getEquipo() == equipo && i.enNomina(jugador) && c.isEnCurso())
                    throw new IllegalStateException(jugador.getNombre() + " está en la nómina de la competencia en curso " + c.getNombre() + ".");
        equipo.retirarJugador(jugador);
    }

    public void darDeBajaEquipo(Equipo equipo) {
        if (equipo == null) throw new IllegalArgumentException("El equipo es obligatorio.");
        equipo.darDeBaja();
    }

    public void darDeBajaEquipo(Equipo equipo, SistemaClub sistema) {
        if (equipo == null || sistema == null) throw new IllegalArgumentException("El equipo y el sistema son obligatorios.");
        for (Competencia c : sistema.getCompetencias())
            for (InscripcionCompetencia i : c.getInscripciones())
                if (i.getEquipo() == equipo && i.isActiva() && c.isEnCurso())
                    throw new IllegalStateException("El equipo está inscrito en la competencia en curso " + c.getNombre() + ".");
        equipo.darDeBaja();
    }
    public InscripcionEquipo inscribirJugadorEnEquipo(Socio socio, Equipo equipo, String posicion, int camiseta) {
        if (equipo == null) throw new IllegalArgumentException("El equipo es obligatorio.");
        return equipo.inscribirJugador(socio, posicion, camiseta);
    }

    public Competencia crearCompetencia(String nombre, String organizador, String temporada,
                                        DeporteConjunto deporte, Categoria categoria, Formato formato) {
        return new Competencia(nombre, organizador, temporada, deporte, categoria, formato);
    }

    public Competencia crearCompetencia(String nombre, String organizador, String temporada,
                                        DeporteConjunto deporte, Categoria categoria, Formato formato, SistemaClub sistema) {
        if (sistema == null) throw new IllegalArgumentException("El sistema es obligatorio.");
        Competencia competencia = crearCompetencia(nombre, organizador, temporada, deporte, categoria, formato);
        sistema.registrarCompetencia(competencia);
        return competencia;
    }

    /** Registra el pago mensual de un socio identificado por su login (HU-A16). */
    public PagoMensualidad registrarPagoMensualidad(SistemaClub sistema, String loginSocio, YearMonth mes, double valor) {
        if (sistema == null) throw new IllegalArgumentException("El sistema es obligatorio.");
        Usuario usuario = sistema.buscarUsuario(loginSocio);
        if (!(usuario instanceof Socio)) throw new IllegalArgumentException("No existe un socio con el login " + loginSocio + ".");
        return sistema.registrarPagoMensualidad((Socio) usuario, mes, valor);
    }

    /**
     * Da el visto bueno a un partido que propuso el calendario (HU-A19): fija fecha, escenario y entrenador
     * responsable. Solo ese entrenador podrá registrar el resultado (HU-T09).
     */
    public void programarPartidoOficial(PartidoCompetencia partido, LocalDate fecha, String escenario, Entrenador responsable) {
        if (partido == null) throw new IllegalArgumentException("El partido es obligatorio.");
        partido.programar(fecha, escenario, responsable);
    }

    public void moverInventario(Producto producto, String origen, String destino, int cantidad) {
    	validarUbicacion(origen);
    	validarUbicacion(destino);
        if (producto == null || origen == null || destino == null || origen.equalsIgnoreCase(destino) || cantidad <= 0) {
            throw new IllegalArgumentException("Producto, ubicaciones distintas y cantidad positiva son obligatorios.");
        }
        Existencia fuente = producto.buscarExistencia(origen);
        if (fuente == null || !fuente.hayDisponible(cantidad)) throw new IllegalStateException("Existencia insuficiente en origen. Disponible: " + (fuente == null ? 0 : fuente.getCantidad()));
        Existencia destinoExistente = producto.buscarExistencia(destino);
        fuente.disminuir(cantidad);
        if (destinoExistente == null) producto.agregarExistencia(new Existencia(cantidad, destino));
        else destinoExistente.aumentar(cantidad);
    }

    public void reabastecer(Producto producto, String ubicacion, int cantidad) {
    	validarUbicacion(ubicacion);
        if (producto == null || ubicacion == null || cantidad <= 0) throw new IllegalArgumentException("Producto, ubicación y cantidad positiva son obligatorios.");
        Existencia existencia = producto.buscarExistencia(ubicacion);
        if (existencia == null) producto.agregarExistencia(new Existencia(cantidad, ubicacion));
        else existencia.aumentar(cantidad);
    }

    public void resolverSugerencia(SugerenciaProducto sugerencia, boolean aprobar) {
        if (sugerencia == null || sugerencia.getProducto() == null) throw new IllegalArgumentException("La sugerencia y el producto son obligatorios.");
        if (!"Pendiente".equalsIgnoreCase(sugerencia.getEstado())) throw new IllegalStateException("La sugerencia ya fue resuelta.");
        if (aprobar) {
            sugerencia.aprobar();
            if (!sugerenciasAprobadas.contains(sugerencia)) sugerenciasAprobadas.add(sugerencia);
            if (!productosCatalogo.contains(sugerencia.getProducto())) productosCatalogo.add(sugerencia.getProducto());
        } else sugerencia.rechazar();
    }

    public void resolverSugerencia(SugerenciaProducto sugerencia, boolean aprobar, SistemaClub sistema) {
        if (sistema == null || sugerencia == null || sugerencia.getProducto() == null) throw new IllegalArgumentException("El sistema, la sugerencia y el producto son obligatorios.");
        if (!"Pendiente".equalsIgnoreCase(sugerencia.getEstado())) throw new IllegalStateException("La sugerencia ya fue resuelta.");
        if (aprobar) {
            for (Producto existente : sistema.getProductos()) if (existente.getNombre().equalsIgnoreCase(sugerencia.getProducto().getNombre())) throw new IllegalArgumentException("Ya existe un producto con ese nombre.");
        }
        resolverSugerencia(sugerencia, aprobar);
        if (aprobar) sistema.registrarProducto(sugerencia.getProducto());
    }

    /**
     * Resuelve una solicitud de cambio de turno (HU-A12). Al aprobar se verifica la cobertura mínima; si no se
     * cumple, la solicitud queda rechazada con el motivo y los turnos no cambian. Un rechazo manual exige motivo.
     */
    public void resolverCambioTurno(SolicitudCambioTurno solicitud, List<Empleado> empleados, boolean aprobar, String motivoRechazo) {
        if (solicitud == null) throw new IllegalArgumentException("La solicitud es obligatoria.");
        if (!"Pendiente".equals(solicitud.getEstado())) throw new IllegalStateException("La solicitud ya fue resuelta.");
        if (!aprobar) { solicitud.rechazar(motivoRechazo); return; }
        if (solicitud.getSolicitante() == null || !solicitud.cumpleCobertura(empleados)) {
            solicitud.rechazar("El cambio dejaría algún momento sin la cobertura mínima de 2 entrenadores y 1 fisioterapeuta.");
            return;
        }
        solicitud.aprobar();
        solicitud.aplicarCambio();
        if (!solicitudesAprobadas.contains(solicitud)) solicitudesAprobadas.add(solicitud);
    }

    /** Publica la programación semanal de turnos si cumple la cobertura mínima (HU-A11). */
    public void publicarProgramacion(ProgramacionSemanal programacion) {
        if (programacion == null) throw new IllegalArgumentException("La programación es obligatoria.");
        programacion.publicar();
    }
    
    public double reporteVentas(SistemaClub sistema, String rubro, LocalDate desde, LocalDate hasta) {
        if (sistema == null || rubro == null || desde == null || hasta == null)
            throw new IllegalArgumentException("Sistema, rubro y periodo son obligatorios.");
        double total = 0.0;
        for (Usuario usuario : sistema.getUsuarios().values()) {
            for (Transaccion t : usuario.consultarHistorialCompras()) {
                LocalDate dia = t.getFechaHora().toLocalDate();
                if (t.isConfirmada() && t.getRubro().equalsIgnoreCase(rubro) && !dia.isBefore(desde) && !dia.isAfter(hasta))
                    total += t.getTotal();
            }
        }
        return total;
    }

    public List<Reserva> historialReservas(SistemaClub sistema) {
        List<Reserva> todas = new ArrayList<>();
        for (Instalacion i : sistema.getInstalaciones()) todas.addAll(i.getReservas());
        return todas;
    }

    public List<SesionEntrenamiento> historialSesiones(SistemaClub sistema) {
        List<SesionEntrenamiento> todas = new ArrayList<>();
        for (Instalacion i : sistema.getInstalaciones()) todas.addAll(i.getSesiones());
        return todas;
    }

    public List<PartidoInterno> historialPartidosInternos(SistemaClub sistema) {
        List<PartidoInterno> todos = new ArrayList<>();
        for (Equipo e : sistema.getEquipos())
            for (PartidoInterno p : e.getPartidosInternos()) if (!todos.contains(p)) todos.add(p);
        return todos;
    }

    public List<PartidoCompetencia> historialPartidosCompetencia(SistemaClub sistema) {
        List<PartidoCompetencia> todos = new ArrayList<>();
        for (Competencia c : sistema.getCompetencias()) todos.addAll(c.getPartidos());
        return todos;
    }

    // ===================================================================== HU-A13 con rango y filtros
    private static boolean enRango(LocalDate f, LocalDate desde, LocalDate hasta) { return f != null && !f.isBefore(desde) && !f.isAfter(hasta); }

    public List<Reserva> historialReservas(SistemaClub sistema, LocalDate desde, LocalDate hasta, Socio socio,
            club.modelo.entrenamiento.DisciplinaIndividual disciplina) {
        validarRango(desde, hasta);
        List<Reserva> r = new ArrayList<>();
        for (Reserva x : historialReservas(sistema)) {
            if (x.getFechaHoraInicio() == null || !enRango(x.getFechaHoraInicio().toLocalDate(), desde, hasta)) continue;
            if (socio != null && !x.incluye(socio) && x.getSocioResponsable() != socio) continue;
            if (disciplina != null && x.getInstalacion().getDisciplina() != disciplina) continue;
            r.add(x);
        }
        return r;
    }

    public List<SesionEntrenamiento> historialSesiones(SistemaClub sistema, LocalDate desde, LocalDate hasta, Entrenador entrenador,
            club.modelo.entrenamiento.DisciplinaIndividual disciplina) {
        validarRango(desde, hasta);
        List<SesionEntrenamiento> r = new ArrayList<>();
        for (SesionEntrenamiento s : historialSesiones(sistema)) {
            if (s.getFechaHora() == null || !enRango(s.getFechaHora().toLocalDate(), desde, hasta)) continue;
            if (entrenador != null && s.getEntrenador() != entrenador) continue;
            if (disciplina != null && s.getInstalacion().getDisciplina() != disciplina) continue;
            r.add(s);
        }
        return r;
    }

    public List<PartidoInterno> historialPartidosInternos(SistemaClub sistema, LocalDate desde, LocalDate hasta, Entrenador entrenador) {
        validarRango(desde, hasta);
        List<PartidoInterno> r = new ArrayList<>();
        for (PartidoInterno p : historialPartidosInternos(sistema)) {
            if (p.getFechaHora() == null || !enRango(p.getFechaHora().toLocalDate(), desde, hasta)) continue;
            if (entrenador != null && p.getEntrenador() != entrenador) continue;
            r.add(p);
        }
        return r;
    }

    public List<PartidoCompetencia> historialPartidosCompetencia(SistemaClub sistema, LocalDate desde, LocalDate hasta, Entrenador entrenador) {
        validarRango(desde, hasta);
        List<PartidoCompetencia> r = new ArrayList<>();
        for (PartidoCompetencia p : historialPartidosCompetencia(sistema)) {
            if (!enRango(p.getFecha(), desde, hasta)) continue;
            if (entrenador != null && p.getEntrenadorResponsable() != entrenador) continue;
            r.add(p);
        }
        return r;
    }

    public List<PartidoPractica> historialPracticas(SistemaClub sistema, LocalDate desde, LocalDate hasta, Socio socio,
            club.modelo.entrenamiento.DisciplinaIndividual disciplina) {
        List<PartidoPractica> r = new ArrayList<>();
        for (PartidoPractica p : historialPracticas(sistema, desde, hasta, socio))
            if (disciplina == null || p.getDisciplina() == disciplina) r.add(p);
        return r;
    }

    // ===================================================================== HU-A15
    /** Reporte deportivo de un periodo: asistencia, resultados y evolución del ELO. Informa si no hay datos. */
    public String reporteDeportivo(SistemaClub sistema, LocalDate desde, LocalDate hasta) {
        if (sistema == null) throw new IllegalArgumentException("El sistema es obligatorio.");
        validarRango(desde, hasta);
        StringBuilder sb = new StringBuilder();
        boolean hay = false;
        StringBuilder asistencia = new StringBuilder();
        for (Socio s : sistema.getSocios()) {
            int total = 0;
            for (club.modelo.entrenamiento.Asistencia a : s.getAsistencias())
                if (a.getSesion() != null && a.getSesion().getFechaHora() != null && enRango(a.getSesion().getFechaHora().toLocalDate(), desde, hasta)) total++;
            if (total > 0) asistencia.append(s.getNombre()).append(": ").append(total).append(" sesiones con asistencia registrada\n");
        }
        if (asistencia.length() > 0) { hay = true; sb.append("--- Asistencia a entrenamientos ---\n").append(asistencia); }
        StringBuilder partidos = new StringBuilder();
        for (PartidoInterno p : historialPartidosInternos(sistema, desde, hasta, null))
            if (p.isFinalizado()) partidos.append(String.format("[Interno] %s | %s vs %s | %s%n", p.getFechaHora().toLocalDate(), p.getEquipoLocal().getNombre(), p.getEquipoVisitante().getNombre(), p.getMarcadorFinal()));
        for (PartidoCompetencia p : historialPartidosCompetencia(sistema, desde, hasta, null))
            if (p.isFinalizado()) partidos.append(String.format("[Competencia] %s | %s | %s%n", p.getFecha(), p.getEquipos().size() == 2 ? p.getEquipos().get(0).getNombre() + " vs " + p.getEquipos().get(1).getNombre() : "-", p.getMarcador()));
        for (PartidoPractica p : historialPracticas(sistema, desde, hasta, null))
            if (p.isResultadoRegistrado()) partidos.append(String.format("[Práctica] %s | sets %d-%d%n", p.getFechaHora().toLocalDate(), p.getSetsLadoA(), p.getSetsLadoB()));
        if (partidos.length() > 0) { hay = true; sb.append("--- Resultados de partidos ---\n").append(partidos); }
        StringBuilder elo = new StringBuilder();
        for (Socio s : sistema.getSocios())
            for (club.modelo.entrenamiento.InscripcionDisciplina i : s.getInscripcionesDisciplina()) {
                List<String> valores = new ArrayList<>();
                for (String e : i.getHistorialRanking()) {
                    int k = e.indexOf(':');
                    if (k > 0 && enRango(LocalDate.parse(e.substring(0, k)), desde, hasta)) valores.add(e.substring(k + 1));
                }
                if (!valores.isEmpty()) elo.append(s.getNombre()).append(" | ").append(i.getDisciplina().getClass().getSimpleName())
                        .append(" | ELO: ").append(String.join(" -> ", valores)).append('\n');
            }
        if (elo.length() > 0) { hay = true; sb.append("--- Evolución del ELO ---\n").append(elo); }
        return hay ? sb.toString() : "No hay datos para ese periodo.";
    }

    public String estadoInventario(SistemaClub sistema, String ubicacionFiltro) {
        if (ubicacionFiltro != null && !ubicacionFiltro.trim().isEmpty()) validarUbicacion(ubicacionFiltro);
        StringBuilder sb = new StringBuilder("--- Instalaciones ---\n");
        for (Instalacion i : sistema.getInstalaciones())
            sb.append(i.getNombre())
              .append(" | disciplina: ").append(i.getDisciplina() == null ? "-" : i.getDisciplina().getClass().getSimpleName())
              .append(" | capacidad: ").append(i.getJugadoresMin()).append("-").append(i.getJugadoresMax()).append(" jugadores")
              .append(" | techada: ").append(i.isTechada() ? "sí" : "no")
              .append(" | estado: ").append(estadoInstalacion(i))
              .append('\n');
        sb.append("--- Artículos ---\n");
        boolean hay = false;
        for (Producto p : sistema.getProductos()) {
            int total = 0; StringBuilder linea = new StringBuilder();
            for (Existencia e : p.getExistencias()) {
                total += e.getCantidad();
                if (ubicacionFiltro == null || ubicacionFiltro.trim().isEmpty() || e.getUbicacionInventario().equalsIgnoreCase(ubicacionFiltro.trim()))
                    linea.append(" | ").append(e.getUbicacionInventario()).append(": ").append(e.getCantidad());
            }
            if (linea.length() == 0 && ubicacionFiltro != null && !ubicacionFiltro.trim().isEmpty()) continue;
            hay = true;
            sb.append(p.getNombre()).append(linea).append(" | total: ").append(total).append('\n');
        }
        if (!hay) sb.append("No se encontró inventario en esa ubicación.\n");
        return sb.toString();
    }


    private String estadoInstalacion(Instalacion i) {
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        for (club.modelo.instalaciones.Reserva r : i.getReservas())
            if (r.getFechaHoraInicio() != null && !r.getFechaHoraInicio().isAfter(ahora) && r.estaActiva(ahora)) return "ocupada";
        for (club.modelo.entrenamiento.SesionEntrenamiento s : i.getSesiones())
            if (s.seTraslapaCon(ahora, ahora.plusMinutes(1))) return "ocupada";
        return "disponible";
    }
    
    public Map<String, Double> generarReporteVentas(List<Transaccion> transacciones, String rubro,
                                                     String periodo, LocalDate fechaRef) {
        Map<String, Double> reporte = new HashMap<>();
        if (transacciones == null || periodo == null || fechaRef == null) return reporte;
        String alcance = periodo.trim().toLowerCase();
        for (Transaccion transaccion : transacciones) {
            if (transaccion == null || !transaccion.isConfirmada()) continue;
            if (rubro != null && !rubro.trim().isEmpty() && !rubro.equalsIgnoreCase(transaccion.getRubro())) continue;
            LocalDate fecha = transaccion.getFechaHora().toLocalDate();
            boolean coincide;
            if (alcance.equals("dia") || alcance.equals("diario")) coincide = fecha.equals(fechaRef);
            else if (alcance.equals("semana") || alcance.equals("semanal")) {
                WeekFields semanas = WeekFields.ISO;
                coincide = fecha.getYear() == fechaRef.getYear() && fecha.get(semanas.weekOfWeekBasedYear()) == fechaRef.get(semanas.weekOfWeekBasedYear());
            } else if (alcance.equals("mes") || alcance.equals("mensual")) coincide = fecha.getYear() == fechaRef.getYear() && fecha.getMonth() == fechaRef.getMonth();
            else if (alcance.equals("anio") || alcance.equals("año") || alcance.equals("anual")) coincide = fecha.getYear() == fechaRef.getYear();
            else throw new IllegalArgumentException("El periodo debe ser día, semana, mes o año.");
            if (coincide) reporte.merge(transaccion.getRubro(), transaccion.getTotal(), Double::sum);
        }
        return reporte;
    }
    
    public List<Empleado> listarEmpleados(SistemaClub sistema) {
        List<Empleado> empleados = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values())
            if (u instanceof Empleado) empleados.add((Empleado) u);
        return empleados;
    }

    public List<SolicitudCambioTurno> consultarSolicitudesPendientes(List<Empleado> empleados) {
        List<SolicitudCambioTurno> pendientes = new ArrayList<>();
        for (Empleado e : empleados)
            for (SolicitudCambioTurno s : e.getSolicitudes())
                if ("Pendiente".equals(s.getEstado())) pendientes.add(s);
        return pendientes;
    }

    public String generarReporteDeportivo(List<Socio> socios) {
        StringBuilder reporte = new StringBuilder("--- Reporte Deportivo Global ---\n");
        if (socios == null) return reporte.toString();
        for (Socio socio : socios) {
            if (socio == null) continue;
            reporte.append(socio.getNombre()).append(" (").append(socio.getLogin()).append(")")
                    .append(" | asistencia: ").append(String.format(java.util.Locale.ROOT, "%.1f", socio.getPorcentajeAsistencia())).append("%")
                    .append(" | equipos activos: ").append(socio.getEquipos().size())
                    .append(" | partidos de práctica: ").append(socio.getPartidosPractica().size())
                    .append(" | sanciones vigentes: ").append(socio.getSancionesVigentes().size()).append('\n');
        }
        return reporte.toString();
    }
    
    

    public List<SugerenciaProducto> getSugerenciasAprobadas() { return sugerenciasAprobadas; }
    public List<SolicitudCambioTurno> getSolicitudesAprobadas() { return solicitudesAprobadas; }
    public List<Producto> getProductosCatalogo() { return productosCatalogo; }
    
    public void rechazarSugerencia(SugerenciaProducto s, String motivo) {
        if (s == null) throw new IllegalArgumentException("La sugerencia es obligatoria.");
        if (!"Pendiente".equalsIgnoreCase(s.getEstado())) throw new IllegalStateException("La sugerencia ya fue resuelta.");
        s.rechazar(motivo);
    }
    
    private static final java.util.List<String> UBICACIONES = java.util.Arrays.asList("tienda", "cafeteria", "bodega");
    private void validarUbicacion(String u) {
        if (u == null || UBICACIONES.stream().noneMatch(x -> x.equalsIgnoreCase(u.trim())))
            throw new IllegalArgumentException("Ubicación inexistente: " + u + ". Válidas: " + UBICACIONES);
    }
    
    private void validarRango(LocalDate d, LocalDate h) {
        if (d == null || h == null || h.isBefore(d)) throw new IllegalArgumentException("El rango de fechas es inválido.");
    }

    public List<PartidoPractica> historialPracticas(SistemaClub sistema, LocalDate desde, LocalDate hasta, Socio filtroSocio) {
        validarRango(desde, hasta);
        List<PartidoPractica> r = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values())
            if (u instanceof Socio)
                for (PartidoPractica p : ((Socio) u).getPartidosPractica()) {
                    if (r.contains(p) || p.getFechaHora() == null) continue;
                    LocalDate f = p.getFechaHora().toLocalDate();
                    if (f.isBefore(desde) || f.isAfter(hasta)) continue;
                    if (filtroSocio != null && !p.incluye(filtroSocio)) continue;
                    r.add(p);
                }
        return r;
    }
    
    public String reporteVentasDetallado(List<Transaccion> ts, String rubro, String periodo, LocalDate ref) {
        return reporteVentasDetallado(ts, null, rubro, periodo, ref);
    }

    /** HU-A14: reporte por rubro (tienda, cafeteria o mensualidad) y periodo, con desglose. */
    public String reporteVentasDetallado(SistemaClub sistema, String rubro, String periodo, LocalDate ref) {
        if (sistema == null) throw new IllegalArgumentException("El sistema es obligatorio.");
        List<Transaccion> todas = new ArrayList<>();
        for (Usuario u : sistema.getUsuarios().values()) todas.addAll(u.consultarHistorialCompras());
        return reporteVentasDetallado(todas, sistema.getPagosMensualidad(), rubro, periodo, ref);
    }

    private String reporteVentasDetallado(List<Transaccion> ts, List<PagoMensualidad> pagos, String rubro, String periodo, LocalDate ref) {
        if (rubro == null || !java.util.Arrays.asList("tienda", "cafeteria", "mensualidad").contains(rubro.trim().toLowerCase()))
            throw new IllegalArgumentException("Rubro inválido (tienda, cafeteria, mensualidad).");
        if (periodo == null || !java.util.Arrays.asList("diario", "semanal", "mensual").contains(periodo.trim().toLowerCase()))
            throw new IllegalArgumentException("Periodo inválido (diario, semanal, mensual).");
        if (ref == null) throw new IllegalArgumentException("La fecha de referencia es obligatoria.");
        rubro = rubro.trim().toLowerCase();
        LocalDate ini = ref, fin = ref;
        switch (periodo.trim().toLowerCase()) {
            case "semanal": ini = ref.with(java.time.DayOfWeek.MONDAY); fin = ini.plusDays(6); break;
            case "mensual": ini = ref.withDayOfMonth(1); fin = ref.withDayOfMonth(ref.lengthOfMonth()); break;
            default: break;
        }
        int n = 0; double neto = 0, iva = 0, consumo = 0, propinas = 0, total = 0;
        if ("mensualidad".equals(rubro)) {
            if (pagos != null) for (PagoMensualidad p : pagos) {
                LocalDate f = p.getFechaRegistro().toLocalDate();
                if (f.isBefore(ini) || f.isAfter(fin)) continue;
                n++; neto += p.getValor(); total += p.getValor();
            }
        } else if (ts != null) {
            for (Transaccion t : ts) {
                if (t == null || !t.isConfirmada() || !rubro.equalsIgnoreCase(t.getRubro())) continue;
                LocalDate f = t.getFechaHora().toLocalDate();
                if (f.isBefore(ini) || f.isAfter(fin)) continue;
                n++; neto += t.getSubtotal() - t.getDescuento();
                if ("cafeteria".equals(rubro)) consumo += t.getImpuestos(); else iva += t.getImpuestos();
                propinas += t.getPropina(); total += t.getTotal();
            }
        }
        if (n == 0) return "No hay datos para ese periodo.";
        return String.format("Ventas: %d | Neto: %.0f | IVA: %.0f | Impuesto al consumo: %.0f | Propinas: %.0f | Total: %.0f", n, neto, iva, consumo, propinas, total);
    }
}
