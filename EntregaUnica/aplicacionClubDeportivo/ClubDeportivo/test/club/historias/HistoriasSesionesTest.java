package club.historias;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.model.Reloj;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.Futbol;
import club.modelo.entrenamiento.Asistencia;
import club.modelo.entrenamiento.Padel;
import club.modelo.entrenamiento.SesionEntrenamiento;
import club.modelo.entrenamiento.Tenis;
import club.modelo.instalaciones.CanchaConjunto;
import club.modelo.instalaciones.Instalacion;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;

/** HU-T01 (planear sesión), HU-T02 (asistencia) y HU-T03 (evaluación). */
public class HistoriasSesionesTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 4, 1, 7, 0);
    private static final LocalDateTime SESION = AHORA.plusDays(1);

    private Entrenador entrenador, otro;
    private Equipo equipo;
    private CanchaConjunto cancha;
    private Tenis tenis;
    private Socio ana, juan, luis;

    @Before
    public void preparar() {
        Reloj.fijar(AHORA);
        entrenador = new Entrenador("ent", "x", "Entrenador", "E1");
        otro = new Entrenador("otro", "x", "Otro", "E2");
        equipo = Fabrica.equipoConJugadores("Séneca", 8, new Futbol("Fútbol", 7), new Categoria("Mayores", 18, 150));
        equipo.retirarJugador(equipo.getInscripcionesActivas().get(7).getSocio());   // un retirado no se convoca
        cancha = new CanchaConjunto(10, 22, "Cancha de fútbol", false);
        tenis = Fabrica.tenis(2);
        ana = Fabrica.socio("ana");
        juan = Fabrica.socio("juan");
        luis = Fabrica.socio("luis");
        ana.inscribirseEnDisciplina(tenis, "Intermedio");
        juan.inscribirseEnDisciplina(tenis, "Intermedio");
    }

    @After
    public void restablecerReloj() { Reloj.usarRelojDelSistema(); }

    private Instalacion canchaTenis() { return tenis.getInstalaciones().get(0); }

    // ===================================================================== HU-T01 · Planear una sesión
    @Test
    public void t01_exitoConjunto_unaConvocatoriaPorCadaJugadorActivo() {
        SesionEntrenamiento s = entrenador.planearSesionEquipo(SESION, 90, cancha, equipo);
        assertEquals(7, s.getConvocados().size());
        assertTrue(entrenador.getSesiones().contains(s));
        assertTrue(s.getConvocados().get(0).getSesionesConvocado().contains(s));
    }

    @Test
    public void t01_exitoAcademia_subconjuntoDeInscritosOUnoSolo() {
        assertEquals(2, entrenador.planearSesionAcademia(SESION, 60, canchaTenis(), tenis, Arrays.asList(ana, juan)).getConvocados().size());
        assertEquals(1, entrenador.planearSesionAcademia(SESION.plusHours(2), 60, canchaTenis(), tenis, Arrays.asList(ana)).getConvocados().size());
    }

    @Test
    public void t01_rechazoPorOcupacion_instalacionConReservaEnEseHorario() {
        ana.reservarInstalacion(canchaTenis(), SESION, SESION.plusHours(1), 2, "sencillos");
        assertThrows(IllegalStateException.class, () -> entrenador.planearSesionAcademia(SESION.plusMinutes(30), 60, canchaTenis(), tenis, Arrays.asList(juan)));
        assertTrue(entrenador.getSesiones().isEmpty());
    }

    @Test
    public void t01_rechazoPorCompatibilidad_instalacionDeOtraDisciplina() {
        Padel padel = Fabrica.padel();
        Instalacion canchaPadel = padel.getInstalaciones().get(0);
        assertThrows(IllegalArgumentException.class, () -> entrenador.planearSesionEquipo(SESION, 90, canchaPadel, equipo));
        assertThrows(IllegalArgumentException.class, () -> entrenador.planearSesionAcademia(SESION, 60, canchaPadel, tenis, Arrays.asList(ana)));
        assertThrows(IllegalArgumentException.class, () -> entrenador.planearSesionAcademia(SESION, 60, cancha, tenis, Arrays.asList(ana)));
        assertTrue(entrenador.getSesiones().isEmpty());
    }

    @Test
    public void t01_rechazoPorConvocados_noPerteneceALaDisciplina() {
        assertThrows(IllegalArgumentException.class, () -> entrenador.planearSesionAcademia(SESION, 60, canchaTenis(), tenis, Arrays.asList(ana, luis)));
        assertTrue(luis.getSesionesConvocado().isEmpty());
        assertTrue(ana.getSesionesConvocado().isEmpty());
    }

    @Test
    public void t01_rechazoPorDatos_duracionFechaPasadaOSesionPropiaTraslapada() {
        assertThrows(IllegalArgumentException.class, () -> entrenador.planearSesionAcademia(SESION, 0, canchaTenis(), tenis, Arrays.asList(ana)));
        assertThrows(IllegalArgumentException.class, () -> entrenador.planearSesionAcademia(AHORA.minusHours(1), 60, canchaTenis(), tenis, Arrays.asList(ana)));
        entrenador.planearSesionEquipo(SESION, 90, cancha, equipo);
        assertThrows(IllegalStateException.class, () -> entrenador.planearSesionAcademia(SESION.plusMinutes(30), 60, canchaTenis(), tenis, Arrays.asList(ana)));
        assertEquals(1, entrenador.getSesiones().size());
    }

    // ===================================================================== HU-T02 · Registrar asistencia
    private SesionEntrenamiento sesionTerminada() {
        SesionEntrenamiento s = entrenador.planearSesionAcademia(SESION, 60, canchaTenis(), tenis, Arrays.asList(ana, juan));
        Reloj.fijar(SESION.plusHours(2));
        return s;
    }

    @Test
    public void t02_exito_sesionFinalizadaYJugadorConvocado() {
        SesionEntrenamiento s = sesionTerminada();
        entrenador.registrarAsistencia(s, ana, "asistió", null, null);
        entrenador.registrarAsistencia(s, juan, "llegó tarde", null, null);
        assertEquals(2, s.getAsistencias().size());
        assertEquals("asistió", ana.getAsistencias().get(0).getEstado());
    }

    @Test
    public void t02_rechazo_jugadorNoConvocado() {
        SesionEntrenamiento s = sesionTerminada();
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarAsistencia(s, luis, "asistió", null, null));
        assertTrue(luis.getAsistencias().isEmpty());
    }

    @Test
    public void t02_rechazo_sesionNoFinalizadaEstadoInvalidoOSesionDeOtroEntrenador() {
        SesionEntrenamiento s = entrenador.planearSesionAcademia(SESION, 60, canchaTenis(), tenis, Arrays.asList(ana, juan));
        assertThrows(IllegalStateException.class, () -> entrenador.registrarAsistencia(s, ana, "asistió", null, null));
        Reloj.fijar(SESION.plusHours(2));
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarAsistencia(s, ana, "presente", null, null));
        assertThrows(IllegalArgumentException.class, () -> otro.registrarAsistencia(s, ana, "asistió", null, null));
        assertTrue(s.getAsistencias().isEmpty());
        assertTrue(ana.getAsistencias().isEmpty());
    }

    // ===================================================================== HU-T03 · Evaluación de desempeño
    @Test
    public void t03_exito_notaYComentarioParaQuienAsistioOLlegoTarde() {
        SesionEntrenamiento s = sesionTerminada();
        entrenador.registrarAsistencia(s, ana, "asistió", null, null);
        entrenador.registrarAsistencia(s, juan, "llegó tarde", null, null);
        Asistencia a = entrenador.evaluar(s, ana, 4, "Buen revés");
        entrenador.evaluar(s, juan, 3, "Mejorar la puntualidad");
        assertEquals(Integer.valueOf(4), a.getNotaDesempeno());
        assertEquals("Buen revés", a.getComentario());
    }

    @Test
    public void t03_opcional_laAsistenciaEsValidaSinEvaluacion() {
        SesionEntrenamiento s = sesionTerminada();
        Asistencia a = entrenador.registrarAsistencia(s, ana, "asistió", null, null);
        assertNull(a.getNotaDesempeno());
    }

    @Test
    public void t03_rechazo_notaFueraDeRango() {
        SesionEntrenamiento s = sesionTerminada();
        entrenador.registrarAsistencia(s, ana, "asistió", null, null);
        assertThrows(IllegalArgumentException.class, () -> entrenador.evaluar(s, ana, 0, "x"));
        assertThrows(IllegalArgumentException.class, () -> entrenador.evaluar(s, ana, 6, "x"));
        assertNull(ana.getAsistencias().get(0).getNotaDesempeno());
    }

    @Test
    public void t03_rechazo_jugadorQueFaltoOSinAsistenciaRegistrada() {
        SesionEntrenamiento s = sesionTerminada();
        entrenador.registrarAsistencia(s, ana, "faltó", null, null);
        assertThrows(IllegalStateException.class, () -> entrenador.evaluar(s, ana, 3, "x"));
        assertThrows(IllegalStateException.class, () -> entrenador.evaluar(s, juan, 3, "x"));
        assertThrows(IllegalStateException.class, () -> entrenador.registrarAsistencia(s, juan, "faltó", 4, "x"));
        assertTrue(juan.getAsistencias().isEmpty());
    }

    // ===================================================================== HU-S08 · Consultar mis convocatorias
    @Test
    public void s08_exito_veFechaHoraDuracionInstalacionYEntrenadorEnOrden() {
        SesionEntrenamiento tarde = entrenador.planearSesionAcademia(SESION.plusDays(2), 60, canchaTenis(), tenis, Arrays.asList(ana));
        SesionEntrenamiento temprano = entrenador.planearSesionAcademia(SESION, 45, canchaTenis(), tenis, Arrays.asList(ana, juan));
        assertEquals(Arrays.asList(temprano, tarde), ana.consultarConvocatorias());
        SesionEntrenamiento s = ana.consultarConvocatorias().get(0);
        assertEquals(SESION, s.getFechaHora());
        assertEquals(45, s.getDuracionMinutos());
        assertSame(canchaTenis(), s.getInstalacion());
        assertSame(entrenador, s.getEntrenador());
    }

    @Test
    public void s08_vacio_socioSinConvocatorias() {
        assertTrue(luis.consultarConvocatorias().isEmpty());
    }
}
