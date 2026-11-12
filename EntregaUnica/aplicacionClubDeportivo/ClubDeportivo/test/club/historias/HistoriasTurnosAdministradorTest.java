package club.historias;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Fisioterapeuta;
import club.modelo.usuarios.ProgramacionSemanal;
import club.modelo.usuarios.SolicitudCambioTurno;
import club.modelo.usuarios.Turno;

/**
 * HU-A11 (programar turnos semanales) y HU-A12 (resolver solicitudes de cambio).
 * Horario de atención (decisión del grupo): lunes a sábado de 05:00 a 23:00.
 * Cobertura mínima: 2 entrenadores y 1 fisioterapeuta en todo instante del horario de atención.
 */
public class HistoriasTurnosAdministradorTest {
    private static final List<DayOfWeek> DIAS_HABILES = Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    private Administrador admin;
    private Entrenador e1, e2, e3, e4;
    private Fisioterapeuta f1, f2;
    private List<Empleado> todos;

    @Before
    public void preparar() {
        admin = new Administrador("admin", "x", "Admin");
        e1 = new Entrenador("e1", "x", "E1", "C1");
        e2 = new Entrenador("e2", "x", "E2", "C2");
        e3 = new Entrenador("e3", "x", "E3", "C3");
        e4 = new Entrenador("e4", "x", "E4", "C4");
        f1 = new Fisioterapeuta("f1", "x", "F1", "C5");
        f2 = new Fisioterapeuta("f2", "x", "F2", "C6");
        todos = new ArrayList<>(Arrays.asList(e1, e2, e3, e4, f1, f2));
    }

    private static Turno turno(DayOfWeek dia, int desde, int hasta) {
        return new Turno(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    /** Dos entrenadores y un fisioterapeuta en la mañana (05–14) y lo mismo en la tarde (14–23), de lunes a sábado. */
    private ProgramacionSemanal programacionCompleta() {
        ProgramacionSemanal p = new ProgramacionSemanal(todos);
        for (DayOfWeek d : DIAS_HABILES) {
            for (Empleado manana : Arrays.<Empleado>asList(e1, e2, f1)) p.asignar(manana, turno(d, 5, 14));
            for (Empleado tarde : Arrays.<Empleado>asList(e3, e4, f2)) p.asignar(tarde, turno(d, 14, 23));
        }
        return p;
    }

    // ===================================================================== HU-A11 · Programar los turnos semanales
    @Test
    public void a11_exito_programacionConCoberturaMinimaQuedaRegistrada() {
        ProgramacionSemanal p = programacionCompleta();
        assertTrue(p.bloquesSinCobertura().isEmpty());

        admin.publicarProgramacion(p);

        assertEquals(6, e1.getTurnos().size());
        assertEquals(6, f2.getTurnos().size());
        assertEquals(DayOfWeek.MONDAY, e1.getTurnos().get(0).getDia());
    }

    @Test
    public void a11_rechazo_bloqueSinCoberturaIndicaElBloqueYNoCambiaNada() {
        ProgramacionSemanal p = programacionCompleta();
        p.quitar(f2, turno(DayOfWeek.WEDNESDAY, 14, 23));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> admin.publicarProgramacion(p));

        assertTrue(e.getMessage(), e.getMessage().contains("miércoles 14:00-23:00"));
        assertTrue("Nadie recibe turnos si la programación se rechaza", e1.getTurnos().isEmpty());
    }

    @Test
    public void a11_rechazo_unSoloEntrenadorEnUnTramoTambienEsFaltaDeCobertura() {
        ProgramacionSemanal p = programacionCompleta();
        p.quitar(e2, turno(DayOfWeek.SATURDAY, 5, 14));
        p.asignar(e2, turno(DayOfWeek.SATURDAY, 7, 14));     // de 05:00 a 07:00 solo queda un entrenador
        assertEquals(Arrays.asList("sábado 05:00-07:00"), p.bloquesSinCobertura());
        assertThrows(IllegalStateException.class, () -> admin.publicarProgramacion(p));
    }

    @Test
    public void a11_rechazo_empleadoConOtroTurnoEnElMismoBloque() {
        ProgramacionSemanal p = new ProgramacionSemanal(todos);
        p.asignar(e1, turno(DayOfWeek.MONDAY, 5, 14));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> p.asignar(e1, turno(DayOfWeek.MONDAY, 10, 18)));
        assertTrue(e.getMessage().contains("traslap"));
    }

