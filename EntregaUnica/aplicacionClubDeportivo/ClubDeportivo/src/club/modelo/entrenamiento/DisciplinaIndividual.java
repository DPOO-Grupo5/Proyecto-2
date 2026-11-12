package club.modelo.entrenamiento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import club.modelo.usuarios.Socio;
import club.modelo.instalaciones.Instalacion;
import club.modelo.instalaciones.Reserva;

public abstract class DisciplinaIndividual implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int maxInstalaciones;
    private List<InscripcionDisciplina> inscripciones = new ArrayList<>();
    private List<Instalacion> instalaciones = new ArrayList<>();

    public DisciplinaIndividual(int maxInstalaciones) {
        if (maxInstalaciones <= 0) throw new IllegalArgumentException("El máximo de instalaciones debe ser positivo.");
        this.maxInstalaciones = maxInstalaciones;
    }

    /**
     * Valida si hay cupo simultáneo para una nueva reserva en el rango dado (RD-IND-04).
     */
    public boolean hayCupoSimultaneo(LocalDateTime ini, LocalDateTime fin, List<Reserva> reservas) {
        if (ini == null || fin == null || !ini.isBefore(fin)) return false;
        Set<Instalacion> ocupadas = new HashSet<>();
        for (Instalacion instalacion : instalaciones) {
            List<Reserva> existentes = instalacion.getReservas();
            if (!instalacion.estaDisponible(ini, fin, existentes)) ocupadas.add(instalacion);
        }
        if (reservas != null) for (Reserva reserva : reservas) {
            if (reserva.getInstalacion() != null && reserva.getInstalacion().getDisciplina() == this && reserva.seTraslapaCon(ini, fin)) ocupadas.add(reserva.getInstalacion());
        }
        return ocupadas.size() < maxInstalaciones;
    }

    public void registrarInscripcion(InscripcionDisciplina inscripcion) {
        if (inscripcion != null && !inscripciones.contains(inscripcion)) inscripciones.add(inscripcion);
    }

    public void registrarInstalacion(Instalacion instalacion) {
        if (instalacion == null) throw new IllegalArgumentException("La instalación es obligatoria.");
        if (instalacion.getDisciplina() != null && instalacion.getDisciplina() != this) throw new IllegalStateException("La instalación ya pertenece a otra disciplina.");
        if (!instalaciones.contains(instalacion)) instalaciones.add(instalacion);
        instalacion.setDisciplina(this);
    }

    public List<InscripcionDisciplina> getInscripciones() { return inscripciones; }
    public List<Instalacion> getInstalaciones() { return instalaciones; }

    public boolean tieneInscripcionActiva(Socio socio) {
        for (InscripcionDisciplina i : inscripciones) if (i.getSocio() == socio && i.isActiva()) return true;
        return false;
    }

    public int getMaxInstalaciones() { return maxInstalaciones; }

    public void setMaxInstalaciones(int maxInstalaciones) { if (maxInstalaciones <= 0) throw new IllegalArgumentException("El máximo de instalaciones debe ser positivo."); this.maxInstalaciones = maxInstalaciones; }
}
