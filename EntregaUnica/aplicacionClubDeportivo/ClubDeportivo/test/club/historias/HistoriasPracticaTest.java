package club.historias;

import static club.apoyo.Fabrica.BLOQUE;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.model.Reloj;
import club.modelo.entrenamiento.EstadisticaIndividual;
import club.modelo.entrenamiento.EstadisticaTenisMesa;
import club.modelo.entrenamiento.EstadisticaTenisPadel;
import club.modelo.entrenamiento.InscripcionDisciplina;
import club.modelo.entrenamiento.Padel;
import club.modelo.entrenamiento.PartidoPractica;
import club.modelo.entrenamiento.Tenis;
import club.modelo.entrenamiento.TenisMesa;
import club.modelo.instalaciones.MesaTenisMesa;
import club.modelo.instalaciones.Reserva;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;

/** HU-S04 (programar partido de práctica), HU-T10 (registrar su resultado) y HU-S07 (historial). */
public class HistoriasPracticaTest {
    private Tenis tenis;
    private Socio ana, juan, pedro, luis;
    private Entrenador entrenador;
    private Reserva reservaAna;

    @Before
    public void preparar() {
        Reloj.fijar(LocalDateTime.of(2030, 5, 1, 8, 0));
        tenis = Fabrica.tenis(2);
        ana = Fabrica.socio("ana");
        juan = Fabrica.socio("juan");
        pedro = Fabrica.socio("pedro");
        luis = Fabrica.socio("luis");
        ana.inscribirseEnDisciplina(tenis, "Intermedio");
        juan.inscribirseEnDisciplina(tenis, "Avanzado");
        pedro.inscribirseEnDisciplina(tenis, "Principiante");
        entrenador = new Entrenador("ent", "x", "Entrenador", "E1");
        reservaAna = ana.reservarInstalacion(tenis.getInstalaciones().get(0), BLOQUE, BLOQUE.plusHours(1), 2, "sencillos");
    }

    @After
    public void restablecerReloj() { Reloj.usarRelojDelSistema(); }

    private static EstadisticaTenisPadel stats(int sets, int games) { return new EstadisticaTenisPadel(sets, games, 0, 0); }

    // ===================================================================== HU-S04 · Programar un partido de práctica
    @Test
    public void s04_exito_sobreMiReservaConParticipantesDeNivelAdyacente() {
        PartidoPractica p = ana.programarPartidoPractica(reservaAna, Arrays.asList(juan), null);
        assertSame(reservaAna, p.getReserva());
        assertEquals(BLOQUE, p.getFechaHora());
        assertEquals(2, p.getParticipantes().size());
        assertTrue(ana.getPartidosPractica().contains(p));
        assertTrue(juan.getPartidosPractica().contains(p));
    }

    @Test
    public void s04_exito_doblesEnPadelConCuatroParticipantes() {
        Padel padel = Fabrica.padel();
        for (Socio s : Arrays.asList(ana, juan, pedro, luis)) s.inscribirseEnDisciplina(padel, "Intermedio");
        Reserva r = ana.reservarInstalacion(padel.getInstalaciones().get(0), BLOQUE.plusHours(3), BLOQUE.plusHours(4), 4, "dobles");
        PartidoPractica p = ana.programarPartidoPractica(r, Arrays.asList(juan, pedro, luis), null);
        assertEquals(4, p.getParticipantes().size());
        assertEquals(Arrays.asList(ana, juan), Arrays.asList(p.getLadoA().get(0).getSocio(), p.getLadoA().get(1).getSocio()));
    }

    @Test
    public void s04_excepcion_conEntrenadorSeAceptanNivelesNoAdyacentes() {
        Reserva r = pedro.reservarInstalacion(tenis.getInstalaciones().get(1), BLOQUE, BLOQUE.plusHours(1), 2, "sencillos");
        assertThrows(IllegalArgumentException.class, () -> pedro.programarPartidoPractica(r, Arrays.asList(juan), null));
        assertNotNull(pedro.programarPartidoPractica(r, Arrays.asList(juan), entrenador));
    }

    @Test
    public void s04_rechazoPorContexto_sinReservaActivaOReservaAjena() {
        assertThrows(IllegalArgumentException.class, () -> ana.programarPartidoPractica(null, Arrays.asList(juan), null));
        assertThrows(IllegalStateException.class, () -> juan.programarPartidoPractica(reservaAna, Arrays.asList(ana), null));
        Reloj.fijar(BLOQUE.plusHours(2));   // la reserva ya terminó
        assertThrows(IllegalStateException.class, () -> ana.programarPartidoPractica(reservaAna, Arrays.asList(juan), null));
        assertTrue(ana.getPartidosPractica().isEmpty());
    }

