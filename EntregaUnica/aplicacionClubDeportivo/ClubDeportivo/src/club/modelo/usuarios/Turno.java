package club.modelo.usuarios;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Turno implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private DayOfWeek dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    public Turno(DayOfWeek dia, LocalTime horaInicio, LocalTime horaFin) {
        setDia(dia);
        setHoraInicio(horaInicio);
        setHoraFin(horaFin);
        validarRango();
    }

    private void validarRango() {
        if (horaInicio == null || horaFin == null || !horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException("El inicio del turno debe ser anterior al fin.");
        }
    }

    /** Horario de atención del club (decisión del grupo, Proyecto 2): lunes a sábado de 05:00 a 23:00. */
    public static final LocalTime APERTURA = LocalTime.of(5, 0);
    public static final LocalTime CIERRE = LocalTime.of(23, 0);
    public static final List<DayOfWeek> DIAS_DE_ATENCION = Collections.unmodifiableList(Arrays.asList(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY));

    public boolean estaDentroDelHorarioDeAtencion() {
        return DIAS_DE_ATENCION.contains(dia) && !horaInicio.isBefore(APERTURA) && !horaFin.isAfter(CIERRE);
    }

    public static String descripcionHorarioDeAtencion() {
        return "lunes a sábado de " + APERTURA + " a " + CIERRE;
    }

    public static String nombreDia(DayOfWeek d) {
        switch (d) {
            case MONDAY: return "lunes";
            case TUESDAY: return "martes";
            case WEDNESDAY: return "miércoles";
            case THURSDAY: return "jueves";
            case FRIDAY: return "viernes";
            case SATURDAY: return "sábado";
            default: return "domingo";
        }
    }

    @Override
    public String toString() {
        return nombreDia(dia) + " " + horaInicio + "-" + horaFin;
    }

    public boolean mismoHorario(Turno otro) {
        return otro != null && dia == otro.dia && horaInicio.equals(otro.horaInicio) && horaFin.equals(otro.horaFin);
    }

    public boolean seTraslapaCon(Turno otro) {
        return otro != null && dia == otro.dia && horaInicio.isBefore(otro.horaFin) && horaFin.isAfter(otro.horaInicio);
    }

    public boolean cubre(DayOfWeek d, LocalTime hora) {
        return d == dia && hora != null && !hora.isBefore(horaInicio) && hora.isBefore(horaFin);
    }

    public DayOfWeek getDia() { return dia; }
    public void setDia(DayOfWeek dia) { if (dia == null) throw new IllegalArgumentException("El día es obligatorio."); this.dia = dia; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { if (horaInicio == null) throw new IllegalArgumentException("La hora de inicio es obligatoria."); this.horaInicio = horaInicio; if (horaFin != null) validarRango(); }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { if (horaFin == null) throw new IllegalArgumentException("La hora de fin es obligatoria."); this.horaFin = horaFin; if (horaInicio != null) validarRango(); }
}
