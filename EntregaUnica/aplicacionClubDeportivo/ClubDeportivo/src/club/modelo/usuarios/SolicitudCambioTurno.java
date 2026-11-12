package club.modelo.usuarios;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class SolicitudCambioTurno implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String estado = "Pendiente";
    private Empleado solicitante;
    private Turno turnoActual;
    private Turno turnoDeseado;
    private String motivoRechazo;

    public SolicitudCambioTurno(Turno turnoActual, Turno turnoDeseado) {
        this(null, turnoActual, turnoDeseado);
    }

    public SolicitudCambioTurno(Empleado solicitante, Turno turnoActual, Turno turnoDeseado) {
        if (turnoActual == null || turnoDeseado == null) throw new IllegalArgumentException("Se requieren ambos turnos.");
        this.solicitante = solicitante;
        this.turnoActual = turnoActual;
        this.turnoDeseado = turnoDeseado;
    }

    public boolean cumpleCobertura(List<Empleado> empleados) {
        if (empleados == null) return false;
        if (solicitante != null) {
            if (!empleados.contains(solicitante) || !solicitante.getTurnos().contains(turnoActual)) return false;
            for (Turno turno : solicitante.getTurnos()) {
                if (turno != turnoActual && turno.seTraslapaCon(turnoDeseado)) return false;
            }
        }
        return coberturaEn(turnoActual, empleados) && coberturaEn(turnoDeseado, empleados);
    }

    private boolean coberturaEn(Turno rango, List<Empleado> empleados) {
        TreeSet<LocalTime> limites = new TreeSet<>();
        limites.add(rango.getHoraInicio());
        limites.add(rango.getHoraFin());
        for (Empleado empleado : empleados) {
            for (Turno turno : empleado.getTurnos()) {
                if (turno.getDia() == rango.getDia()) {
                    if (turno.getHoraInicio().isAfter(rango.getHoraInicio()) && turno.getHoraInicio().isBefore(rango.getHoraFin())) limites.add(turno.getHoraInicio());
                    if (turno.getHoraFin().isAfter(rango.getHoraInicio()) && turno.getHoraFin().isBefore(rango.getHoraFin())) limites.add(turno.getHoraFin());
                }
            }
        }
        List<LocalTime> puntos = new ArrayList<>(limites);
        for (int i = 0; i < puntos.size() - 1; i++) {
            LocalTime inicio = puntos.get(i);
            LocalTime fin = puntos.get(i + 1);
            LocalTime muestra = inicio.plusSeconds(java.time.Duration.between(inicio, fin).getSeconds() / 2);
            int entrenadores = 0;
            int fisioterapeutas = 0;
            for (Empleado empleado : empleados) {
                boolean activo = empleado == solicitante
                        ? (turnoDeseado.getDia() == rango.getDia() && turnoDeseado.cubre(rango.getDia(), muestra))
                        : tieneTurno(empleado, rango.getDia(), muestra);
                if (activo) {
                    if (empleado instanceof Entrenador) entrenadores++;
                    if (empleado instanceof Fisioterapeuta) fisioterapeutas++;
                }
            }
            if (entrenadores < 2 || fisioterapeutas < 1) return false;
        }
        return true;
    }

    private boolean tieneTurno(Empleado empleado, java.time.DayOfWeek dia, LocalTime hora) {
        for (Turno turno : empleado.getTurnos()) if (turno.cubre(dia, hora)) return true;
        return false;
    }

    public void aprobar() {
        if (!"Pendiente".equals(estado)) throw new IllegalStateException("La solicitud ya fue resuelta.");
        estado = "Aprobada";
    }

    /** Rechaza la solicitud con un motivo que el empleado puede consultar (HU-A12, HU-E04). */
    public void rechazar(String motivo) {
        if (!"Pendiente".equals(estado)) throw new IllegalStateException("La solicitud ya fue resuelta.");
        if (motivo == null || motivo.trim().isEmpty()) throw new IllegalArgumentException("El motivo del rechazo es obligatorio.");
        motivoRechazo = motivo.trim();
        estado = "Rechazada";
    }

    public String getMotivoRechazo() { return motivoRechazo; }

    public void aplicarCambio() {
        if (!"Aprobada".equals(estado) || solicitante == null) throw new IllegalStateException("La solicitud aprobada no tiene empleado asignado.");
        solicitante.retirarTurno(turnoActual);
        try {
            solicitante.agregarTurno(turnoDeseado);
        } catch (RuntimeException e) {
            solicitante.agregarTurno(turnoActual);
            throw e;
        }
    }

    public String getEstado() { return estado; }
    public Empleado getSolicitante() { return solicitante; }
    public Turno getTurnoActual() { return turnoActual; }
    public Turno getTurnoDeseado() { return turnoDeseado; }
}
