package club.modelo.usuarios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import club.modelo.instalaciones.CanchaConjunto;

import club.modelo.competencias.InscripcionEquipo;

import club.model.Reloj;

import club.modelo.competencias.Equipo;
import club.modelo.competencias.EstadisticaJugadorPartido;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.competencias.PartidoInterno;
import club.modelo.entrenamiento.Asistencia;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.EstadisticaIndividual;
import club.modelo.entrenamiento.PartidoPractica;
import club.modelo.entrenamiento.SesionEntrenamiento;
import club.modelo.instalaciones.Instalacion;
import club.modelo.instalaciones.Reserva;

public class Entrenador extends Empleado implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private List<SesionEntrenamiento> sesiones = new ArrayList<>();
    private List<DisciplinaIndividual> disciplinas = new ArrayList<>();
    private List<Reserva> reservas = new ArrayList<>();

    public Entrenador(String login, String password, String nombre, String codigoDescuento) {
        super(login, password, nombre, codigoDescuento);
    }


    /** HU-T01 (deporte de conjunto): convoca a todos los jugadores activos del equipo en una cancha de conjunto. */
    public SesionEntrenamiento planearSesionEquipo(LocalDateTime fechaHora, int duracionMin, Instalacion inst, Equipo equipo) {
        if (equipo == null || !equipo.isActivo()) throw new IllegalArgumentException("El equipo debe existir y estar activo.");
        if (!(inst instanceof CanchaConjunto)) throw new IllegalArgumentException("La instalación no es compatible con un deporte de conjunto.");
        List<Socio> convocados = new ArrayList<>();
        for (InscripcionEquipo i : equipo.getInscripcionesActivas()) convocados.add(i.getSocio());
        return planear(fechaHora, duracionMin, "equipo " + equipo.getNombre(), inst, convocados);
    }

    /** HU-T01 (academia): convoca a uno o varios inscritos en la disciplina, en una instalación de esa disciplina. */
    public SesionEntrenamiento planearSesionAcademia(LocalDateTime fechaHora, int duracionMin, Instalacion inst,
                                                     DisciplinaIndividual disciplina, List<Socio> convocados) {
        if (disciplina == null) throw new IllegalArgumentException("La disciplina es obligatoria.");
        if (inst == null || inst.getDisciplina() != disciplina)
            throw new IllegalArgumentException("La instalación no es de " + disciplina.getClass().getSimpleName() + ".");
        if (convocados != null) for (Socio s : convocados)
            if (s == null || !disciplina.tieneInscripcionActiva(s))
                throw new IllegalArgumentException((s == null ? "Un convocado" : s.getNombre()) + " no está inscrito en " + disciplina.getClass().getSimpleName() + ".");
        return planear(fechaHora, duracionMin, "academia " + disciplina.getClass().getSimpleName(), inst, convocados);
    }

    private SesionEntrenamiento planear(LocalDateTime fechaHora, int duracionMin, String tipo, Instalacion inst, List<Socio> convocados) {
        if (fechaHora == null || !fechaHora.isAfter(Reloj.ahora())) throw new IllegalArgumentException("La fecha de la sesión debe ser futura.");
        if (duracionMin <= 0) throw new IllegalArgumentException("La duración debe ser mayor que cero.");
        if (convocados == null || convocados.isEmpty()) throw new IllegalArgumentException("La sesión debe tener convocados.");
        LocalDateTime fin = fechaHora.plusMinutes(duracionMin);
        for (SesionEntrenamiento otra : sesiones)
            if (otra.seTraslapaCon(fechaHora, fin)) throw new IllegalStateException("Ya tiene otra sesión en ese horario (" + otra.getFechaHora() + ").");
        SesionEntrenamiento sesion = new SesionEntrenamiento(fechaHora, duracionMin, tipo, inst, this);
        sesion.validarDisponibilidad();
        sesion.validarConvocados(convocados);
        for (Socio s : convocados) sesion.agregarConvocado(s);
        this.sesiones.add(sesion);
        inst.registrarSesion(sesion);
        return sesion;
    }

    /** HU-T02 y HU-T03: asistencia de un convocado en una sesión propia ya finalizada; la nota es opcional. */
    public Asistencia registrarAsistencia(SesionEntrenamiento s, Socio j, String estado, Integer nota, String comentario) {
        if (s == null || s.getEntrenador() != this) throw new IllegalArgumentException("La sesión debe pertenecer a este entrenador.");
        if (!s.haFinalizado(Reloj.ahora())) throw new IllegalStateException("La sesión aún no ha finalizado.");
        Asistencia asistencia = new Asistencia(estado, null, null, j, s);
        if (nota != null) asistencia.evaluar(nota, comentario);
        s.registrarAsistencia(asistencia);
        j.getAsistencias().add(asistencia);
        return asistencia;
    }

    /** HU-T03: evalúa con nota de 1 a 5 a un jugador que asistió o llegó tarde a una sesión propia. */
    public Asistencia evaluar(SesionEntrenamiento s, Socio j, int nota, String comentario) {
        if (s == null || s.getEntrenador() != this) throw new IllegalArgumentException("La sesión debe pertenecer a este entrenador.");
        for (Asistencia a : s.getAsistencias()) if (a.getSocio() == j) { a.evaluar(nota, comentario); return a; }
        throw new IllegalStateException("El jugador no tiene asistencia registrada en esta sesión.");
    }

    public void asignarNivel(Socio s, DisciplinaIndividual d, String nivel) {
        if (s == null || d == null) throw new IllegalArgumentException("Socio y disciplina son obligatorios.");
        for (club.modelo.entrenamiento.InscripcionDisciplina insc : s.getInscripcionesDisciplina()) {
            if (insc.getDisciplina() == d && insc.isActiva()) { insc.cambiarNivel(nivel); return; }
        }
        throw new IllegalArgumentException("El socio no tiene una inscripción activa en la disciplina.");
    }

    /** HU-T04: programa un partido interno entre dos equipos del club para una fecha futura. */
    public PartidoInterno programarPartidoInterno(Equipo local, Equipo visitante, LocalDateTime fecha) {
        PartidoInterno partido = new PartidoInterno(local, visitante, fecha);
        partido.programar(this, Reloj.ahora());
        return partido;
    }

    /** HU-T06, T07 y T08: registra marcador, estadísticas y tarjetas de un partido que este entrenador programó. */
    public void registrarResultadoInterno(PartidoInterno partido, int local, int visitante, List<EstadisticaJugadorPartido> estadisticas) {
        if (partido == null) throw new IllegalArgumentException("El partido no existe.");
        if (partido.getEntrenador() != this) throw new IllegalStateException("Solo el entrenador que programó el partido puede registrar su resultado.");
        partido.registrarResultado(local, visitante, estadisticas, Reloj.ahora());
    }

    /**
     * Registra el resultado de un partido oficial que este entrenador tiene a cargo (HU-T09).
     * El partido debe estar programado por el administrador y este entrenador debe ser el responsable.
     */
    public void registrarResultadoOficial(PartidoCompetencia partido, int local, int visitante) {
        if (partido == null) throw new IllegalArgumentException("El partido es obligatorio.");
        if (!partido.isProgramado()) throw new IllegalStateException("El partido no ha sido programado por el administrador.");
        if (partido.getEntrenadorResponsable() != this) throw new IllegalStateException("Solo el entrenador responsable del partido puede registrar su resultado.");
        partido.registrarMarcador(local, visitante);
    }

    /** Registra el resultado y las estadísticas de un partido de práctica (HU-T10). */
    public void registrarResultadoPractica(PartidoPractica partido, List<EstadisticaIndividual> estadisticas) {
        if (partido == null) throw new IllegalArgumentException("El partido de práctica no existe.");
        partido.registrarResultado(estadisticas);
    }

    public String consultarAnalitica(Socio j) {
        if (j == null) throw new IllegalArgumentException("El socio es obligatorio.");
        StringBuilder reporte = new StringBuilder();
        reporte.append("=== Analítica del Jugador: ").append(j.getLogin()).append(" ===\n");
        reporte.append("Porcentaje de asistencia: ").append(j.getPorcentajeAsistencia()).append("%\n");
        reporte.append("Partidos oficiales: ").append(j.getEstadisticas().size()).append("\n");
        reporte.append("Partidos de práctica: ").append(j.getPartidosPractica().size()).append("\n");
        // Aquí se agregarán más detalles del rendimiento cuando completemos las estadísticas
        return reporte.toString();
    }

    // --- GETTERS (Se eliminaron los SETTERS por encapsulamiento) ---

    public List<SesionEntrenamiento> getSesiones() { return sesiones; }

    public List<DisciplinaIndividual> getDisciplinas() { return disciplinas; }

    public List<Reserva> getReservas() { return reservas; }
}
