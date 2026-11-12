package club.modelo.competencias;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;

public class PartidoCompetencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDate fecha;
    private String marcador = "Pendiente";
    private String escenario;
    private int marcadorLocal;
    private int marcadorVisitante;
    private boolean finalizado;
    private Competencia competencia;
    private Grupo grupo;
    private List<EquipoCompetencia> equipos = new ArrayList<>();
    private List<EstadisticaJugadorPartido> estadisticas = new ArrayList<>();
    private Set<Socio> sancionadosEnEstePartido = new HashSet<>();
    private EquipoCompetencia ganadorPorDesempate;
    /** Falso mientras el partido solo está propuesto por el calendario; el administrador lo programa (HU-A19). */
    private boolean programado;
    private Entrenador entrenadorResponsable;
    /** Alineación del equipo del club (HU-T05). Las instituciones rivales no registran alineación. */
    private List<Socio> alineacionLocal = new ArrayList<>();
    private List<Socio> alineacionVisitante = new ArrayList<>();

    public PartidoCompetencia(LocalDate fecha, String marcador, String escenario) {
        this.fecha = fecha;
        this.escenario = escenario;
        if (marcador != null && !marcador.trim().isEmpty() && !"pendiente".equalsIgnoreCase(marcador)) setMarcador(marcador);
    }

    public void agregarEquipo(EquipoCompetencia equipo) {
        if (equipo == null) throw new IllegalArgumentException("El equipo es obligatorio.");
        if (finalizado) throw new IllegalStateException("El partido ya finalizó.");
        if (equipos.contains(equipo)) return;
        if (equipos.size() >= 2) throw new IllegalStateException("Un partido solo admite dos participantes.");
        equipos.add(equipo);
    }

    public void validarPuedeJugarse() {
        if (equipos.size() != 2 || equipos.get(0) == equipos.get(1)) throw new IllegalStateException("El partido requiere dos equipos distintos.");
        if (competencia != null) {
            for (EquipoCompetencia equipo : equipos) if (!competencia.getEquipos().contains(equipo)) throw new IllegalStateException("El equipo no está inscrito en la competencia.");
        }
        for (EquipoCompetencia participante : equipos) {
            if (!(participante instanceof Equipo)) continue;
            Equipo equipo = (Equipo) participante;
            if (!equipo.isActivo() || equipo.getDeporte() == null) throw new IllegalStateException("El equipo debe estar activo y configurado.");
            if (competencia != null && (equipo.getDeporte() != competencia.getDeporte() || equipo.getCategoria() != competencia.getCategoria())) throw new IllegalStateException("El equipo no corresponde a esta competencia.");
            int disponibles = 0;
            for (InscripcionEquipo inscripcion : equipo.getInscripcionesActivas()) if (!inscripcion.getSocio().tieneSancionVigente()) disponibles++;
            if (disponibles < equipo.getDeporte().getMinJugadores()) throw new IllegalStateException("El equipo no cumple la nómina mínima disponible.");
        }
    }

    /**
     * Visto bueno del administrador (HU-A19): fija fecha, escenario y entrenador responsable.
     * Exige que el partido pertenezca a una competencia y que sus dos equipos estén inscritos en ella.
     */
    public void programar(LocalDate fecha, String escenario, Entrenador responsable) {
        if (finalizado) throw new IllegalStateException("El partido ya finalizó.");
        if (fecha == null) throw new IllegalArgumentException("La fecha del partido es obligatoria.");
        if (escenario == null || escenario.trim().isEmpty()) throw new IllegalArgumentException("El escenario es obligatorio.");
        if (responsable == null) throw new IllegalArgumentException("El entrenador responsable es obligatorio.");
        if (competencia == null) throw new IllegalStateException("El partido no pertenece a una competencia.");
        validarPuedeJugarse();
        this.fecha = fecha;
        this.escenario = escenario.trim();
        this.entrenadorResponsable = responsable;
        this.programado = true;
    }

    public void registrarMarcador(int local, int visitante) {
        if (finalizado) throw new IllegalStateException("El resultado ya fue registrado.");
        if (!programado) throw new IllegalStateException("El partido no ha sido programado por el administrador (falta su visto bueno).");
        if (local < 0 || visitante < 0) throw new IllegalArgumentException("Los marcadores no pueden ser negativos.");
        validarPuedeJugarse();
        if (competencia != null) competencia.getDeporte().validarMarcador(local, visitante);
        capturarSancionados(equipos.get(0));
        capturarSancionados(equipos.get(1));
        marcadorLocal = local;
        marcadorVisitante = visitante;
        marcador = local + " - " + visitante;
        finalizado = true;
        if (competencia != null && competencia.getFormato() instanceof FormatoLiga) ((FormatoLiga) competencia.getFormato()).actualizarPosiciones();
        if (grupo != null) grupo.actualizarPosiciones();
        consumirSuspensiones(equipos.get(0));
        consumirSuspensiones(equipos.get(1));
    }

    public EquipoCompetencia getGanador() {
    	if (!finalizado) return null;
    	if (marcadorLocal == marcadorVisitante) return ganadorPorDesempate;
        return marcadorLocal > marcadorVisitante ? equipos.get(0) : equipos.get(1);
    }

    public void agregarEstadistica(EstadisticaJugadorPartido estadistica) {
        if (!finalizado) throw new IllegalStateException("Se debe registrar el resultado antes de las estadísticas.");
        if (competencia == null || estadistica == null || estadistica.getJugador() == null) throw new IllegalArgumentException("Partido, competencia y jugador son obligatorios.");
        Socio jugador = estadistica.getJugador();
        if (sancionadosEnEstePartido.contains(jugador)) throw new IllegalArgumentException("Un jugador sancionado antes del partido no puede registrar participación.");
        if (!perteneceAEquipoParticipante(jugador)) throw new IllegalArgumentException("El jugador no pertenece a un equipo participante.");
        if (!competencia.estaEnNomina(jugador)) throw new IllegalArgumentException("El jugador no está en la nómina de la competencia.");
        if (!alineacionLocal.contains(jugador) && !alineacionVisitante.contains(jugador))
            throw new IllegalArgumentException(jugador.getNombre() + " no fue alineado en este partido.");
        
        validarTipoEstadistica(estadistica);
        for (EstadisticaJugadorPartido existente : estadisticas) if (existente.getJugador() == jugador) throw new IllegalStateException("Ya hay estadísticas para este jugador.");
        estadistica.setPartidoCompetencia(this);
        estadisticas.add(estadistica);
        jugador.registrarEstadistica(estadistica);
        if (estadistica instanceof EstadisticaJugadorFutbol) registrarSancionDisciplinaria((EstadisticaJugadorFutbol) estadistica, jugador);
    }
    
    private void registrarSancionDisciplinaria(EstadisticaJugadorFutbol estadistica, Socio jugador) {
        int acumuladas = 0;
        for (PartidoCompetencia partido : competencia.getPartidos())
            for (EstadisticaJugadorPartido e : partido.getEstadisticas())
                if (e.getJugador() == jugador && e instanceof EstadisticaJugadorFutbol)
                    acumuladas += ((EstadisticaJugadorFutbol) e).getTarjetasAmarillas();
        int antes = acumuladas - estadistica.getTarjetasAmarillas();
        boolean porAmarillas = acumuladas / 2 > antes / 2;   // cada 2 amarillas acumuladas en la competencia
        boolean roja = estadistica.getTarjetasRojas() > 0;
        if (roja || porAmarillas) {
            Sancion sancion = new Sancion(roja ? "Tarjeta roja" : "Acumulación de amarillas en la competencia", 1, false, jugador);
            estadistica.setSancion(sancion);
            jugador.registrarSancion(sancion);
        }
    }

    public void resolverEmpate(EquipoCompetencia ganador) {
        if (!finalizado || marcadorLocal != marcadorVisitante) throw new IllegalStateException("Solo se resuelve un partido finalizado en empate.");
        if (ganador == null || !equipos.contains(ganador)) throw new IllegalArgumentException("El ganador debe ser uno de los equipos del partido.");
        ganadorPorDesempate = ganador;
    }

    private boolean perteneceAEquipoParticipante(Socio jugador) {
        for (EquipoCompetencia participante : equipos) if (participante instanceof Equipo) {
            for (InscripcionEquipo inscripcion : ((Equipo) participante).getInscripcionesActivas()) if (inscripcion.getSocio() == jugador) return true;
        }
        return false;
    }

    private void validarTipoEstadistica(EstadisticaJugadorPartido estadistica) {
        DeporteConjunto deporte = competencia.getDeporte();
        boolean coincide = (deporte instanceof Futbol && estadistica instanceof EstadisticaJugadorFutbol)
                || (deporte instanceof Baloncesto && estadistica instanceof EstadisticaJugadorBaloncesto)
                || (deporte instanceof Voleibol && estadistica instanceof EstadisticaJugadorVoleibol);
        if (!coincide) throw new IllegalArgumentException("La estadística no corresponde al deporte de la competencia.");
    }

    private void capturarSancionados(EquipoCompetencia participante) {
        if (!(participante instanceof Equipo)) return;
        for (InscripcionEquipo inscripcion : ((Equipo) participante).getInscripcionesActivas()) {
            Socio socio = inscripcion.getSocio();
            if (socio.tieneSancionVigente()) sancionadosEnEstePartido.add(socio);
        }
    }

    private void consumirSuspensiones(EquipoCompetencia participante) {
        if (!(participante instanceof Equipo)) return;
        for (InscripcionEquipo inscripcion : ((Equipo) participante).getInscripcionesActivas()) {
            for (Sancion sancion : inscripcion.getSocio().getSancionesVigentes()) sancion.consumirPartido();
        }
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getMarcador() { return marcador; }
    public void setMarcador(String marcador) {
        if (finalizado) throw new IllegalStateException("El resultado ya fue registrado.");
        if (marcador == null || !marcador.trim().matches("\\d+\\s*-\\s*\\d+")) throw new IllegalArgumentException("El marcador debe tener el formato 'n - n'.");
        String[] valores = marcador.trim().split("\\s*-\\s*");
        registrarMarcador(Integer.parseInt(valores[0]), Integer.parseInt(valores[1]));
    }
    public String getEscenario() { return escenario; }
    public void setEscenario(String escenario) { this.escenario = escenario; }
    public int getMarcadorLocal() { return marcadorLocal; }
    public int getMarcadorVisitante() { return marcadorVisitante; }
    public boolean isFinalizado() { return finalizado; }
    public Competencia getCompetencia() { return competencia; }
    /** Define la alineación de un equipo del club: jugadores activos, de la nómina de la competencia y sin suspensión. */
    public void definirAlineacion(Equipo equipo, List<Socio> jugadores) {
        if (finalizado) throw new IllegalStateException("El partido ya finalizó.");
        int indice = equipos.indexOf(equipo);
        if (equipo == null || indice < 0) throw new IllegalArgumentException("El equipo no juega este partido.");
        if (jugadores == null || jugadores.isEmpty()) throw new IllegalArgumentException("La alineación no puede estar vacía.");
        InscripcionCompetencia inscripcion = null;
        if (competencia != null) for (InscripcionCompetencia i : competencia.getInscripciones()) if (i.getEquipo() == equipo) inscripcion = i;
        Set<Socio> distintos = new HashSet<>();
        for (Socio j : jugadores) {
            boolean activo = false;
            for (InscripcionEquipo i : equipo.getInscripcionesActivas()) if (i.getSocio() == j) activo = true;
            if (j == null || !activo) throw new IllegalArgumentException((j == null ? "Un jugador" : j.getNombre()) + " no pertenece al equipo " + equipo.getNombre() + ".");
            if (inscripcion == null || !inscripcion.enNomina(j)) throw new IllegalArgumentException(j.getNombre() + " no está en la nómina de la competencia.");
            if (!distintos.add(j)) throw new IllegalArgumentException(j.getNombre() + " está repetido en la alineación.");
            if (j.tieneSancionVigente()) throw new IllegalArgumentException(j.getNombre() + " está suspendido.");
        }
        List<Socio> destino = indice == 0 ? alineacionLocal : alineacionVisitante;
        destino.clear();
        destino.addAll(jugadores);
    }

    public List<Socio> getAlineacion(EquipoCompetencia equipo) {
        int indice = equipos.indexOf(equipo);
        if (indice < 0) throw new IllegalArgumentException("El equipo no juega este partido.");
        return indice == 0 ? alineacionLocal : alineacionVisitante;
    }

    public boolean isProgramado() { return programado; }
    public Entrenador getEntrenadorResponsable() { return entrenadorResponsable; }

    public void setCompetencia(Competencia competencia) { if (this.competencia != null && this.competencia != competencia) throw new IllegalStateException("El partido ya pertenece a otra competencia."); this.competencia = competencia; }
    public Grupo getGrupo() { return grupo; }
    public void setGrupo(Grupo grupo) { if (this.grupo != null && this.grupo != grupo) throw new IllegalStateException("El partido ya pertenece a otro grupo."); this.grupo = grupo; }
    public List<EquipoCompetencia> getEquipos() { return equipos; }
    public void setEquipos(List<EquipoCompetencia> equipos) {
        if (equipos == null || equipos.size() > 2 || new HashSet<>(equipos).size() != equipos.size()) throw new IllegalArgumentException("El partido admite hasta dos participantes distintos.");
        this.equipos = new ArrayList<>(equipos);
    }
    public List<EstadisticaJugadorPartido> getEstadisticas() { return estadisticas; }
    public void setEstadisticas(List<EstadisticaJugadorPartido> estadisticas) { this.estadisticas = estadisticas == null ? new ArrayList<>() : estadisticas; }
}
