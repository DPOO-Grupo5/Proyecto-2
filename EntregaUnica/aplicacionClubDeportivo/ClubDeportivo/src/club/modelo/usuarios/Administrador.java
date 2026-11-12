package club.modelo.usuarios;

import java.time.LocalDate;
import java.time.YearMonth;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.competencias.PartidoInterno;
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
        if (producto == null || origen == null || destino == null || origen.equalsIgnoreCase(destino) || cantidad <= 0) {
            throw new IllegalArgumentException("Producto, ubicaciones distintas y cantidad positiva son obligatorios.");
        }
        Existencia fuente = producto.buscarExistencia(origen);
        if (fuente == null || !fuente.hayDisponible(cantidad)) throw new IllegalStateException("No hay suficiente inventario en la ubicación de origen.");
        Existencia destinoExistente = producto.buscarExistencia(destino);
        fuente.disminuir(cantidad);
        if (destinoExistente == null) producto.agregarExistencia(new Existencia(cantidad, destino));
        else destinoExistente.aumentar(cantidad);
    }

    public void reabastecer(Producto producto, String ubicacion, int cantidad) {
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

    public String estadoInventario(SistemaClub sistema) {
        StringBuilder sb = new StringBuilder("--- Instalaciones ---\n");
        for (Instalacion i : sistema.getInstalaciones())
            sb.append(i.getNombre()).append(" | reservas: ").append(i.getReservas().size()).append('\n');
        sb.append("--- Artículos ---\n");
        for (Producto p : sistema.getProductos()) {
            sb.append(p.getNombre());
            for (Existencia e : p.getExistencias())
                sb.append(" | ").append(e.getUbicacionInventario()).append(": ").append(e.getCantidad());
            sb.append('\n');
        }
        return sb.toString();
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
}
