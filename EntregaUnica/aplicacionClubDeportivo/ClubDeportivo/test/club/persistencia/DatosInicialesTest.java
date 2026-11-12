package club.persistencia;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.Before;
import org.junit.Test;

import club.model.SistemaClub;
import club.modelo.competencias.Competencia;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.Tenis;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.ProgramacionSemanal;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.SolicitudCambioTurno;
import club.modelo.usuarios.Turno;

/** Los datos de demostración deben respetar las mismas reglas que el resto del sistema. */
public class DatosInicialesTest {
    private SistemaClub sistema;

    @Before
    public void preparar() {
        sistema = new SistemaClub();
        DatosIniciales.completar(sistema);
    }

    @Test
    public void lasCredencialesDeDemostracionFuncionanConSuRol() {
        assertTrue(sistema.autenticar("admin", "admin123") instanceof Administrador);
        assertTrue(sistema.autenticar("entrenador.demo", "entre123") instanceof Entrenador);
        assertNotNull(sistema.autenticar("fisio.demo", "fisio123"));
        assertTrue(sistema.autenticar("socio.demo", "demo123") instanceof Socio);
        assertTrue(sistema.autenticar("jugador14", "demo123") instanceof Socio);
    }

    @Test
    public void laProgramacionDeTurnosCumpleLaCoberturaMinima() {
        assertEquals(7, sistema.getEmpleados().size());
        assertTrue(new ProgramacionSemanal(sistema.getEmpleados()).bloquesSinCobertura().isEmpty());
    }

    @Test
    public void unCambioDeTurnoDelLunesEnLaMananaSePuedeAprobar() {
        Entrenador carlos = (Entrenador) sistema.buscarUsuario("entrenador.demo");
        Turno lunes = null;
        for (Turno t : carlos.getTurnos()) if (t.getDia() == DayOfWeek.MONDAY) lunes = t;
        SolicitudCambioTurno s = carlos.solicitarCambioTurno(lunes, new Turno(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(23, 0)));
        ((Administrador) sistema.buscarUsuario("admin")).resolverCambioTurno(s, sistema.getEmpleados(), true, null);
        assertEquals("Aprobada", s.getEstado());
    }

    @Test
    public void losEquiposPuedenJugarYLaCompetenciaTienePartidosPropuestosYProgramados() {
        assertEquals(2, sistema.getEquiposActivos().size());
        for (Equipo e : sistema.getEquiposActivos())
            assertTrue(e.getInscripcionesActivas().size() >= e.getDeporte().getMinJugadores());

        Competencia liga = sistema.getCompetencias().get(0);
        assertEquals(3, liga.getPartidos().size());
        int programados = 0;
        for (PartidoCompetencia p : liga.getPartidos()) if (p.isProgramado()) programados++;
        assertEquals(1, programados);
        PartidoCompetencia programado = liga.getPartidos().get(0);
        assertSame(sistema.buscarUsuario("entrenador.demo"), programado.getEntrenadorResponsable());
        ((Entrenador) sistema.buscarUsuario("entrenador.demo")).registrarResultadoOficial(programado, 2, 0);
        assertTrue(programado.isFinalizado());
    }

    @Test
    public void losSociosDeDemostracionEstanEnTenisConNivelesCompatibles() {
        Tenis tenis = null;
        for (DisciplinaIndividual d : sistema.getDisciplinas()) if (d instanceof Tenis) tenis = (Tenis) d;
        Socio ana = (Socio) sistema.buscarUsuario("socio.demo");
        Socio juan = (Socio) sistema.buscarUsuario("socio.demo2");
        assertTrue(tenis.tieneInscripcionActiva(ana));
        assertTrue(tenis.tieneInscripcionActiva(juan));
        club.model.Reloj.fijar(java.time.LocalDateTime.of(2030, 1, 1, 8, 0));
        try {
            club.modelo.instalaciones.Reserva r = ana.reservarInstalacion(tenis.getInstalaciones().get(0),
                    java.time.LocalDateTime.of(2030, 1, 2, 9, 0), java.time.LocalDateTime.of(2030, 1, 2, 10, 0), 2, "sencillos");
            assertNotNull(ana.programarPartidoPractica(r, java.util.Arrays.asList(juan), null));
        } finally {
            club.model.Reloj.usarRelojDelSistema();
        }
    }
}
