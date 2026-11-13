package club.modelo.usuarios;

import club.model.Reloj;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import club.modelo.competencias.Equipo;
import club.modelo.competencias.EstadisticaJugadorPartido;
import club.modelo.competencias.InscripcionEquipo;
import club.modelo.competencias.Sancion;
import club.modelo.entrenamiento.Asistencia;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.EstadisticaIndividual;
import club.modelo.entrenamiento.InscripcionDisciplina;
import club.modelo.entrenamiento.PartidoPractica;
import club.modelo.entrenamiento.SesionEntrenamiento;
import club.modelo.instalaciones.Instalacion;
import club.modelo.instalaciones.CanchaPadel;
import club.modelo.instalaciones.MesaTenisMesa;
import club.modelo.instalaciones.Reserva;
import club.modelo.tienda.Transaccion;

public class Socio extends Usuario implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDate fechaNacimiento;

    private List<InscripcionDisciplina> inscripcionesDisciplina = new ArrayList<>();
    private List<Reserva> reservas = new ArrayList<>();
    private List<InscripcionEquipo> inscripcionesEquipo = new ArrayList<>();
    private List<EstadisticaJugadorPartido> estadisticas = new ArrayList<>();
    private List<EstadisticaIndividual> estadisticasIndividuales = new ArrayList<>();
    private List<Asistencia> asistencias = new ArrayList<>();
    private List<Sancion> sanciones = new ArrayList<>();
    private List<SesionEntrenamiento> sesionesConvocado = new ArrayList<>();
    private List<PartidoPractica> partidosPractica = new ArrayList<>();
    private String documento;

    public Socio(String login, String password, String nombre, LocalDate fechaNacimiento) {
        super(login, password, nombre);
        if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha de nacimiento debe ser válida y no futura.");
        this.fechaNacimiento = fechaNacimiento;
    }



    public int calcularEdad(LocalDate fechaReferencia) {
        if (this.fechaNacimiento == null) return 0;
        return Period.between(this.fechaNacimiento, fechaReferencia).getYears();
    }
    public String getDocumento() { return documento; }
    public void setDocumento(String d) {
        if (d == null || d.trim().isEmpty()) throw new IllegalArgumentException("El documento es obligatorio.");
        this.documento = d.trim();
    }
    
    public boolean tienePartidosEn(DisciplinaIndividual d) {
        for (PartidoPractica p : partidosPractica) if (p.getDisciplina() == d && p.isResultadoRegistrado()) return true;
        return false;
    }
    public List<SesionEntrenamiento> sesionesPendientesDeAsistencia() {
        List<SesionEntrenamiento> r = new ArrayList<>();
        for (SesionEntrenamiento s : sesionesConvocado) {
            boolean tiene = false;
            for (Asistencia a : asistencias) if (a.getSesion() == s) tiene = true; // ⚠️ confirma el getter de Asistencia
            if (!tiene) r.add(s);
        }
        return r;
    }

    /** Inscribe al socio en una academia individual con el nivel inicial que indica y ELO 1000 (HU-S02). */
    public InscripcionDisciplina inscribirseEnDisciplina(DisciplinaIndividual d, String nivelInicial) {
        if (d == null) throw new IllegalArgumentException("La disciplina es obligatoria.");
        for (InscripcionDisciplina i : inscripcionesDisciplina) {
            if (i.getDisciplina() == d && i.isActiva()) throw new IllegalStateException("El socio ya está inscrito en esta disciplina.");
        }
        InscripcionDisciplina inscripcion = new InscripcionDisciplina(nivelInicial, 1000, this, d);
        inscripcionesDisciplina.add(inscripcion);
        d.registrarInscripcion(inscripcion);
        return inscripcion;
    }

    public Reserva reservarInstalacion(Instalacion inst, LocalDateTime ini, LocalDateTime fin, int jugadores, String modalidad) {
        if (inst == null) throw new IllegalArgumentException("La instalación es obligatoria.");
        if (inst.getDisciplina() != null && !inst.getDisciplina().tieneInscripcionActiva(this))
            throw new IllegalStateException("El socio debe estar inscrito en la disciplina de esta instalación.");
        Reserva reserva = new Reserva(ini, fin, jugadores, modalidad, inst);
        reserva.setSocioResponsable(this);
        reserva.validar();
        reservas.add(reserva);
        inst.registrarReserva(reserva);
        return reserva;
    }

    /**
     * Programa un partido de práctica sobre una reserva activa del socio (HU-S04). Los participantes son el socio y
     * los otros indicados; en dobles los dos primeros forman un lado. El entrenador participante es opcional y
     * permite niveles que no son iguales ni adyacentes.
     */
    public PartidoPractica programarPartidoPractica(Reserva reserva, List<Socio> otros, Entrenador entrenadorParticipante) {
        if (reserva == null) throw new IllegalArgumentException("Debe indicar una reserva activa.");
        if (reserva.getSocioResponsable() != this) throw new IllegalStateException("La reserva no pertenece a este socio.");
        if (!reserva.estaActiva(Reloj.ahora())) throw new IllegalStateException("La reserva ya terminó.");
        if (reserva.getPartidoPractica() != null) throw new IllegalStateException("La reserva ya tiene un partido de práctica programado.");
        DisciplinaIndividual d = reserva.getInstalacion().getDisciplina();
        if (d == null) throw new IllegalArgumentException("La instalación reservada no pertenece a una academia individual.");
        List<Socio> socios = new ArrayList<>();
        socios.add(this);
        if (otros != null) socios.addAll(otros);
        List<InscripcionDisciplina> inscripciones = new ArrayList<>();
        for (Socio s : socios) {
            if (s == null) throw new IllegalArgumentException("Los participantes son obligatorios.");
            InscripcionDisciplina i = s.buscarInscripcion(d);
            if (i == null) throw new IllegalStateException(s.getNombre() + " no está inscrito en " + d.getClass().getSimpleName() + ".");
            inscripciones.add(i);
        }
        PartidoPractica partido = new PartidoPractica(reserva, inscripciones, entrenadorParticipante);
        for (Socio s : socios)
            if (s.tieneCompromisoEntre(reserva.getFechaHoraInicio(), reserva.getFechaHoraFin(), reserva))
                throw new IllegalStateException(s.getNombre() + " tiene otro partido, reserva o sesión en ese horario.");
        reserva.asignarPartidoPractica(partido);
        for (Socio s : socios) {
            if (s != this) reserva.agregarParticipante(s);
            s.partidosPractica.add(partido);
        }
        return partido;
    }

    /** Indica si el socio tiene otro partido de práctica, reserva o sesión que se cruce con el bloque. */
    public boolean tieneCompromisoEntre(LocalDateTime inicio, LocalDateTime fin, Reserva excluida) {
        for (PartidoPractica p : partidosPractica) {
            if (p.getReserva() == excluida || p.getFechaHora() == null) continue;
            if (p.getFechaHora().isBefore(fin) && p.getFechaHoraFin().isAfter(inicio)) return true;
        }
        for (Reserva r : reservas) if (r != excluida && r.seTraslapaCon(inicio, fin)) return true;
        for (SesionEntrenamiento sesion : sesionesConvocado) if (sesion.seTraslapaCon(inicio, fin)) return true;
        return false;
    }

    /**
     * Partidos de práctica con resultado en una disciplina, en orden cronológico (HU-S07).
     * Las fechas son opcionales e inclusivas; se rechaza una disciplina no inscrita o un rango invertido.
     */
    public List<PartidoPractica> historialPracticas(DisciplinaIndividual d, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) throw new IllegalArgumentException("La fecha final es anterior a la inicial.");
        if (d == null) throw new IllegalArgumentException("La disciplina es obligatoria.");
        if (buscarInscripcion(d) == null) throw new IllegalStateException("No está inscrito en " + d.getClass().getSimpleName() + ".");
        List<PartidoPractica> lista = new ArrayList<>();
        for (PartidoPractica p : partidosPractica) {
            if (p.getDisciplina() != d || !p.isResultadoRegistrado() || p.getFechaHora() == null) continue;
            LocalDate dia = p.getFechaHora().toLocalDate();
            if ((desde == null || !dia.isBefore(desde)) && (hasta == null || !dia.isAfter(hasta))) lista.add(p);
        }
        lista.sort((x, y) -> x.getFechaHora().compareTo(y.getFechaHora()));
        return lista;
    }

    /** HU-S08: sesiones a las que fue convocado (futuras y pasadas), en orden cronológico. */
    public List<SesionEntrenamiento> consultarConvocatorias() {
        List<SesionEntrenamiento> lista = new ArrayList<>(sesionesConvocado);
        lista.sort((a, b) -> a.getFechaHora().compareTo(b.getFechaHora()));
        return lista;
    }

    public InscripcionDisciplina inscripcionEn(DisciplinaIndividual d) { return buscarInscripcion(d); }

    private InscripcionDisciplina buscarInscripcion(DisciplinaIndividual disciplina) {
        for (InscripcionDisciplina i : inscripcionesDisciplina) if (i.getDisciplina() == disciplina && i.isActiva()) return i;
        return null;
    }

    private boolean instalacionRestringida(Instalacion instalacion) {
        return instalacion instanceof CanchaPadel || (instalacion instanceof MesaTenisMesa && instalacion.isTechada());
    }

    public void registrarInscripcionEquipo(InscripcionEquipo inscripcion) {
        if (inscripcion == null || inscripcion.getSocio() != this) throw new IllegalArgumentException("La inscripción debe pertenecer al socio.");
        if (!inscripcionesEquipo.contains(inscripcion)) inscripcionesEquipo.add(inscripcion);
    }

    public void registrarSesionConvocado(SesionEntrenamiento sesion) {
        if (sesion == null || !sesion.incluye(this)) throw new IllegalArgumentException("La sesión debe incluir al socio como convocado.");
        if (!sesionesConvocado.contains(sesion)) sesionesConvocado.add(sesion);
    }

    public void registrarEstadistica(EstadisticaJugadorPartido estadistica) {
        if (estadistica == null || estadistica.getJugador() != this) throw new IllegalArgumentException("La estadística debe pertenecer al socio.");
        if (!estadisticas.contains(estadistica)) estadisticas.add(estadistica);
    }

    public void registrarEstadisticaIndividual(EstadisticaIndividual estadistica) {
        if (estadistica == null || estadistica.getInscripcion() == null || estadistica.getInscripcion().getSocio() != this) throw new IllegalArgumentException("La estadística individual debe pertenecer al socio.");
        if (!estadisticasIndividuales.contains(estadistica)) estadisticasIndividuales.add(estadistica);
    }

    public void registrarSancion(Sancion sancion) {
        if (sancion == null) throw new IllegalArgumentException("La sanción es obligatoria.");
        if (sancion.getSocio() == null) sancion.setSocio(this);
        if (sancion.getSocio() != this) throw new IllegalArgumentException("La sanción debe pertenecer al socio.");
        if (!sanciones.contains(sancion)) sanciones.add(sancion);
    }

    public boolean tieneSancionVigente() {
        for (Sancion sancion : sanciones) if (sancion.estaVigente()) return true;
        return false;
    }

    public List<Equipo> getEquipos() {
        List<Equipo> misEquipos = new ArrayList<>();
        
        for (InscripcionEquipo ie : inscripcionesEquipo) {
           if (ie.isActiva() && ie.getEquipo().isActivo()) misEquipos.add(ie.getEquipo());
        }
        return misEquipos;
    }

    public List<Sancion> getSancionesVigentes() {
        List<Sancion> vigentes = new ArrayList<>();
        for (Sancion sancion : sanciones) if (sancion.estaVigente()) vigentes.add(sancion);
        return vigentes;
    }

    public boolean tieneBebidaCalienteReciente(LocalDateTime ahora) {
        for (Transaccion t : this.getTransacciones()) {
            long minutos = ChronoUnit.MINUTES.between(t.getFechaHora(), ahora);
            if (t.isConfirmada() && minutos >= 0 && minutos <= 30 && t.contieneBebidaCaliente()) {
                return true;
            }
        }
  
        return false;
    }

    public boolean estaEnInstalacion(LocalDateTime ahora) {
        if (ahora == null) return false;
        for (Reserva r : reservas) {
            if (!ahora.isBefore(r.getFechaHoraInicio()) && ahora.isBefore(r.getFechaHoraFin())) {
                return true;
            }
        }
        for (SesionEntrenamiento sesion : sesionesConvocado) {
            if (sesion.incluye(this) && sesion.estaVigente(ahora)) return true;
        }

        return false;
    }

    public Instalacion getInstalacionActual(LocalDateTime ahora) {
        if (ahora == null) return null;
        for (Reserva r : reservas) {
            if (r.incluye(this) && !ahora.isBefore(r.getFechaHoraInicio()) && ahora.isBefore(r.getFechaHoraFin())) return r.getInstalacion();
        }
        for (SesionEntrenamiento sesion : sesionesConvocado) {
            if (sesion.incluye(this) && sesion.estaVigente(ahora)) return sesion.getInstalacion();
        }
        return null;
    }

    public boolean estaEnZonaRestringidaParaBebidasCalientes(LocalDateTime ahora) {
        if (ahora == null) return false;
        Instalacion instalacion = getInstalacionActual(ahora);
        return instalacion != null && instalacionRestringida(instalacion);
    }

    public double getPorcentajeAsistencia() {
        if (asistencias.isEmpty()) return 0.0;
        long asistidas = 0;
        
        for (Asistencia a : asistencias) {
        	if ("asistió".equalsIgnoreCase(a.getEstado()) || "llegó tarde".equalsIgnoreCase(a.getEstado())) {
                asistidas++;
            }
        }
        return (double) asistidas / asistencias.size() * 100;
    }

    // --- GETTERS (Se eliminaron los SETTERS por encapsulamiento) ---

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }

    public List<InscripcionDisciplina> getInscripcionesDisciplina() { return inscripcionesDisciplina; }

    public List<Reserva> getReservas() { return reservas; }

    public List<InscripcionEquipo> getInscripcionesEquipo() { return inscripcionesEquipo; }

    public List<EstadisticaJugadorPartido> getEstadisticas() { return estadisticas; }
    public List<EstadisticaIndividual> getEstadisticasIndividuales() { return estadisticasIndividuales; }

    public List<Asistencia> getAsistencias() { return asistencias; }

    public List<Sancion> getSanciones() { return sanciones; }

    public List<SesionEntrenamiento> getSesionesConvocado() { return sesionesConvocado; }

    public List<PartidoPractica> getPartidosPractica() { return partidosPractica; }
}