    @Test
    public void a11_rechazo_turnoFueraDelHorarioDeAtencionODomingo() {
        ProgramacionSemanal p = new ProgramacionSemanal(todos);
        assertThrows(IllegalArgumentException.class, () -> p.asignar(e1, turno(DayOfWeek.SUNDAY, 8, 12)));
        assertThrows(IllegalArgumentException.class, () -> p.asignar(e1, turno(DayOfWeek.MONDAY, 4, 9)));
    }

    // ===================================================================== HU-A12 · Resolver solicitudes de cambio
    private void publicarCompleta() {
        admin.publicarProgramacion(programacionCompleta());
    }

    private static Turno turnoDe(Empleado e, DayOfWeek dia) {
        for (Turno t : e.getTurnos()) if (t.getDia() == dia) return t;
        throw new AssertionError("Sin turno el " + dia);
    }

    @Test
    public void a12_aprobacion_cambioQueMantieneLaCoberturaActualizaLosTurnos() {
        publicarCompleta();
        // Un quinto entrenador refuerza la mañana del lunes, así que e1 puede pasarse a la tarde sin romper la cobertura.
        Entrenador extra = new Entrenador("e5", "x", "E5", "C7");
        todos.add(extra);
        extra.agregarTurno(turno(DayOfWeek.MONDAY, 5, 14));
        Turno actual = turnoDe(e1, DayOfWeek.MONDAY);
        SolicitudCambioTurno s = e1.solicitarCambioTurno(actual, turno(DayOfWeek.MONDAY, 14, 23));

        admin.resolverCambioTurno(s, todos, true, null);

        assertEquals("Aprobada", s.getEstado());
        assertEquals(LocalTime.of(14, 0), turnoDe(e1, DayOfWeek.MONDAY).getHoraInicio());
    }

    @Test
    public void a12_rechazoPorCobertura_quedaRechazadaConMotivoYLosTurnosNoCambian() {
        publicarCompleta();
        Turno actual = turnoDe(f1, DayOfWeek.TUESDAY);
        SolicitudCambioTurno s = f1.solicitarCambioTurno(actual, turno(DayOfWeek.TUESDAY, 14, 23));

        admin.resolverCambioTurno(s, todos, true, null);

        assertEquals("Rechazada", s.getEstado());
        assertTrue(s.getMotivoRechazo(), s.getMotivoRechazo().contains("cobertura"));
        assertSame(actual, turnoDe(f1, DayOfWeek.TUESDAY));
    }

    @Test
    public void a12_rechazoManual_exigeMotivoYElEmpleadoLoPuedeVer() {
        publicarCompleta();
        SolicitudCambioTurno s = e1.solicitarCambioTurno(turnoDe(e1, DayOfWeek.MONDAY), turno(DayOfWeek.MONDAY, 14, 23));
        assertThrows(IllegalArgumentException.class, () -> admin.resolverCambioTurno(s, todos, false, "  "));
        assertEquals("Pendiente", s.getEstado());
        admin.resolverCambioTurno(s, todos, false, "Temporada de torneos.");
        assertEquals("Temporada de torneos.", e1.getSolicitudes().get(0).getMotivoRechazo());
    }

    @Test
    public void a12_rechazoDeLaAccion_solicitudYaResuelta() {
        publicarCompleta();
        SolicitudCambioTurno s = e1.solicitarCambioTurno(turnoDe(e1, DayOfWeek.MONDAY), turno(DayOfWeek.MONDAY, 14, 23));
        admin.resolverCambioTurno(s, todos, false, "No procede.");
        assertThrows(IllegalStateException.class, () -> admin.resolverCambioTurno(s, todos, true, null));
        assertEquals("Rechazada", s.getEstado());
    }
}