    @Test
    public void s04_rechazoPorContexto_participanteNoInscritoRepetidoONumeroDistinto() {
        assertThrows(IllegalStateException.class, () -> ana.programarPartidoPractica(reservaAna, Arrays.asList(luis), null));
        assertThrows(IllegalArgumentException.class, () -> ana.programarPartidoPractica(reservaAna, Arrays.asList(ana), null));
        assertThrows(IllegalArgumentException.class, () -> ana.programarPartidoPractica(reservaAna, Arrays.asList(juan, pedro), null));
        assertThrows(IllegalArgumentException.class, () -> ana.programarPartidoPractica(reservaAna, Collections.<Socio>emptyList(), null));
        assertTrue(ana.getPartidosPractica().isEmpty());
    }

    @Test
    public void s04_rechazoPorContexto_unaReservaAdmiteUnSoloPartido() {
        ana.programarPartidoPractica(reservaAna, Arrays.asList(juan), null);
        assertThrows(IllegalStateException.class, () -> ana.programarPartidoPractica(reservaAna, Arrays.asList(juan), null));
    }

    @Test
    public void s04_rechazoPorAgenda_participanteConOtroPartidoEnElMismoBloque() {
        ana.programarPartidoPractica(reservaAna, Arrays.asList(juan), null);
        Reserva otra = pedro.reservarInstalacion(tenis.getInstalaciones().get(1), BLOQUE.plusMinutes(30), BLOQUE.plusMinutes(90), 2, "sencillos");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> pedro.programarPartidoPractica(otra, Arrays.asList(juan), entrenador));
        assertTrue(e.getMessage().contains(juan.getNombre()));
    }

    // ===================================================================== HU-T10 · Resultado de un partido de práctica
    @Test
    public void t10_exito_guardaElResultadoYActualizaElEloDeCadaParticipante() {
        PartidoPractica p = ana.programarPartidoPractica(reservaAna, Arrays.asList(juan), null);
        entrenador.registrarResultadoPractica(p, Arrays.<EstadisticaIndividual>asList(stats(2, 12), stats(1, 9)));
        assertTrue(p.isResultadoRegistrado());
        assertTrue(ana.getInscripcionesDisciplina().get(0).getRanking() > 1000);
        assertTrue(juan.getInscripcionesDisciplina().get(0).getRanking() < 1000);
        assertTrue(p.ganoLadoA());
    }

    @Test
    public void t10_exito_dobles_losCompanerosCompartenSetsYCadaUnoActualizaSuElo() {
        Padel padel = Fabrica.padel();
        for (Socio s : Arrays.asList(ana, juan, pedro, luis)) s.inscribirseEnDisciplina(padel, "Intermedio");
        Reserva r = ana.reservarInstalacion(padel.getInstalaciones().get(0), BLOQUE.plusHours(3), BLOQUE.plusHours(4), 4, "dobles");
        PartidoPractica p = ana.programarPartidoPractica(r, Arrays.asList(juan, pedro, luis), null);
        entrenador.registrarResultadoPractica(p, Arrays.<EstadisticaIndividual>asList(stats(0, 3), stats(0, 4), stats(2, 12), stats(2, 12)));
        for (InscripcionDisciplina i : p.getLadoA()) assertTrue(i.getRanking() < 1000);
        for (InscripcionDisciplina i : p.getLadoB()) assertTrue(i.getRanking() > 1000);
        assertThrows(IllegalStateException.class, () -> entrenador.registrarResultadoPractica(p,
                Arrays.<EstadisticaIndividual>asList(stats(0, 3), stats(0, 4), stats(2, 12), stats(2, 12))));
    }

    @Test
    public void t10_rechazo_partidoInexistenteOConResultado() {
        PartidoPractica p = ana.programarPartidoPractica(reservaAna, Arrays.asList(juan), null);
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoPractica(null, Arrays.<EstadisticaIndividual>asList(stats(2, 12), stats(0, 4))));
        entrenador.registrarResultadoPractica(p, Arrays.<EstadisticaIndividual>asList(stats(2, 12), stats(0, 4)));
        int elo = ana.getInscripcionesDisciplina().get(0).getRanking();
        assertThrows(IllegalStateException.class, () -> entrenador.registrarResultadoPractica(p, Arrays.<EstadisticaIndividual>asList(stats(2, 12), stats(0, 4))));
        assertEquals(elo, ana.getInscripcionesDisciplina().get(0).getRanking());
    }

    @Test
    public void t10_rechazo_marcadorDeSetsImposible() {
        PartidoPractica p = ana.programarPartidoPractica(reservaAna, Arrays.asList(juan), null);
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoPractica(p, Arrays.<EstadisticaIndividual>asList(stats(2, 12), stats(2, 12))));
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoPractica(p, Arrays.<EstadisticaIndividual>asList(stats(1, 6), stats(0, 4))));
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoPractica(p, Arrays.<EstadisticaIndividual>asList(stats(3, 18), stats(0, 4))));
        assertFalse(p.isResultadoRegistrado());
        assertEquals(1000, ana.getInscripcionesDisciplina().get(0).getRanking());
    }

    @Test
    public void t10_rechazo_estadisticaDeOtraDisciplina() {
        PartidoPractica p = ana.programarPartidoPractica(reservaAna, Arrays.asList(juan), null);
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoPractica(p,
                Arrays.<EstadisticaIndividual>asList(new EstadisticaTenisMesa(2, 30), new EstadisticaTenisMesa(1, 25))));
        assertFalse(p.isResultadoRegistrado());
    }

    @Test
    public void t10_tenisDeMesaSeJuegaAlMejorDeCinco() {
        TenisMesa mesa = new TenisMesa(2);
        mesa.registrarInstalacion(new MesaTenisMesa(2, 4, "Mesa", false));
        ana.inscribirseEnDisciplina(mesa, "Intermedio");
        juan.inscribirseEnDisciplina(mesa, "Intermedio");
        Reserva r = ana.reservarInstalacion(mesa.getInstalaciones().get(0), BLOQUE.plusHours(3), BLOQUE.plusHours(4), 2, "sencillos");
        PartidoPractica p = ana.programarPartidoPractica(r, Arrays.asList(juan), null);
        assertThrows(IllegalArgumentException.class, () -> entrenador.registrarResultadoPractica(p,
                Arrays.<EstadisticaIndividual>asList(new EstadisticaTenisMesa(2, 30), new EstadisticaTenisMesa(1, 25))));
        entrenador.registrarResultadoPractica(p, Arrays.<EstadisticaIndividual>asList(new EstadisticaTenisMesa(3, 40), new EstadisticaTenisMesa(2, 38)));
        assertTrue(p.isResultadoRegistrado());
    }

    // ===================================================================== HU-S07 · Historial de partidos de práctica
    private PartidoPractica jugado(LocalDateTime inicio, int setsAna, int setsJuan) {
        Reserva r = ana.reservarInstalacion(tenis.getInstalaciones().get(0), inicio, inicio.plusHours(1), 2, "sencillos");
        PartidoPractica p = ana.programarPartidoPractica(r, Arrays.asList(juan), null);
        entrenador.registrarResultadoPractica(p, Arrays.<EstadisticaIndividual>asList(stats(setsAna, 12), stats(setsJuan, 10)));
        return p;
    }

    @Test
    public void s07_exito_listaCronologicaConOponentesYResultado() {
        PartidoPractica tarde = jugado(BLOQUE.plusDays(3), 2, 1);
        PartidoPractica temprano = jugado(BLOQUE.plusDays(1), 0, 2);
        List<PartidoPractica> historial = ana.historialPracticas(tenis, null, null);
        assertEquals(Arrays.asList(temprano, tarde), historial);
        assertEquals(Arrays.asList(juan), temprano.oponentesDe(ana));
        assertEquals("Perdió 0-2", temprano.resultadoPara(ana));
        assertEquals("Ganó 2-1", tarde.resultadoPara(ana));
    }

    @Test
    public void s07_exito_filtraPorRangoDeFechas() {
        jugado(BLOQUE.plusDays(1), 2, 0);
        PartidoPractica segundo = jugado(BLOQUE.plusDays(5), 2, 0);
        LocalDate dia = BLOQUE.plusDays(5).toLocalDate();
        assertEquals(Arrays.asList(segundo), ana.historialPracticas(tenis, dia.minusDays(1), dia));
    }

    @Test
    public void s07_vacio_inscritoSinPartidosRecibeListaVacia() {
        assertTrue(pedro.historialPracticas(tenis, null, null).isEmpty());
    }

    @Test
    public void s07_rechazo_disciplinaNoInscritaORangoInvertido() {
        assertThrows(IllegalStateException.class, () -> luis.historialPracticas(tenis, null, null));
        assertThrows(IllegalArgumentException.class, () -> ana.historialPracticas(tenis, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 5, 1)));
    }
}
