package club.modelo.usuarios;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Borrador de la programación semanal de turnos (HU-A11). El administrador asigna y quita turnos sobre el borrador
 * y lo publica; la publicación solo se acepta si en todo instante del horario de atención (lunes a sábado,
 * 05:00–23:00) hay al menos {@value #MIN_ENTRENADORES} entrenadores y {@value #MIN_FISIOTERAPEUTAS} fisioterapeuta.
 * Si se rechaza, los turnos vigentes de los empleados no cambian.
 */
public class ProgramacionSemanal {
    public static final int MIN_ENTRENADORES = 2;
    public static final int MIN_FISIOTERAPEUTAS = 1;

    private final Map<Empleado, List<Turno>> borrador = new LinkedHashMap<>();

    /** Crea el borrador a partir de los turnos vigentes de los empleados, para modificarlos sobre esa base. */
    public ProgramacionSemanal(Collection<? extends Empleado> empleados) {
        if (empleados == null) throw new IllegalArgumentException("La lista de empleados es obligatoria.");
        for (Empleado e : empleados) borrador.put(e, new ArrayList<>(e.getTurnos()));
    }

    public void asignar(Empleado empleado, Turno turno) {
        List<Turno> turnos = turnosDe(empleado);
        if (turno == null) throw new IllegalArgumentException("El turno es obligatorio.");
        if (!turno.estaDentroDelHorarioDeAtencion())
            throw new IllegalArgumentException("El turno " + turno + " está fuera del horario de atención (" + Turno.descripcionHorarioDeAtencion() + ").");
        for (Turno t : turnos)
            if (t.seTraslapaCon(turno)) throw new IllegalArgumentException(empleado.getNombre() + " ya tiene un turno que se traslapa: " + t + ".");
        turnos.add(turno);
    }

    public void quitar(Empleado empleado, Turno turno) {
        List<Turno> turnos = turnosDe(empleado);
        if (!turnos.removeIf(t -> t.mismoHorario(turno)))
            throw new IllegalArgumentException(empleado.getNombre() + " no tiene el turno " + turno + " en la programación.");
    }

    private List<Turno> turnosDe(Empleado empleado) {
        List<Turno> turnos = borrador.get(empleado);
        if (turnos == null) throw new IllegalArgumentException("El empleado no hace parte de esta programación.");
        return turnos;
    }

    public Map<Empleado, List<Turno>> getBorrador() { return borrador; }

    /**
     * Tramos del horario de atención que no tienen la cobertura mínima, con el formato "lunes 05:00-07:00".
     * Los tramos contiguos sin cobertura se unen en uno solo.
     */
    public List<String> bloquesSinCobertura() {
        List<String> bloques = new ArrayList<>();
        for (DayOfWeek dia : Turno.DIAS_DE_ATENCION) {
            TreeSet<LocalTime> cortes = new TreeSet<>();
            cortes.add(Turno.APERTURA);
            cortes.add(Turno.CIERRE);
            for (List<Turno> turnos : borrador.values())
                for (Turno t : turnos) if (t.getDia() == dia) {
                    if (t.getHoraInicio().isAfter(Turno.APERTURA) && t.getHoraInicio().isBefore(Turno.CIERRE)) cortes.add(t.getHoraInicio());
                    if (t.getHoraFin().isAfter(Turno.APERTURA) && t.getHoraFin().isBefore(Turno.CIERRE)) cortes.add(t.getHoraFin());
                }
            List<LocalTime> puntos = new ArrayList<>(cortes);
            LocalTime inicioFalla = null;
            for (int i = 0; i < puntos.size() - 1; i++) {
                LocalTime desde = puntos.get(i);
                LocalTime hasta = puntos.get(i + 1);
                boolean cubierto = cubierto(dia, desde.plus(Duration.between(desde, hasta).dividedBy(2)));
                if (!cubierto && inicioFalla == null) inicioFalla = desde;
                if (cubierto && inicioFalla != null) { bloques.add(Turno.nombreDia(dia) + " " + inicioFalla + "-" + desde); inicioFalla = null; }
            }
            if (inicioFalla != null) bloques.add(Turno.nombreDia(dia) + " " + inicioFalla + "-" + Turno.CIERRE);
        }
        return bloques;
    }

    private boolean cubierto(DayOfWeek dia, LocalTime instante) {
        int entrenadores = 0;
        int fisioterapeutas = 0;
        for (Map.Entry<Empleado, List<Turno>> e : borrador.entrySet()) {
            boolean activo = false;
            for (Turno t : e.getValue()) if (t.cubre(dia, instante)) activo = true;
            if (!activo) continue;
            if (e.getKey() instanceof Entrenador) entrenadores++;
            else if (e.getKey() instanceof Fisioterapeuta) fisioterapeutas++;
        }
        return entrenadores >= MIN_ENTRENADORES && fisioterapeutas >= MIN_FISIOTERAPEUTAS;
    }

    /** Aplica el borrador a los empleados. Rechaza la programación completa si queda algún bloque sin cobertura. */
    public void publicar() {
        List<String> faltantes = bloquesSinCobertura();
        if (!faltantes.isEmpty())
            throw new IllegalStateException("La programación deja bloques sin la cobertura mínima de " + MIN_ENTRENADORES
                    + " entrenadores y " + MIN_FISIOTERAPEUTAS + " fisioterapeuta: " + String.join(", ", faltantes) + ".");
        for (Map.Entry<Empleado, List<Turno>> e : borrador.entrySet()) e.getKey().reemplazarTurnos(e.getValue());
    }
}
