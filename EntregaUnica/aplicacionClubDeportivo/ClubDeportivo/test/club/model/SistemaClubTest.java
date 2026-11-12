package club.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.DeporteConjunto;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.Futbol;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Fisioterapeuta;
import club.modelo.usuarios.Socio;

/** Pruebas unitarias del registro central del club. */
public class SistemaClubTest {
    private SistemaClub sistema;
    private Socio socio;

    @Before
    public void preparar() {
        sistema = new SistemaClub();
        socio = Fabrica.registrar(sistema, Fabrica.socio("ana"));
    }

    @Test
    public void autenticaConCredencialesCorrectasSinDistinguirMayusculasEnElLogin() {
        assertSame(socio, sistema.autenticar("ANA", "clave123"));
    }

    @Test
    public void rechazaContrasenaIncorrectaYLoginInexistenteDeLaMismaForma() {
        assertNull(sistema.autenticar("ana", "otra"));
        assertNull(sistema.autenticar("nadie", "clave123"));
    }

    @Test
    public void guardaLaContrasenaComoHashYNoEnTextoPlano() {
        assertNotEquals("clave123", socio.getPassword());
        assertTrue(socio.getPassword().startsWith("pbkdf2$"));
    }

    @Test
    public void rechazaUnLoginRepetidoAunqueCambienLasMayusculas() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> sistema.registrarUsuario(Fabrica.socio("ANA")));
        assertTrue(e.getMessage().contains("login"));
        assertEquals(1, sistema.getUsuarios().size());
    }

    @Test
    public void filtraLosUsuariosPorTipo() {
        sistema.registrarUsuario(new Entrenador("e1", "x", "Entrenador", "C1"));
        sistema.registrarUsuario(new Fisioterapeuta("f1", "x", "Fisio", "C2"));
        sistema.registrarUsuario(new Administrador("adm", "x", "Admin"));
        assertEquals(1, sistema.getSocios().size());
        assertEquals(2, sistema.getEmpleados().size());
        assertEquals(1, sistema.getEntrenadores().size());
    }

    @Test
    public void reutilizaElDeporteYLaCategoriaDeLosEquiposExistentes() {
        Equipo equipo = new Equipo("Séneca FC", new Futbol("Fútbol", 7), new Categoria("Sub-15", 13, 14), 15);
        sistema.registrarEquipo(equipo);
        assertSame(equipo.getDeporte(), sistema.obtenerDeporte("futbol"));
        assertSame(equipo.getCategoria(), sistema.obtenerCategoria("sub-15"));
    }

    @Test
    public void creaDeportesConElMinimoDeJugadoresDelBanco() {
        DeporteConjunto baloncesto = sistema.obtenerDeporte("Baloncesto");
        DeporteConjunto voleibol = sistema.obtenerDeporte("Voleibol");
        assertEquals(7, sistema.obtenerDeporte("Fútbol").getMinJugadores());
        assertEquals(5, baloncesto.getMinJugadores());
        assertEquals(6, voleibol.getMinJugadores());
        assertThrows(IllegalArgumentException.class, () -> sistema.obtenerDeporte("Ajedrez"));
    }

    @Test
    public void listaSoloLosEquiposActivos() {
        Equipo activo = Fabrica.equipoFutbol("Activo", Fabrica.abierta(), 15);
        Equipo deBaja = Fabrica.equipoFutbol("De baja", Fabrica.abierta(), 15);
        sistema.registrarEquipo(activo);
        sistema.registrarEquipo(deBaja);
        deBaja.darDeBaja();
        assertEquals(1, sistema.getEquiposActivos().size());
        assertSame(activo, sistema.getEquiposActivos().get(0));
    }
}
