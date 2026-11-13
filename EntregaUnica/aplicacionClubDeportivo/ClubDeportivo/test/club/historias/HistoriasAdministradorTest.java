package club.historias;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.model.SistemaClub;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.Competencia;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.FormatoLiga;
import club.modelo.competencias.FormatoPorFases;
import club.modelo.competencias.InscripcionEquipo;
import club.modelo.competencias.InstitucionRival;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.competencias.ReglaFutbol;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Entrenador;
import club.modelo.tienda.PagoMensualidad;
import club.modelo.usuarios.Socio;

/** Pruebas de integración derivadas de las historias del administrador (banco oficial). */
public class HistoriasAdministradorTest {
    private SistemaClub sistema;
    private Administrador admin;

    @Before
    public void preparar() {
        sistema = new SistemaClub();
        admin = new Administrador("admin", "admin123", "Administrador");
        sistema.registrarUsuario(admin);
    }

    // ===================================================================== HU-A01 · Iniciar sesión como administrador
    @Test
    public void a01_exito_credencialesValidasDeAdministrador() {
        assertSame(admin, sistema.autenticar("admin", "admin123"));
    }

    @Test
    public void a01_rechazo_noRevelaQueDatoFallo() {
        assertNull(sistema.autenticar("admin", "incorrecta"));
        assertNull(sistema.autenticar("otro", "admin123"));
    }

    // ===================================================================== HU-A04 · Inscribir a un socio en un equipo
    /** Equipo de un deporte con mínimo de 1 habilitado, para probar cupos pequeños (HU-A03 exige cupo >= mínimo). */
    private Equipo equipoPequenoSub15(int cupo) {
        return admin.crearEquipo("Séneca Sub-15", new club.modelo.competencias.Futbol("Fútbol", 1), sistema.obtenerCategoria("Sub-15"), cupo, sistema);
    }

    private Equipo equipoSub15(int cupo) {
        Equipo e = admin.crearEquipo("Séneca Sub-15", sistema.obtenerDeporte("Fútbol"), sistema.obtenerCategoria("Sub-15"), cupo, sistema);
        return e;
    }

    @Test
    public void a04_exito_quedaCreadoComoJugadorDelEquipo() {
        Equipo equipo = equipoSub15(15);
        Socio socio = Fabrica.socioConEdad("juan", 14);

        InscripcionEquipo i = admin.inscribirJugadorEnEquipo(socio, equipo, "delantero", 9);

        assertTrue(equipo.getInscripcionesActivas().contains(i));
        assertTrue(socio.getEquipos().contains(equipo));
        assertEquals(9, i.getNumeroCamiseta());
    }

    @Test
    public void a04_rechazoPorEdad_socioDe20EnEquipoSub15() {
        Equipo equipo = equipoSub15(15);
        Socio adulto = Fabrica.socioConEdad("adulto", 20);
        assertThrows(IllegalArgumentException.class, () -> admin.inscribirJugadorEnEquipo(adulto, equipo, "defensa", 4));
        assertTrue(equipo.getInscripcionesActivas().isEmpty());
    }

    @Test
    public void a04_rechazoPorEquipoRepetido_yaEstaEnOtroEquipoDelMismoDeporte() {
        Equipo a = equipoSub15(15);
        Equipo b = admin.crearEquipo("Séneca Sub-15 B", sistema.obtenerDeporte("Fútbol"), sistema.obtenerCategoria("Sub-15"), 15, sistema);
        Socio socio = Fabrica.socioConEdad("juan", 14);
        admin.inscribirJugadorEnEquipo(socio, a, "delantero", 9);
        assertThrows(IllegalArgumentException.class, () -> admin.inscribirJugadorEnEquipo(socio, b, "delantero", 9));
        assertTrue(b.getInscripcionesActivas().isEmpty());
    }

