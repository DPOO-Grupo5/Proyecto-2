package club.historias;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.Before;
import org.junit.Test;

import club.model.SistemaClub;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Fisioterapeuta;
import club.modelo.usuarios.SolicitudCambioTurno;
import club.modelo.usuarios.Turno;

/** Pruebas de integración derivadas de las historias del empleado (banco oficial). */
public class HistoriasEmpleadoTest {
    private SistemaClub sistema;
    private Entrenador entrenador;
    private Fisioterapeuta fisio;

    @Before
    public void preparar() {
        sistema = new SistemaClub();
        entrenador = new Entrenador("entrenador1", "clave123", "Entrenador Uno", "ENT1");
        fisio = new Fisioterapeuta("fisio1", "clave123", "Fisio Uno", "FIS1");
        sistema.registrarUsuario(entrenador);
        sistema.registrarUsuario(fisio);
    }

    // ===================================================================== HU-E01 · Iniciar sesión como empleado
    @Test
    public void e01_exito_cadaEmpleadoAccedeSegunSuCargo() {
        assertTrue(sistema.autenticar("entrenador1", "clave123") instanceof Entrenador);
        assertTrue(sistema.autenticar("fisio1", "clave123") instanceof Fisioterapeuta);
    }

    @Test
    public void e01_rechazo_credencialesIncorrectas() {
        assertNull(sistema.autenticar("entrenador1", "otra"));
        assertNull(sistema.autenticar("fisio9", "clave123"));
    }

    // ===================================================================== HU-E02 · Consultar mis turnos semanales
    @Test
    public void e02_exito_veDiaYFranjaDeCadaTurno() {
        Empleado empleado = entrenador;
        empleado.agregarTurno(new Turno(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        empleado.agregarTurno(new Turno(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

        assertEquals(2, empleado.consultarTurnos().size());
        Turno primero = empleado.consultarTurnos().get(0);
        assertEquals(DayOfWeek.MONDAY, primero.getDia());
        assertEquals(LocalTime.of(8, 0), primero.getHoraInicio());
        assertEquals(LocalTime.of(12, 0), primero.getHoraFin());
    }

    @Test
    public void e02_vacio_empleadoSinTurnos() {
        assertTrue(fisio.consultarTurnos().isEmpty());
    }

    // ===================================================================== HU-E03 · Solicitar un cambio de turno
    private static Turno turno(DayOfWeek dia, int desde, int hasta) {
        return new Turno(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    @Test
    public void e03_exito_quedaPendienteYElTurnoOriginalSigueVigente() {
        Turno actual = turno(DayOfWeek.MONDAY, 8, 12);
        entrenador.agregarTurno(actual);

        SolicitudCambioTurno s = entrenador.solicitarCambioTurno(actual, turno(DayOfWeek.TUESDAY, 14, 18));

        assertEquals("Pendiente", s.getEstado());
        assertTrue(entrenador.getSolicitudes().contains(s));
        assertTrue(entrenador.getTurnos().contains(actual));
    }

    @Test
    public void e03_rechazo_turnoQueNoLePertenece() {
        Turno ajeno = turno(DayOfWeek.MONDAY, 8, 12);
        fisio.agregarTurno(ajeno);
        assertThrows(IllegalArgumentException.class, () -> entrenador.solicitarCambioTurno(ajeno, turno(DayOfWeek.TUESDAY, 8, 12)));
        assertTrue(entrenador.getSolicitudes().isEmpty());
    }

    @Test
    public void e03_rechazo_horarioFueraDeAtencionOIgualAlActual() {
        Turno actual = turno(DayOfWeek.MONDAY, 8, 12);
        entrenador.agregarTurno(actual);
        // El club atiende de 05:00 a 23:00.
        assertThrows(IllegalArgumentException.class, () -> entrenador.solicitarCambioTurno(actual, turno(DayOfWeek.MONDAY, 4, 8)));
        assertThrows(IllegalArgumentException.class, () -> entrenador.solicitarCambioTurno(actual,
                new Turno(DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(23, 30))));
        assertThrows(IllegalArgumentException.class, () -> entrenador.solicitarCambioTurno(actual, turno(DayOfWeek.MONDAY, 8, 12)));
        assertTrue(entrenador.getSolicitudes().isEmpty());
        // El club no abre los domingos.
        assertThrows(IllegalArgumentException.class, () -> entrenador.solicitarCambioTurno(actual, turno(DayOfWeek.SUNDAY, 8, 12)));
        assertTrue(entrenador.getSolicitudes().isEmpty());
        // Los límites exactos del horario sí se aceptan.
        entrenador.solicitarCambioTurno(actual, new Turno(DayOfWeek.MONDAY, LocalTime.of(5, 0), LocalTime.of(9, 0)));
    }

    @Test
    public void e03_rechazo_turnoConUnaSolicitudPendiente() {
        Turno actual = turno(DayOfWeek.MONDAY, 8, 12);
        entrenador.agregarTurno(actual);
        SolicitudCambioTurno primera = entrenador.solicitarCambioTurno(actual, turno(DayOfWeek.TUESDAY, 8, 12));
        assertThrows(IllegalStateException.class, () -> entrenador.solicitarCambioTurno(actual, turno(DayOfWeek.FRIDAY, 8, 12)));
        assertEquals(1, entrenador.getSolicitudes().size());
        // Una vez resuelta la primera, se puede solicitar otro cambio.
        primera.rechazar("Sin reemplazo disponible.");
        entrenador.solicitarCambioTurno(actual, turno(DayOfWeek.FRIDAY, 8, 12));
        assertEquals(2, entrenador.getSolicitudes().size());
    }

    // ===================================================================== HU-E04 · Estado de mis solicitudes
    @Test
    public void e04_exito_veCadaSolicitudConSuEstadoYElMotivoDelRechazo() {
        Turno lunes = turno(DayOfWeek.MONDAY, 8, 12);
        Turno martes = turno(DayOfWeek.TUESDAY, 8, 12);
        entrenador.agregarTurno(lunes);
        entrenador.agregarTurno(martes);
        SolicitudCambioTurno pendiente = entrenador.solicitarCambioTurno(lunes, turno(DayOfWeek.FRIDAY, 8, 12));
        SolicitudCambioTurno rechazada = entrenador.solicitarCambioTurno(martes, turno(DayOfWeek.FRIDAY, 14, 18));
        rechazada.rechazar("No hay quién cubra el martes en la mañana.");

        assertEquals(2, entrenador.getSolicitudes().size());
        assertEquals("Pendiente", pendiente.getEstado());
        assertNull(pendiente.getMotivoRechazo());
        assertEquals("Rechazada", rechazada.getEstado());
        assertEquals("No hay quién cubra el martes en la mañana.", rechazada.getMotivoRechazo());
    }

    @Test
    public void e04_vacio_empleadoSinSolicitudes() {
        assertTrue(fisio.getSolicitudes().isEmpty());
    }
}
