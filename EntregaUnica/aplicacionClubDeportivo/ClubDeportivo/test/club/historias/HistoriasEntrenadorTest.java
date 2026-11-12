package club.historias;

import static org.junit.Assert.*;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.model.SistemaClub;
import club.modelo.competencias.Competencia;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.FormatoLiga;
import club.modelo.competencias.InstitucionRival;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.competencias.PosicionEquipo;
import club.modelo.competencias.ReglaFutbol;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Entrenador;

/** Pruebas de integración derivadas de las historias del entrenador (banco oficial). */
public class HistoriasEntrenadorTest {
    private SistemaClub sistema;
    private Administrador admin;
    private Entrenador responsable;
    private Entrenador otroEntrenador;

    @Before
    public void preparar() {
        sistema = new SistemaClub();
        admin = new Administrador("admin", "x", "Admin");
        responsable = new Entrenador("resp", "x", "Responsable", "R1");
        otroEntrenador = new Entrenador("otro", "x", "Otro", "O1");
    }

    // ===================================================================== HU-T09 · Resultado de un partido oficial
    private Competencia liga;
    private Equipo seneca;
    private InstitucionRival rival;

    /** Liga de fútbol con el equipo del club y un rival; el partido queda programado con el entrenador responsable. */
    private PartidoCompetencia partidoProgramado() {
        seneca = Fabrica.equipoConJugadores("Séneca", 8);
        rival = new InstitucionRival("Javeriana");
        sistema.registrarEquipo(seneca);
        liga = admin.crearCompetencia("Copa Andes", "ASCUN", "2026", seneca.getDeporte(), seneca.getCategoria(),
                new FormatoLiga("Liga", new ReglaFutbol(3, 0)), sistema);
        liga.agregarEquipo(seneca);
        java.util.List<club.modelo.usuarios.Socio> nomina = new java.util.ArrayList<>();
        for (club.modelo.competencias.InscripcionEquipo i : seneca.getInscripcionesActivas()) nomina.add(i.getSocio());
        nomina.remove(nomina.size() - 1);   // el último jugador queda fuera de la nómina
        liga.registrarNomina(seneca, nomina);
        liga.agregarEquipo(rival);
        liga.generarCalendario();
        PartidoCompetencia p = liga.getPartidos().get(0);
        admin.programarPartidoOficial(p, LocalDate.of(2026, 12, 5), "Campín", responsable);
        return p;
    }

    private int puntos(Object equipo) {
        for (PosicionEquipo pos : ((FormatoLiga) liga.getFormato()).getPosiciones())
            if (pos.getEquipo() == equipo) return pos.getPuntuacion();
        throw new AssertionError("Equipo sin posición");
    }

    @Test
    public void t09_exitoFutbol_victoriaSumaTresYDerrotaCero() {
        PartidoCompetencia p = partidoProgramado();
        responsable.registrarResultadoOficial(p, 2, 1);
        assertTrue(p.isFinalizado());
        assertEquals(3, puntos(seneca));
        assertEquals(0, puntos(rival));
    }

    @Test
    public void t09_exitoFutbol_empateSumaUnoACadaEquipo() {
        PartidoCompetencia p = partidoProgramado();
        responsable.registrarResultadoOficial(p, 1, 1);
        assertEquals(1, puntos(seneca));
        assertEquals(1, puntos(rival));
    }

    @Test
    public void t09_rechazo_marcadorNegativoOPartidoConResultado() {
        PartidoCompetencia p = partidoProgramado();
        assertThrows(IllegalArgumentException.class, () -> responsable.registrarResultadoOficial(p, -1, 0));
        responsable.registrarResultadoOficial(p, 2, 0);
        assertThrows(IllegalStateException.class, () -> responsable.registrarResultadoOficial(p, 3, 0));
        assertEquals("2 - 0", p.getMarcador());
    }

    @Test
    public void t09_rechazo_entrenadorQueNoEsElResponsable() {
        PartidoCompetencia p = partidoProgramado();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> otroEntrenador.registrarResultadoOficial(p, 2, 1));
        assertTrue(e.getMessage().contains("responsable"));
        assertFalse(p.isFinalizado());
    }

    @Test
    public void t09_rechazo_partidoSinVistoBuenoDelAdministrador() {
        PartidoCompetencia p = partidoProgramado();
        PartidoCompetencia sinProgramar = new PartidoCompetencia(null, "Pendiente", "Por definir");
        sinProgramar.agregarEquipo(seneca);
        sinProgramar.agregarEquipo(new InstitucionRival("Rosario"));
        assertThrows(IllegalStateException.class, () -> responsable.registrarResultadoOficial(sinProgramar, 1, 0));
        assertFalse(sinProgramar.isFinalizado());
        assertFalse(p.isFinalizado());
    }

    // ===================================================================== HU-T05 · Alineación en un partido oficial
    @Test
    public void t05_oficial_soloJugadoresDeLaNomina() {
        PartidoCompetencia p = partidoProgramado();
        java.util.List<club.modelo.usuarios.Socio> todos = new java.util.ArrayList<>();
        for (club.modelo.competencias.InscripcionEquipo i : seneca.getInscripcionesActivas()) todos.add(i.getSocio());
        club.modelo.usuarios.Socio fueraDeNomina = todos.get(todos.size() - 1);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> p.definirAlineacion(seneca, todos));
        assertTrue(e.getMessage(), e.getMessage().contains("nómina"));
        todos.remove(fueraDeNomina);
        p.definirAlineacion(seneca, todos);
        assertEquals(7, p.getAlineacion(seneca).size());
    }

    @Test
    public void t07_oficial_estadisticasSoloParaAlineados() {
        PartidoCompetencia p = partidoProgramado();
        java.util.List<club.modelo.usuarios.Socio> titulares = new java.util.ArrayList<>();
        for (club.modelo.competencias.InscripcionEquipo i : seneca.getInscripcionesActivas()) titulares.add(i.getSocio());
        titulares.remove(titulares.size() - 1);
        p.definirAlineacion(seneca, titulares.subList(0, 7));
        responsable.registrarResultadoOficial(p, 1, 0);
        p.agregarEstadistica(new club.modelo.competencias.EstadisticaJugadorFutbol(titulares.get(0), 1, 0, 0, 0));
        assertEquals(1, p.getEstadisticas().size());
    }
}