    @Test
    public void a04_rechazoPorCamiseta_numeroYaUsadoEnElEquipo() {
        Equipo equipo = equipoSub15(15);
        admin.inscribirJugadorEnEquipo(Fabrica.socioConEdad("uno", 14), equipo, "delantero", 9);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> admin.inscribirJugadorEnEquipo(Fabrica.socioConEdad("dos", 14), equipo, "defensa", 9));
        assertTrue(e.getMessage().contains("camiseta"));
    }

    @Test
    public void a04_rechazoPorCupo_equipoLleno() {
        Equipo equipo = equipoPequenoSub15(1);
        admin.inscribirJugadorEnEquipo(Fabrica.socioConEdad("uno", 14), equipo, "delantero", 9);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> admin.inscribirJugadorEnEquipo(Fabrica.socioConEdad("dos", 14), equipo, "portero", 1));
        assertTrue(e.getMessage().contains("cupo"));
        assertEquals(1, equipo.getInscripcionesActivas().size());
    }

    @Test
    public void a04_rechazoPorDatos_equipoInactivoOPosicionInexistente() {
        Equipo equipo = equipoSub15(15);
        assertThrows(IllegalArgumentException.class,
                () -> admin.inscribirJugadorEnEquipo(Fabrica.socioConEdad("uno", 14), equipo, "pívot", 9));
        equipo.darDeBaja();
        assertThrows(IllegalStateException.class,
                () -> admin.inscribirJugadorEnEquipo(Fabrica.socioConEdad("dos", 14), equipo, "delantero", 10));
        assertTrue(equipo.getInscripcionesActivas().isEmpty());
    }

    // ===================================================================== HU-A05 · Dar de baja a un jugador
    @Test
    public void a05_exito_liberaCupoYCamisetaYConservaElHistorial() {
        Equipo equipo = equipoPequenoSub15(1);
        Socio juan = Fabrica.socioConEdad("juan", 14);
        admin.inscribirJugadorEnEquipo(juan, equipo, "delantero", 9);

        admin.darDeBajaJugador(sistema, equipo, juan);

        assertTrue(equipo.getInscripcionesActivas().isEmpty());
        assertFalse(juan.getInscripcionesEquipo().get(0).isActiva());
        assertEquals(1, juan.getInscripcionesEquipo().size());
        // El cupo (1) y la camiseta 9 quedan libres para otro jugador.
        admin.inscribirJugadorEnEquipo(Fabrica.socioConEdad("pedro", 14), equipo, "portero", 9);
        assertEquals(1, equipo.getInscripcionesActivas().size());
    }

    @Test
    public void a05_rechazo_jugadorEnLaNominaDeUnaCompetenciaEnCurso() {
        Equipo equipo = Fabrica.equipoConJugadores("Séneca", 7);
        sistema.registrarEquipo(equipo);
        Competencia c = admin.crearCompetencia("Copa", "ASCUN", "2026", equipo.getDeporte(), equipo.getCategoria(),
                new FormatoLiga("Liga", new ReglaFutbol(3, 0)), sistema);
        c.agregarEquipo(equipo);
        java.util.List<Socio> nomina = new java.util.ArrayList<>();
        for (InscripcionEquipo i : equipo.getInscripcionesActivas()) nomina.add(i.getSocio());
        c.registrarNomina(equipo, nomina);
        Socio jugador = nomina.get(0);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> admin.darDeBajaJugador(sistema, equipo, jugador));
        assertTrue(e.getMessage(), e.getMessage().contains("Copa"));
        assertEquals(7, equipo.getInscripcionesActivas().size());
    }

    @Test
    public void a05_rechazo_jugadorYaDadoDeBaja() {
        Equipo equipo = equipoSub15(15);
        Socio juan = Fabrica.socioConEdad("juan", 14);
        admin.inscribirJugadorEnEquipo(juan, equipo, "delantero", 9);
        admin.darDeBajaJugador(sistema, equipo, juan);
        assertThrows(IllegalStateException.class, () -> admin.darDeBajaJugador(sistema, equipo, juan));
    }

    // ===================================================================== HU-A17 · Registrar una competencia
    private Competencia competencia(String nombre, String temporada) {
        return admin.crearCompetencia(nombre, "ASCUN", temporada, sistema.obtenerDeporte("Fútbol"), sistema.obtenerCategoria("Mayores"),
                new FormatoLiga("Liga", new ReglaFutbol(3, 0)), sistema);
    }

    @Test
    public void a17_exito_quedaCreadaConSusDatos() {
        Competencia c = competencia("Copa Andes", "2026");
        assertTrue(sistema.getCompetencias().contains(c));
        assertEquals("ASCUN", c.getOrganizador());
        assertEquals("2026", c.getTemporada());
    }

    @Test
    public void a17_exito_formatoDeGruposConEliminacion() {
        Competencia c = admin.crearCompetencia("Torneo Cerros", "Liga Bogotá", "2026", sistema.obtenerDeporte("Fútbol"),
                sistema.obtenerCategoria("Mayores"), new FormatoPorFases("Grupos y eliminación"), sistema);
        assertTrue(c.getFormato() instanceof FormatoPorFases);
    }

    @Test
    public void a17_rechazo_mismoNombreYTemporada() {
        competencia("Copa Andes", "2026");
        assertThrows(IllegalArgumentException.class, () -> competencia("copa andes", "2026"));
        assertEquals(1, sistema.getCompetencias().size());
        competencia("Copa Andes", "2027");   // otra temporada sí se permite
        assertEquals(2, sistema.getCompetencias().size());
    }

    @Test
    public void a17_rechazo_datoFaltante() {
        Categoria mayores = sistema.obtenerCategoria("Mayores");
        assertThrows(IllegalArgumentException.class, () -> admin.crearCompetencia("", "ASCUN", "2026",
                sistema.obtenerDeporte("Fútbol"), mayores, new FormatoLiga("Liga", new ReglaFutbol(3, 0)), sistema));
        assertThrows(IllegalArgumentException.class, () -> admin.crearCompetencia("Copa", "ASCUN", "2026",
                sistema.obtenerDeporte("Fútbol"), mayores, null, sistema));
        assertTrue(sistema.getCompetencias().isEmpty());
    }

    // ===================================================================== HU-A16 · Registrar el pago de una mensualidad
    @Test
    public void a16_exito_quedaRegistradoEnElRubroMensualidades() {
        Socio socio = Fabrica.registrar(sistema, Fabrica.socio("pagador"));
        PagoMensualidad pago = admin.registrarPagoMensualidad(sistema, "pagador", YearMonth.of(2026, 11), 150000);
        assertSame(socio, pago.getSocio());
        assertEquals(YearMonth.of(2026, 11), pago.getMes());
        assertEquals(150000, pago.getValor(), 0.001);
        assertEquals("mensualidad", pago.getRubro());
        assertTrue(sistema.getPagosMensualidad().contains(pago));
    }

    @Test
    public void a16_rechazo_mesYaPagadoPorDuplicado() {
        Fabrica.registrar(sistema, Fabrica.socio("pagador"));
        admin.registrarPagoMensualidad(sistema, "pagador", YearMonth.of(2026, 11), 150000);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> admin.registrarPagoMensualidad(sistema, "pagador", YearMonth.of(2026, 11), 150000));
        assertTrue(e.getMessage().contains("ya"));
        assertEquals(1, sistema.getPagosMensualidad().size());
        admin.registrarPagoMensualidad(sistema, "pagador", YearMonth.of(2026, 12), 150000);   // otro mes sí
        assertEquals(2, sistema.getPagosMensualidad().size());
    }

    @Test
    public void a16_rechazo_valorNoPositivoOSocioInexistente() {
        Fabrica.registrar(sistema, Fabrica.socio("pagador"));
        assertThrows(IllegalArgumentException.class, () -> admin.registrarPagoMensualidad(sistema, "pagador", YearMonth.of(2026, 11), 0));
        assertThrows(IllegalArgumentException.class, () -> admin.registrarPagoMensualidad(sistema, "pagador", YearMonth.of(2026, 11), -5));
        assertThrows(IllegalArgumentException.class, () -> admin.registrarPagoMensualidad(sistema, "nadie", YearMonth.of(2026, 11), 150000));
        // Un login que existe pero no es socio tampoco se acepta.
        assertThrows(IllegalArgumentException.class, () -> admin.registrarPagoMensualidad(sistema, "admin", YearMonth.of(2026, 11), 150000));
        assertTrue(sistema.getPagosMensualidad().isEmpty());
    }

    // ===================================================================== HU-A19 · Programar un partido oficial
    /** Competencia de liga con un equipo del club y una institución rival: el calendario propone un partido. */
    private PartidoCompetencia partidoPropuesto() {
        Equipo equipo = Fabrica.equipoConJugadores("Séneca", 7);
        sistema.registrarEquipo(equipo);
        Competencia c = admin.crearCompetencia("Copa Andes", "ASCUN", "2026", equipo.getDeporte(), equipo.getCategoria(),
                new FormatoLiga("Liga", new ReglaFutbol(3, 0)), sistema);
        c.agregarEquipo(equipo);
        c.agregarEquipo(new InstitucionRival("Javeriana"));
        c.generarCalendario();
        return c.getPartidos().get(0);
    }

    @Test
    public void a19_elCalendarioGeneraPartidosPropuestosSinProgramar() {
        PartidoCompetencia p = partidoPropuesto();
        assertFalse(p.isProgramado());
        assertNull(p.getEntrenadorResponsable());
    }

    @Test
    public void a19_exito_elVistoBuenoProgramaFechaEscenarioYEntrenador() {
        PartidoCompetencia p = partidoPropuesto();
        Entrenador entrenador = new Entrenador("ent", "x", "Entrenador", "E1");
        admin.programarPartidoOficial(p, LocalDate.of(2026, 12, 5), "Estadio El Campín", entrenador);
        assertTrue(p.isProgramado());
        assertEquals(LocalDate.of(2026, 12, 5), p.getFecha());
        assertEquals("Estadio El Campín", p.getEscenario());
        assertSame(entrenador, p.getEntrenadorResponsable());
        assertEquals("Javeriana", p.getEquipos().get(1).getNombre());
    }

    @Test
    public void a19_rechazo_faltaFechaEscenarioOEntrenador() {
        PartidoCompetencia p = partidoPropuesto();
        Entrenador entrenador = new Entrenador("ent", "x", "Entrenador", "E1");
        assertThrows(IllegalArgumentException.class, () -> admin.programarPartidoOficial(p, null, "Campín", entrenador));
        assertThrows(IllegalArgumentException.class, () -> admin.programarPartidoOficial(p, LocalDate.of(2026, 12, 5), "  ", entrenador));
        assertThrows(IllegalArgumentException.class, () -> admin.programarPartidoOficial(p, LocalDate.of(2026, 12, 5), "Campín", null));
        assertFalse(p.isProgramado());
    }

    @Test
    public void a19_rechazo_partidoSinCompetenciaOConEquipoNoInscrito() {
        PartidoCompetencia suelto = new PartidoCompetencia(null, "Pendiente", "Por definir");
        suelto.agregarEquipo(Fabrica.equipoConJugadores("Otro", 7));
        suelto.agregarEquipo(new InstitucionRival("Rosario"));
        Entrenador entrenador = new Entrenador("ent", "x", "Entrenador", "E1");
        assertThrows(IllegalStateException.class, () -> admin.programarPartidoOficial(suelto, LocalDate.of(2026, 12, 5), "Campín", entrenador));
        assertFalse(suelto.isProgramado());
    }

    // ===================================================================== HU-A20 · Restricción de uso personal
    @Test
    public void a20_elAdministradorNoPuedeReservarNiSerParticipante() {
        // La restricción se garantiza por diseño: reservar instalaciones y participar en partidos son
        // operaciones de Socio, y el administrador no es un Socio.
        assertFalse(Socio.class.isAssignableFrom(Administrador.class));
        assertFalse(sistema.getSocios().contains(admin));
    }
}
