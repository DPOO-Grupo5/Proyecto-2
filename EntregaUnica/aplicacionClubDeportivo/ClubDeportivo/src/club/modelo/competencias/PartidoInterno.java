package club.modelo.competencias;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;

public class PartidoInterno implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDateTime fechaHora;
    private String marcadorFinal;
    private int marcadorLocal;
    private int marcadorVisitante;
    private boolean finalizado;
    private boolean validado;
    private Equipo equipoLocal;
    private Equipo equipoVisitante;
    private List<EstadisticaJugadorPartido> estadisticas = new ArrayList<>();
    /** Entrenador que programó el partido; solo él registra el resultado (HU-T04, HU-T06). */
    private Entrenador entrenador;
    private List<Socio> alineacionLocal = new ArrayList<>();
    private List<Socio> alineacionVisitante = new ArrayList<>();

    public PartidoInterno(Equipo equipoLocal, Equipo equipoVisitante, LocalDateTime fechaHora) {
        this.equipoLocal = equipoLocal;
        this.equipoVisitante = equipoVisitante;
        this.fechaHora = fechaHora;
    }

    public void validarPuedeJugarse() {
        if (equipoLocal == null || equipoVisitante == null || equipoLocal == equipoVisitante) throw new IllegalArgumentException("Se requieren dos equipos distintos.");
        if (fechaHora == null) throw new IllegalArgumentException("La fecha del partido es obligatoria.");
        validarEquipo(equipoLocal, "local");
        validarEquipo(equipoVisitante, "visitante");
        if (equipoLocal.getDeporte() != equipoVisitante.getDeporte() || equipoLocal.getCategoria() != equipoVisitante.getCategoria()) {
            throw new IllegalStateException("Los equipos deben pertenecer al mismo deporte y categoría.");
        }
        validado = true;
    }

    private void validarEquipo(Equipo equipo, String etiqueta) {
        if (!equipo.isActivo() || equipo.getDeporte() == null || equipo.getCategoria() == null) throw new IllegalStateException("El equipo " + etiqueta + " debe estar activo y configurado.");
        int disponibles = 0;
        for (InscripcionEquipo inscripcion : equipo.getInscripcionesActivas()) {
            if (!inscripcion.getSocio().tieneSancionVigente()) disponibles++;
        }
        int minimo = equipo.getDeporte().getMinJugadores();
        if (disponibles < minimo)
            throw new IllegalStateException("El equipo " + equipo.getNombre() + " tiene " + disponibles + " jugadores habilitados; faltan "
                    + (minimo - disponibles) + " para el mínimo de " + minimo + ".");
    }

    /** Programa el partido (HU-T04): equipos distintos, mismo deporte y categoría, mínimo de habilitados y fecha futura. */
    public void programar(Entrenador responsable, LocalDateTime ahora) {
        if (responsable == null) throw new IllegalArgumentException("El entrenador es obligatorio.");
        if (equipoLocal == null || equipoVisitante == null || equipoLocal == equipoVisitante) throw new IllegalArgumentException("Se requieren dos equipos distintos.");
        if (fechaHora == null || !fechaHora.isAfter(ahora)) throw new IllegalArgumentException("La fecha del partido debe ser futura.");
        validarPuedeJugarse();
        this.entrenador = responsable;
        if (!equipoLocal.getPartidosInternos().contains(this)) equipoLocal.getPartidosInternos().add(this);
        if (!equipoVisitante.getPartidosInternos().contains(this)) equipoVisitante.getPartidosInternos().add(this);
    }

    /**
     * Define la alineación de uno de los dos equipos (HU-T05): solo jugadores activos del equipo y sin suspensión vigente.
     * Una alineación rechazada no modifica la anterior.
     */
    public void definirAlineacion(Equipo equipo, List<Socio> jugadores) {
        if (finalizado) throw new IllegalStateException("El partido ya finalizó.");
        if (equipo != equipoLocal && equipo != equipoVisitante) throw new IllegalArgumentException("El equipo no juega este partido.");
        if (jugadores == null || jugadores.isEmpty()) throw new IllegalArgumentException("La alineación no puede estar vacía.");
        Set<Socio> distintos = new HashSet<>();
        for (Socio j : jugadores) {
            if (j == null || !perteneceActivo(equipo, j)) throw new IllegalArgumentException((j == null ? "Un jugador" : j.getNombre()) + " no pertenece al equipo " + equipo.getNombre() + ".");
            if (!distintos.add(j)) throw new IllegalArgumentException(j.getNombre() + " está repetido en la alineación.");
            for (Sancion s : j.getSancionesVigentes())
                throw new IllegalArgumentException(j.getNombre() + " está suspendido (" + s.getMotivo() + ", " + s.getPartidosSuspension() + " partido(s) pendiente(s)).");
        }
        List<Socio> destino = equipo == equipoLocal ? alineacionLocal : alineacionVisitante;
        destino.clear();
        destino.addAll(jugadores);
    }

    public List<Socio> getAlineacion(Equipo equipo) {
        if (equipo == equipoLocal) return alineacionLocal;
        if (equipo == equipoVisitante) return alineacionVisitante;
        throw new IllegalArgumentException("El equipo no juega este partido.");
    }

    private boolean estaAlineado(Socio jugador) {
        return alineacionLocal.contains(jugador) || alineacionVisitante.contains(jugador);
    }

    /**
     * Registra el marcador y las estadísticas (HU-T06, HU-T07, HU-T08). El partido debe haberse disputado, tener las dos
     * alineaciones y no tener resultado. Todo se valida antes de modificar el estado: si algo falla, nada cambia.
     */
    public void registrarResultado(int golesA, int golesB, List<EstadisticaJugadorPartido> stats, LocalDateTime ahora) {
        if (finalizado) throw new IllegalStateException("El resultado ya fue registrado.");
        if (fechaHora.isAfter(ahora)) throw new IllegalStateException("El partido aún no se ha disputado (" + fechaHora + ").");
        if (alineacionLocal.isEmpty() || alineacionVisitante.isEmpty()) throw new IllegalStateException("Debe definir la alineación de ambos equipos.");
        if (golesA < 0 || golesB < 0) throw new IllegalArgumentException("El marcador no puede ser negativo.");
        equipoLocal.getDeporte().validarMarcador(golesA, golesB);
        validarEstadisticas(stats == null ? new ArrayList<>() : stats);
        marcadorLocal = golesA;
        marcadorVisitante = golesB;
        marcadorFinal = golesA + " - " + golesB;
        finalizado = true;
        consumirSuspensiones(equipoLocal);
        consumirSuspensiones(equipoVisitante);
        if (stats != null) for (EstadisticaJugadorPartido e : stats) registrarEstadistica(e);
    }

    private void registrarEstadistica(EstadisticaJugadorPartido estadistica) {
        Socio jugador = estadistica.getJugador();
        estadistica.setPartidoInterno(this);
        estadisticas.add(estadistica);
        jugador.registrarEstadistica(estadistica);
        if (estadistica instanceof EstadisticaJugadorFutbol) registrarSancionDisciplinaria((EstadisticaJugadorFutbol) estadistica, jugador);
    }

    public void validarEstadisticas(List<EstadisticaJugadorPartido> estadisticas) {
        if (estadisticas == null) throw new IllegalArgumentException("La lista de estadísticas es obligatoria.");
        Set<Socio> jugadores = new HashSet<>();
        for (EstadisticaJugadorPartido estadistica : estadisticas) {
            validarEstadistica(estadistica);
            if (!jugadores.add(estadistica.getJugador())) throw new IllegalArgumentException("Un jugador no puede tener dos estadísticas para el mismo partido.");
        }
    }

    private void validarEstadistica(EstadisticaJugadorPartido estadistica) {
        if (estadistica == null || estadistica.getJugador() == null) throw new IllegalArgumentException("La estadística debe estar asociada a un jugador.");
        Socio jugador = estadistica.getJugador();
        if (!estaAlineado(jugador)) throw new IllegalArgumentException(jugador.getNombre() + " no fue alineado en este partido.");
        for (EstadisticaJugadorPartido existente : estadisticas) if (existente.getJugador() == jugador) throw new IllegalStateException("Ya hay estadísticas para este jugador.");
        validarTipoEstadistica(estadistica);
    }

    private boolean perteneceActivo(Equipo equipo, Socio jugador) {
        for (InscripcionEquipo inscripcion : equipo.getInscripcionesActivas()) if (inscripcion.getSocio() == jugador) return true;
        return false;
    }

    private void validarTipoEstadistica(EstadisticaJugadorPartido estadistica) {
        DeporteConjunto deporte = equipoLocal.getDeporte();
        boolean coincide = (deporte instanceof Futbol && estadistica instanceof EstadisticaJugadorFutbol)
                || (deporte instanceof Baloncesto && estadistica instanceof EstadisticaJugadorBaloncesto)
                || (deporte instanceof Voleibol && estadistica instanceof EstadisticaJugadorVoleibol);
        if (!coincide) throw new IllegalArgumentException("El tipo de estadística no corresponde al deporte.");
    }

    private void registrarSancionDisciplinaria(EstadisticaJugadorFutbol estadistica, Socio jugador) {
        int sanciones = estadistica.getTarjetasRojas() + (estadistica.getTarjetasAmarillas() >= 2 ? 1 : 0);
        if (sanciones > 0) {
            Sancion sancion = new Sancion(estadistica.getTarjetasRojas() > 0 ? "Tarjeta roja" : "Acumulación de tarjetas amarillas", sanciones, false, jugador);
            estadistica.setSancion(sancion);
            jugador.registrarSancion(sancion);
        }
    }

    private void consumirSuspensiones(Equipo equipo) {
        for (InscripcionEquipo inscripcion : equipo.getInscripcionesActivas()) {
            for (Sancion sancion : inscripcion.getSocio().getSancionesVigentes()) sancion.consumirPartido();
        }
    }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public Entrenador getEntrenador() { return entrenador; }

    public void setFechaHora(LocalDateTime fechaHora) { if (fechaHora == null) throw new IllegalArgumentException("La fecha es obligatoria."); this.fechaHora = fechaHora; }
    public String getMarcadorFinal() { return marcadorFinal; }
    public int getMarcadorLocal() { return marcadorLocal; }
    public int getMarcadorVisitante() { return marcadorVisitante; }
    public boolean isFinalizado() { return finalizado; }
    public Equipo getEquipoLocal() { return equipoLocal; }
    public void setEquipoLocal(Equipo equipoLocal) { this.equipoLocal = equipoLocal; }
    public Equipo getEquipoVisitante() { return equipoVisitante; }
    public void setEquipoVisitante(Equipo equipoVisitante) { this.equipoVisitante = equipoVisitante; }
    public List<EstadisticaJugadorPartido> getEstadisticas() { return estadisticas; }
    public void setEstadisticas(List<EstadisticaJugadorPartido> estadisticas) { this.estadisticas = estadisticas == null ? new ArrayList<>() : estadisticas; }
}
