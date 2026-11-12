package club.modelo.instalaciones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import club.modelo.usuarios.Socio;

public class Reserva implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private int cantidadJugadores;
    private String modalidad;
    private Instalacion instalacion;
    private Socio socioResponsable; 
    private List<Socio> participantes = new ArrayList<>();
    /** Partido de práctica jugado sobre esta reserva (HU-S04); una reserva admite a lo sumo uno. */
    private club.modelo.entrenamiento.PartidoPractica partidoPractica;

    public Reserva(LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, int cantidadJugadores, String modalidad, Instalacion instalacion) {
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.cantidadJugadores = cantidadJugadores;
        this.modalidad = modalidad;
        this.instalacion = instalacion;
    }


    public boolean seTraslapaCon(LocalDateTime ini, LocalDateTime fin) {
        // Dos rangos horarios se traslapan si el inicio de uno es antes del fin del otro y viceversa
        return this.fechaHoraInicio != null && this.fechaHoraFin != null && ini != null && fin != null
                && this.fechaHoraInicio.isBefore(fin) && this.fechaHoraFin.isAfter(ini);
    }

    /** Una reserva está activa mientras no haya terminado. */
    public boolean estaActiva(LocalDateTime ahora) {
        return fechaHoraFin != null && ahora != null && fechaHoraFin.isAfter(ahora);
    }

    public club.modelo.entrenamiento.PartidoPractica getPartidoPractica() { return partidoPractica; }

    public void asignarPartidoPractica(club.modelo.entrenamiento.PartidoPractica partido) {
        if (partidoPractica != null) throw new IllegalStateException("La reserva ya tiene un partido de práctica programado.");
        partidoPractica = partido;
    }

    public boolean incluye(Socio s) {
        return s != null && participantes.contains(s);
    }

    public void validar() {
        if (fechaHoraInicio == null || fechaHoraFin == null) {
            throw new IllegalArgumentException("Las fechas de la reserva no pueden ser nulas.");
        }
        if (!fechaHoraInicio.isBefore(fechaHoraFin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        if (instalacion == null) throw new IllegalArgumentException("La instalación es obligatoria.");
        if (socioResponsable == null) throw new IllegalArgumentException("El socio responsable es obligatorio.");
        if (cantidadJugadores <= 0 || !instalacion.admiteJugadores(cantidadJugadores, modalidad)) {
            throw new IllegalArgumentException("La cantidad de jugadores o la modalidad no cumple con las reglas de la instalación.");
        }
        if (!instalacion.estaDisponible(fechaHoraInicio, fechaHoraFin, instalacion.getReservas())) {
            throw new IllegalStateException("La instalación ya está reservada o tiene una sesión en ese horario.");
        }
        if (instalacion.getDisciplina() != null) {
            if (!instalacion.getDisciplina().hayCupoSimultaneo(fechaHoraInicio, fechaHoraFin, instalacion.getReservas())) {
                throw new IllegalStateException("Se alcanzó el máximo de instalaciones simultáneas para la disciplina.");
            }
        }
        for (Socio participante : participantes) {
            for (Reserva existente : participante.getReservas()) {
                if (existente != this && existente.seTraslapaCon(fechaHoraInicio, fechaHoraFin)) throw new IllegalStateException("El socio ya tiene otra reserva en ese horario.");
            }
            for (club.modelo.entrenamiento.SesionEntrenamiento sesion : participante.getSesionesConvocado()) {
                if (sesion.seTraslapaCon(fechaHoraInicio, fechaHoraFin)) throw new IllegalStateException("El socio tiene una sesión de entrenamiento en ese horario.");
            }
            if ((instalacion instanceof CanchaPadel || (instalacion instanceof MesaTenisMesa && instalacion.isTechada()))
                    && participante.tieneBebidaCalienteReciente(fechaHoraInicio)) {
                throw new IllegalStateException("Un participante consumió una bebida caliente recientemente.");
            }
        }
    }

    // --- GETTERS (Se eliminaron setters sensibles por encapsulamiento) ---

    public LocalDateTime getFechaHoraInicio() { return fechaHoraInicio; }

    public LocalDateTime getFechaHoraFin() { return fechaHoraFin; }

    public int getCantidadJugadores() { return cantidadJugadores; }

    public void setCantidadJugadores(int cantidadJugadores) { if (cantidadJugadores <= 0 || cantidadJugadores < participantes.size()) throw new IllegalArgumentException("El cupo debe ser positivo y cubrir a los participantes."); this.cantidadJugadores = cantidadJugadores; }

    public String getModalidad() { return modalidad; }

    public void setModalidad(String modalidad) { if (modalidad == null || modalidad.trim().isEmpty()) throw new IllegalArgumentException("La modalidad es obligatoria."); this.modalidad = modalidad.trim(); }

    public Instalacion getInstalacion() { return instalacion; }

    public Socio getSocioResponsable() { return socioResponsable; }

    public void setSocioResponsable(Socio socioResponsable) {
        if (socioResponsable == null) throw new IllegalArgumentException("El socio responsable es obligatorio.");
        if (!participantes.contains(socioResponsable) && participantes.size() >= cantidadJugadores) throw new IllegalStateException("La reserva no tiene cupo para el responsable.");
        this.socioResponsable = socioResponsable;
        if (!participantes.contains(socioResponsable)) participantes.add(socioResponsable);
    }

    public void agregarParticipante(Socio socio) {
        if (socio == null) throw new IllegalArgumentException("El participante es obligatorio.");
        if (!participantes.contains(socio) && participantes.size() >= cantidadJugadores) throw new IllegalArgumentException("La reserva alcanzó su cupo.");
        if (!participantes.contains(socio)) participantes.add(socio);
        if (participantes.size() > cantidadJugadores) throw new IllegalArgumentException("Hay más participantes que el cupo de la reserva.");
    }

    public List<Socio> getParticipantes() { return participantes; }
}
