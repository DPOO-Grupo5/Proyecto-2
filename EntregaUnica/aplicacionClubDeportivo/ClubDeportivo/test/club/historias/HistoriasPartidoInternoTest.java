package club.historias;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.model.Reloj;
import club.modelo.competencias.Baloncesto;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.DeporteConjunto;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.EstadisticaJugadorBaloncesto;
import club.modelo.competencias.EstadisticaJugadorFutbol;
import club.modelo.competencias.Futbol;
import club.modelo.competencias.InscripcionEquipo;
import club.modelo.competencias.PartidoInterno;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;

/** HU-T04 a HU-T08: programar un partido interno, alineación, resultado, estadísticas y tarjetas. */
public class HistoriasPartidoInternoTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 3, 1, 9, 0);
    private static final LocalDateTime FECHA = AHORA.plusDays(2);

    private DeporteConjunto futbol;
    private Categoria mayores;
    private Equipo a, b;
    private Entrenador entrenador, otro;

    @Before
    public void preparar() {
        Reloj.fijar(AHORA);
        futbol = new Futbol("Fútbol", 7);
        mayores = new Categoria("Mayores", 18, 150);
        a = Fabrica.equipoConJugadores("Equipo A", 8, futbol, mayores);
        b = Fabrica.equipoConJugadores("Equipo B", 8, futbol, mayores);
        entrenador = new Entrenador("ent", "x", "Entrenador", "E1");
        otro = new Entrenador("otro", "x", "Otro", "E2");
    }

    @After
    public void restablecerReloj() { Reloj.usarRelojDelSistema(); }

    private static List<Socio> jugadores(Equipo e, int n) {
        List<Socio> lista = new ArrayList<>();
        for (InscripcionEquipo i : e.getInscripcionesActivas()) if (lista.size() < n) lista.add(i.getSocio());
        return lista;
    }

    /** Programa, alinea a los primeros 7 de cada equipo y deja el reloj después del partido. */
    private PartidoInterno partidoListoParaResultado() {
        PartidoInterno p = entrenador.programarPartidoInterno(a, b, FECHA);
        p.definirAlineacion(a, jugadores(a, 7));
        p.definirAlineacion(b, jugadores(b, 7));
        Reloj.fijar(FECHA.plusHours(2));
        return p;
    }

    // ===================================================================== HU-T04 · Programar un partido interno
    @Test
    public void t04_exito_quedaRegistradoComoProgramado() {
        PartidoInterno p = entrenador.programarPartidoInterno(a, b, FECHA);
        assertFalse(p.isFinalizado());
        assertSame(entrenador, p.getEntrenador());
        assertTrue(a.getPartidosInternos().contains(p));
        assertTrue(b.getPartidosInternos().contains(p));
    }

    @Test
    public void t04_rechazo_categoriaODeporteDistintoMismoEquipoOFechaPasada() {
        Equipo otraCategoria = Fabrica.equipoConJugadores("Sub17", 8, futbol, new Categoria("Sub-17", 0, 150));
        Equipo baloncesto = Fabrica.equipoConJugadores("Balón", 8, new Baloncesto("Baloncesto", 5), mayores);
        assertThrows(IllegalStateException.class, () -> entrenador.programarPartidoInterno(a, otraCategoria, FECHA));
        assertThrows(IllegalStateException.class, () -> entrenador.programarPartidoInterno(a, baloncesto, FECHA));
        assertThrows(IllegalArgumentException.class, () -> entrenador.programarPartidoInterno(a, a, FECHA));
        assertThrows(IllegalArgumentException.class, () -> entrenador.programarPartidoInterno(a, b, AHORA.minusDays(1)));
        assertTrue(a.getPartidosInternos().isEmpty());
    }

    @Test
    public void t04_rechazo_equipoSinElMinimoIndicaCualYCuantosFaltan() {
        Equipo corto = Fabrica.equipoConJugadores("Corto", 5, futbol, mayores);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> entrenador.programarPartidoInterno(a, corto, FECHA));
        assertTrue(e.getMessage(), e.getMessage().contains("Corto") && e.getMessage().contains("faltan 2"));
    }

    // ===================================================================== HU-T05 · Definir la alineación
    @Test
    public void t05_exito_jugadoresActivosNoSuspendidos() {
        PartidoInterno p = entrenador.programarPartidoInterno(a, b, FECHA);
        p.definirAlineacion(a, jugadores(a, 7));
        assertEquals(7, p.getAlineacion(a).size());
    }

    @Test
    public void t05_rechazo_jugadorSuspendidoInformaLaSuspension() {
        PartidoInterno primero = partidoListoParaResultado();
        Socio expulsado = jugadores(a, 1).get(0);
        entrenador.registrarResultadoInterno(primero, 1, 0,
                Arrays.asList(new EstadisticaJugadorFutbol(expulsado, 0, 0, 0, 1)));
        Reloj.fijar(AHORA);
        PartidoInterno segundo = entrenador.programarPartidoInterno(a, b, FECHA.plusDays(7));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> segundo.definirAlineacion(a, jugadores(a, 7)));
        assertTrue(e.getMessage(), e.getMessage().contains("suspendido"));
        assertTrue(segundo.getAlineacion(a).isEmpty());
    }

    @Test
    public void t05_exito_conUnaSolaAmarillaSePuedeAlinear() {
        PartidoInterno primero = partidoListoParaResultado();
        Socio amonestado = jugadores(a, 1).get(0);
        entrenador.registrarResultadoInterno(primero, 1, 0, Arrays.asList(new EstadisticaJugadorFutbol(amonestado, 0, 0, 1, 0)));
        Reloj.fijar(AHORA);
        PartidoInterno segundo = entrenador.programarPartidoInterno(a, b, FECHA.plusDays(7));
        segundo.definirAlineacion(a, jugadores(a, 7));
        assertTrue(segundo.getAlineacion(a).contains(amonestado));
    }

    @Test
    public void t05_exito_despuesDeCumplirLaSuspensionVuelveASerAlineable() {
        PartidoInterno primero = partidoListoParaResultado();
        Socio expulsado = jugadores(a, 1).get(0);
        entrenador.registrarResultadoInterno(primero, 1, 0, Arrays.asList(new EstadisticaJugadorFutbol(expulsado, 0, 0, 0, 1)));
        // Segundo partido: el expulsado no juega (cumple la suspensión).
        Reloj.fijar(AHORA);
        PartidoInterno segundo = entrenador.programarPartidoInterno(a, b, FECHA.plusDays(7));
        List<Socio> sinExpulsado = jugadores(a, 8);
        sinExpulsado.remove(expulsado);
        segundo.definirAlineacion(a, sinExpulsado);
        segundo.definirAlineacion(b, jugadores(b, 7));
        Reloj.fijar(FECHA.plusDays(7).plusHours(2));
        entrenador.registrarResultadoInterno(segundo, 0, 0, new ArrayList<>());
        // Tercer partido: ya puede jugar.
        PartidoInterno tercero = entrenador.programarPartidoInterno(a, b, FECHA.plusDays(14));
        tercero.definirAlineacion(a, jugadores(a, 7));
        assertTrue(tercero.getAlineacion(a).contains(expulsado));
    }

    @Test
    public void t05_rechazo_jugadorDeOtroEquipo() {
        PartidoInterno p = entrenador.programarPartidoInterno(a, b, FECHA);
        List<Socio> mezcla = jugadores(a, 6);
        mezcla.add(jugadores(b, 1).get(0));
        assertThrows(IllegalArgumentException.class, () -> p.definirAlineacion(a, mezcla));
        assertTrue(p.getAlineacion(a).isEmpty());
    }

    // ===================================================================== HU-T06 · Resultado de un partido interno
    @Test
    public void t06_exito_partidoYaDisputadoQuedaFinalizado() {
        PartidoInterno p = partidoListoParaResultado();
        entrenador.registrarResultadoInterno(p, 3, 1, new ArrayList<>());
        assertTrue(p.isFinalizado());
        assertEquals("3 - 1", p.getMarcadorFinal());
    }

    @Test
    public void t06_rechazo_partidoQueAunNoSeDisputa() {
        PartidoInterno p = entrenador.programarPartidoInterno(a, b, FECHA);
        p.definirAlineacion(a, jugadores(a, 7));
        p.definirAlineacion(b, jugadores(b, 7));
        assertThrows(IllegalStateException.class, () -> entrenador.registrarResultadoInterno(p, 1, 0, new ArrayList<>()));
        assertFalse(p.isFinalizado());
    }

    @Test
    public void t06_rechazo_noProgramadoPorMiYaConResultadoOMarcadorNegativo() {
        PartidoInterno p = partidoListoParaResultado();
        assertThrows(IllegalStateException.class, () -> otro.registrarResultadoInterno(p, 1, 0, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoInterno(p, -1, 0, new ArrayList<>()));
        entrenador.registrarResultadoInterno(p, 1, 0, new ArrayList<>());
        assertThrows(IllegalStateException.class, () -> entrenador.registrarResultadoInterno(p, 2, 0, new ArrayList<>()));
        assertEquals("1 - 0", p.getMarcadorFinal());
    }

    @Test
    public void t06_rechazo_empateEnBaloncesto() {
        DeporteConjunto baloncesto = new Baloncesto("Baloncesto", 5);
        Equipo x = Fabrica.equipoConJugadores("X", 6, baloncesto, mayores);
        Equipo y = Fabrica.equipoConJugadores("Y", 6, baloncesto, mayores);
        PartidoInterno p = entrenador.programarPartidoInterno(x, y, FECHA);
        p.definirAlineacion(x, jugadores(x, 5));
        p.definirAlineacion(y, jugadores(y, 5));
        Reloj.fijar(FECHA.plusHours(2));
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoInterno(p, 80, 80, new ArrayList<>()));
        assertFalse(p.isFinalizado());
    }

    // ===================================================================== HU-T07 · Estadísticas individuales
    @Test
    public void t07_exito_estadisticasDelDeporteParaUnJugadorAlineado() {
        PartidoInterno p = partidoListoParaResultado();
        Socio goleador = jugadores(a, 1).get(0);
        entrenador.registrarResultadoInterno(p, 2, 0, Arrays.asList(new EstadisticaJugadorFutbol(goleador, 2, 1, 0, 0)));
        assertEquals(1, p.getEstadisticas().size());
        assertEquals(1, goleador.getEstadisticas().size());
    }

    @Test
    public void t07_rechazo_jugadorNoAlineadoOEstadisticaDeOtroDeporte() {
        PartidoInterno p = partidoListoParaResultado();
        Socio suplente = jugadores(a, 8).get(7);   // el octavo no está en la alineación
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoInterno(p, 1, 0,
                Arrays.asList(new EstadisticaJugadorFutbol(suplente, 1, 0, 0, 0))));
        Socio titular = jugadores(a, 1).get(0);
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoInterno(p, 1, 0,
                Arrays.asList(new EstadisticaJugadorBaloncesto(titular, 10, 3))));
        assertFalse("Un rechazo no deja el partido a medias", p.isFinalizado());
        assertThrows(IllegalArgumentException.class, () -> new EstadisticaJugadorFutbol(titular, -1, 0, 0, 0));
    }

    // ===================================================================== HU-T08 · Tarjetas en fútbol
    @Test
    public void t08_rojaODosAmarillasSuspendenYUnaAmarillaNo() {
        PartidoInterno p = partidoListoParaResultado();
        List<Socio> titulares = jugadores(a, 3);
        entrenador.registrarResultadoInterno(p, 1, 1, Arrays.asList(
                new EstadisticaJugadorFutbol(titulares.get(0), 0, 0, 0, 1),
                new EstadisticaJugadorFutbol(titulares.get(1), 0, 0, 2, 0),
                new EstadisticaJugadorFutbol(titulares.get(2), 0, 0, 1, 0)));
        assertTrue(titulares.get(0).tieneSancionVigente());
        assertTrue(titulares.get(1).tieneSancionVigente());
        assertFalse(titulares.get(2).tieneSancionVigente());
    }
}
