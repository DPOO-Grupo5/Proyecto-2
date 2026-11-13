package club.historias;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.model.Reloj;
import club.model.SistemaClub;
import club.modelo.competencias.*;
import club.modelo.entrenamiento.InscripcionDisciplina;
import club.modelo.entrenamiento.SesionEntrenamiento;
import club.modelo.entrenamiento.Tenis;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;

/** Pruebas de A13, A14 (mensualidad), A15, A18, S09, S11 y S12 (consultas y reportes del banco). */
public class HistoriasConsultasBancoTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 3, 1, 9, 0);
    private SistemaClub sistema;
    private Administrador admin;
    private Entrenador responsable;

    @Before
    public void preparar() {
        Reloj.fijar(AHORA);
        sistema = new SistemaClub();
        admin = new Administrador("admin", "admin123", "Administrador");
        sistema.registrarUsuario(admin);
        responsable = new Entrenador("resp", "x", "Responsable", "R1");
    }

    @After
    public void restablecerReloj() { Reloj.usarRelojDelSistema(); }

    private Competencia liga(Equipo base) {
        return admin.crearCompetencia("Copa", "ASCUN", "2030", base.getDeporte(), base.getCategoria(),
                new FormatoLiga("Liga", new ReglaFutbol(3, 0)), sistema);
    }

    private List<Socio> jugadores(Equipo e) {
        List<Socio> l = new ArrayList<>();
        for (InscripcionEquipo i : e.getInscripcionesActivas()) l.add(i.getSocio());
        return l;
    }

    // ---------------------------------------------------------------- HU-A18
    @Test
    public void a18_exito_equipoYNominaSeInscribenJuntos() {
        Equipo e = Fabrica.equipoConJugadores("Séneca", 8);
        Competencia c = liga(e);
        c.inscribirEquipoConNomina(e, jugadores(e));
        assertTrue(c.getEquipos().contains(e));
        assertEquals(8, c.getInscripciones().get(0).getNomina().size());
    }

    @Test
    public void a18_rechazo_nominaConSuspendidosBajoElMinimoIndicaCuantosFaltan() {
        Equipo e = Fabrica.equipoConJugadores("Séneca", 9);
        Competencia c = liga(e);
        List<Socio> nomina = jugadores(e).subList(0, 7);          // 7 en la nómina
        nomina.get(0).registrarSancion(new Sancion("Tarjeta roja", 1, false, nomina.get(0)));  // uno suspendido → 6 habilitados
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> c.inscribirEquipoConNomina(e, new ArrayList<>(nomina)));
        assertTrue(ex.getMessage(), ex.getMessage().contains("faltan 1"));
        assertFalse(c.getEquipos().contains(e));                   // el rechazo no deja cambios
        assertTrue(c.getInscripciones().isEmpty());
    }

    @Test
    public void a18_rechazo_nominaAjenaCategoriaDuplicado() {
        Equipo e = Fabrica.equipoConJugadores("Séneca", 8);
        Equipo otro = Fabrica.equipoConJugadores("Otro", 8);
        Competencia c = liga(e);
        List<Socio> ajena = jugadores(e);
        ajena.set(0, jugadores(otro).get(0));
        assertThrows(IllegalArgumentException.class, () -> c.inscribirEquipoConNomina(e, ajena));
        assertTrue(c.getInscripciones().isEmpty());
        c.inscribirEquipoConNomina(e, jugadores(e));
        assertThrows(IllegalArgumentException.class, () -> c.inscribirEquipoConNomina(e, jugadores(e)));
        Equipo sub13 = Fabrica.equipoConJugadores("Peques", 8, e.getDeporte(), new Categoria("Otra", 0, 150));
        assertThrows(IllegalArgumentException.class, () -> c.inscribirEquipoConNomina(sub13, jugadores(sub13)));
    }

    // ---------------------------------------------------------------- HU-S11 y S12
    /** Liga de 4 instituciones: A le gana a B pero B tiene mejor diferencia de goles; ambos terminan con 3 puntos. */
    @Test
    public void s11_empateEnPuntosSeResuelvePorEnfrentamientoDirecto() {
        Equipo base = Fabrica.equipoConJugadores("Base", 8);
        Competencia c = liga(base);
        InstitucionRival a = new InstitucionRival("A"), b = new InstitucionRival("B"), x = new InstitucionRival("C"), d = new InstitucionRival("D");
        for (InstitucionRival r : Arrays.asList(a, b, x, d)) c.agregarEquipo(r);
        c.generarCalendario();
        resultado(c, a, b, 1, 0);
        resultado(c, b, x, 5, 0);
        resultado(c, x, a, 1, 0);
        resultado(c, d, b, 1, 0);
        resultado(c, d, a, 1, 0);
        resultado(c, x, d, 0, 0);
        FormatoLiga f = (FormatoLiga) c.getFormato();
        f.actualizarPosiciones();
        PosicionEquipo pa = null, pb = null;
        for (PosicionEquipo p : f.getPosiciones()) { if (p.getEquipo() == a) pa = p; if (p.getEquipo() == b) pb = p; }
        assertEquals(pa.getPuntuacion(), pb.getPuntuacion());
        assertTrue("B tiene mejor diferencia de goles", pb.getTantosFavor() - pb.getTantosContra() > pa.getTantosFavor() - pa.getTantosContra());
        assertTrue("A le ganó a B: debe quedar arriba", pa.getPosicion() < pb.getPosicion());
    }

    private void resultado(Competencia c, EquipoCompetencia golesX, EquipoCompetencia golesY, int gx, int gy) {
        for (PartidoCompetencia p : c.getPartidos()) {
            if (!p.getEquipos().contains(golesX) || !p.getEquipos().contains(golesY)) continue;
            p.programar(LocalDate.of(2030, 4, 1), "Campín", responsable);
            boolean xLocal = p.getEquipos().get(0) == golesX;
            p.registrarMarcador(xLocal ? gx : gy, xLocal ? gy : gx);
            return;
        }
        throw new AssertionError("No hay partido entre los equipos");
    }

    private Competencia ligaConEstadisticas(Socio[] destacados) {
        Equipo seneca = Fabrica.equipoConJugadores("Séneca", 8);
        sistema.registrarEquipo(seneca);
        Competencia c = liga(seneca);
        c.agregarEquipo(seneca);
        c.registrarNomina(seneca, jugadores(seneca));
        c.agregarEquipo(new InstitucionRival("Javeriana"));
        c.generarCalendario();
        PartidoCompetencia p = c.getPartidos().get(0);
        p.programar(LocalDate.of(2030, 4, 1), "Campín", responsable);
        p.definirAlineacion(seneca, jugadores(seneca).subList(0, 7));
        responsable.registrarResultadoOficial(p, 2, 1);
        destacados[0] = jugadores(seneca).get(0);
        destacados[1] = jugadores(seneca).get(1);
        p.agregarEstadistica(new EstadisticaJugadorFutbol(destacados[0], 5, 1, 0, 0));   // más goles
        p.agregarEstadistica(new EstadisticaJugadorFutbol(destacados[1], 1, 3, 0, 0));   // más asistencias
        return c;
    }

    @Test
    public void s12_goleadoresPorGolesYDestacadosPorAsistenciasEnFutbol() {
        Socio[] j = new Socio[2];
        Competencia c = ligaConEstadisticas(j);
        assertTrue(c.tieneResultados());
        List<Map.Entry<Socio, Integer>> goleadores = c.clasificacionGoleadores();
        assertSame(j[0], goleadores.get(0).getKey());
        assertEquals(5, (int) goleadores.get(0).getValue());
        List<Map.Entry<Socio, Integer>> destacados = c.clasificacionDestacados();
        assertSame(j[1], destacados.get(0).getKey());               // el criterio es asistencias, no goles + asistencias
        assertEquals(3, (int) destacados.get(0).getValue());
    }

    @Test
    public void s12_s11_competenciaSinResultadosNoTieneInformacion() {
        Equipo e = Fabrica.equipoConJugadores("Séneca", 8);
        Competencia c = liga(e);
        assertFalse(c.tieneResultados());
        assertTrue(c.clasificacionGoleadores().isEmpty());
        assertTrue(c.clasificacionDestacados().isEmpty());
    }

    // ---------------------------------------------------------------- HU-S09
    @Test
    public void s09_sesionConvocadaSinAsistenciaApareceComoPendiente() {
        Tenis tenis = Fabrica.tenis(2);
        Socio ana = Fabrica.socio("ana"), luis = Fabrica.socio("luis");
        ana.inscribirseEnDisciplina(tenis, "Intermedio");
        Entrenador ent = new Entrenador("ent", "x", "Ent", "E1");
        SesionEntrenamiento s = ent.planearSesionAcademia(AHORA.plusDays(1), 60, tenis.getInstalaciones().get(0), tenis, Arrays.asList(ana));
        assertEquals(Arrays.asList(s), ana.sesionesPendientesDeAsistencia());
        assertTrue(luis.sesionesPendientesDeAsistencia().isEmpty());
    }

    // ---------------------------------------------------------------- HU-A13
    @Test
    public void a13_reservasConRangoYFiltroPorSocio() {
        Tenis tenis = Fabrica.tenis(2);
        sistema.registrarDisciplina(tenis);
        Socio ana = Fabrica.socio("ana"), luis = Fabrica.socio("luis");
        ana.inscribirseEnDisciplina(tenis, "Principiante");
        luis.inscribirseEnDisciplina(tenis, "Principiante");
        ana.reservarInstalacion(tenis.getInstalaciones().get(0), Fabrica.BLOQUE, Fabrica.BLOQUE.plusHours(1), 2, "sencillos");
        LocalDate d = Fabrica.BLOQUE.toLocalDate();
        assertEquals(1, admin.historialReservas(sistema, d.minusDays(1), d.plusDays(1), null, null).size());
        assertEquals(1, admin.historialReservas(sistema, d, d, ana, tenis).size());
        assertTrue(admin.historialReservas(sistema, d, d, luis, null).isEmpty());                 // filtro por socio
        assertTrue(admin.historialReservas(sistema, d.plusDays(1), d.plusDays(5), null, null).isEmpty());   // periodo vacío
        assertThrows(IllegalArgumentException.class, () -> admin.historialReservas(sistema, d.plusDays(1), d, null, null));
    }

    @Test
    public void a13_sesionesConFiltroPorEntrenadorYRangoInvertido() {
        Tenis tenis = Fabrica.tenis(2);
        sistema.registrarDisciplina(tenis);
        Socio ana = Fabrica.socio("ana");
        ana.inscribirseEnDisciplina(tenis, "Intermedio");
        Entrenador ent = new Entrenador("ent", "x", "Ent", "E1"), otro = new Entrenador("otro", "x", "Otro", "E2");
        ent.planearSesionAcademia(AHORA.plusDays(1), 60, tenis.getInstalaciones().get(0), tenis, Arrays.asList(ana));
        LocalDate d = AHORA.plusDays(1).toLocalDate();
        assertEquals(1, admin.historialSesiones(sistema, d, d, ent, null).size());
        assertTrue(admin.historialSesiones(sistema, d, d, otro, null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> admin.historialSesiones(sistema, d.plusDays(1), d, null, null));
        assertThrows(IllegalArgumentException.class, () -> admin.historialPartidosInternos(sistema, d.plusDays(1), d, null));
        assertThrows(IllegalArgumentException.class, () -> admin.historialPartidosCompetencia(sistema, d.plusDays(1), d, null));
    }

    // ---------------------------------------------------------------- HU-A14 (mensualidad)
    @Test
    public void a14_mensualidadesAparecenEnElReporteDeVentas() {
        Socio ana = Fabrica.registrar(sistema, Fabrica.socio("ana"));
        assertEquals("No hay datos para ese periodo.", admin.reporteVentasDetallado(sistema, "mensualidad", "mensual", LocalDate.now()));
        admin.registrarPagoMensualidad(sistema, "ana", YearMonth.of(2030, 3), 50000);
        String reporte = admin.reporteVentasDetallado(sistema, "mensualidad", "mensual", LocalDate.now());
        assertTrue(reporte, reporte.contains("Ventas: 1"));
        assertTrue(reporte, reporte.contains("Total: 50000"));
    }

    // ---------------------------------------------------------------- HU-A15
    @Test
    public void a15_reporteDeportivoMuestraLaEvolucionDelEloEnElPeriodo() {
        Tenis tenis = Fabrica.tenis(2);
        Socio ana = Fabrica.registrar(sistema, Fabrica.socio("ana"));
        InscripcionDisciplina i = ana.inscribirseEnDisciplina(tenis, "Principiante");
        i.actualizarRanking(true, 1000);
        String reporte = admin.reporteDeportivo(sistema, LocalDate.of(2030, 3, 1), LocalDate.of(2030, 3, 31));
        assertTrue(reporte, reporte.contains("ELO: 1000 -> 1016"));
        assertEquals("No hay datos para ese periodo.", admin.reporteDeportivo(sistema, LocalDate.of(2031, 1, 1), LocalDate.of(2031, 1, 31)));
        assertThrows(IllegalArgumentException.class, () -> admin.reporteDeportivo(sistema, LocalDate.of(2030, 4, 1), LocalDate.of(2030, 3, 1)));
    }
}
