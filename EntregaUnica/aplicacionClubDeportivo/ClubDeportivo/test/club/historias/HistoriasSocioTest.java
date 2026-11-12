package club.historias;

import static club.apoyo.Fabrica.BLOQUE;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.model.SistemaClub;
import club.modelo.entrenamiento.InscripcionDisciplina;
import club.modelo.entrenamiento.Padel;
import club.modelo.entrenamiento.Tenis;
import club.modelo.instalaciones.Instalacion;
import club.modelo.instalaciones.Reserva;
import club.modelo.tienda.ProductoTienda;
import club.modelo.tienda.Transaccion;
import club.modelo.usuarios.Socio;

/**
 * Pruebas de integración derivadas de las historias del socio (banco oficial).
 * Cada método indica la historia y el criterio de aceptación que verifica.
 */
public class HistoriasSocioTest {
    private SistemaClub sistema;
    private Socio socio;

    @Before
    public void preparar() {
        sistema = new SistemaClub();
        socio = Fabrica.registrar(sistema, Fabrica.socio("socio1"));
    }

    // ===================================================================== HU-S01 · Iniciar sesión como socio
    @Test
    public void s01_exito_credencialesCorrectasDanAccesoAlSocio() {
        assertSame(socio, sistema.autenticar("socio1", "clave123"));
    }

    @Test
    public void s01_rechazo_loginInexistenteOContrasenaErradaNoRevelanQueFallo() {
        // Ambos casos devuelven exactamente el mismo resultado: no se puede saber cuál dato falló.
        assertNull(sistema.autenticar("socio1", "errada"));
        assertNull(sistema.autenticar("no.existe", "clave123"));
    }

    // ===================================================================== HU-S02 · Inscribirme en una academia individual
    @Test
    public void s02_exito_seCreaConElNivelElegidoYEloInicial1000() {
        Tenis tenis = Fabrica.tenis(2);
        InscripcionDisciplina i = socio.inscribirseEnDisciplina(tenis, "Intermedio");
        assertEquals("Intermedio", i.getNivel());
        assertEquals(1000, i.getRanking());
        assertTrue(tenis.tieneInscripcionActiva(socio));
    }

    @Test
    public void s02_exito_cadaDisciplinaConservaSuPropioElo() {
        Tenis tenis = Fabrica.tenis(2);
        Padel padel = Fabrica.padel();
        InscripcionDisciplina enTenis = socio.inscribirseEnDisciplina(tenis, "Avanzado");
        enTenis.actualizarRanking(true, 1000);
        InscripcionDisciplina enPadel = socio.inscribirseEnDisciplina(padel, "Principiante");
        assertEquals(1000, enPadel.getRanking());
        assertNotEquals(1000, enTenis.getRanking());
        assertEquals(2, socio.getInscripcionesDisciplina().size());
    }

    @Test
    public void s02_rechazo_inscripcionDuplicada() {
        Tenis tenis = Fabrica.tenis(2);
        socio.inscribirseEnDisciplina(tenis, "Principiante");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> socio.inscribirseEnDisciplina(tenis, "Intermedio"));
        assertTrue(e.getMessage().contains("ya está inscrito"));
        assertEquals(1, socio.getInscripcionesDisciplina().size());
    }

    @Test
    public void s02_rechazo_nivelFueraDelConjuntoODisciplinaInexistente() {
        Tenis tenis = Fabrica.tenis(2);
        assertThrows(IllegalArgumentException.class, () -> socio.inscribirseEnDisciplina(tenis, "Experto"));
        assertThrows(IllegalArgumentException.class, () -> socio.inscribirseEnDisciplina(null, "Principiante"));
        assertTrue(socio.getInscripcionesDisciplina().isEmpty());
        assertFalse(tenis.tieneInscripcionActiva(socio));
    }

    // ===================================================================== HU-S03 · Reservar una instalación
    @Test
    public void s03_exito_reservaQuedaActivaYAsociadaASuCuenta() {
        Tenis tenis = Fabrica.tenis(2);
        socio.inscribirseEnDisciplina(tenis, "Principiante");
        Instalacion cancha = tenis.getInstalaciones().get(0);

        Reserva r = socio.reservarInstalacion(cancha, BLOQUE, BLOQUE.plusHours(1), 2, "sencillos");

        assertTrue(socio.getReservas().contains(r));
        assertTrue(cancha.getReservas().contains(r));
        assertSame(socio, r.getSocioResponsable());
    }

    @Test
    public void s03_rechazoPorCupo_disciplinaConMaximoDeInstalacionesSimultaneas() {
        Tenis tenis = Fabrica.tenis(1);
        Socio otro = Fabrica.registrar(sistema, Fabrica.socio("socio2"));
        socio.inscribirseEnDisciplina(tenis, "Principiante");
        otro.inscribirseEnDisciplina(tenis, "Principiante");
        socio.reservarInstalacion(tenis.getInstalaciones().get(0), BLOQUE, BLOQUE.plusHours(1), 2, "sencillos");

        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                otro.reservarInstalacion(tenis.getInstalaciones().get(1), BLOQUE, BLOQUE.plusHours(1), 2, "sencillos"));
        assertTrue(e.getMessage().contains("máximo de instalaciones simultáneas"));
        assertTrue(otro.getReservas().isEmpty());
    }

    @Test
    public void s03_rechazoPorCapacidad_padelExigeCuatroJugadores() {
        Padel padel = Fabrica.padel();
        socio.inscribirseEnDisciplina(padel, "Principiante");
        Instalacion cancha = padel.getInstalaciones().get(0);
        assertThrows(IllegalArgumentException.class, () -> socio.reservarInstalacion(cancha, BLOQUE, BLOQUE.plusHours(1), 2, "dobles"));
        assertThrows(IllegalArgumentException.class, () -> socio.reservarInstalacion(cancha, BLOQUE, BLOQUE.plusHours(1), 4, "sencillos"));
        assertTrue(cancha.getReservas().isEmpty());
    }

    @Test
    public void s03_rechazoPorCapacidad_tenisSencillosDosYDoblesCuatro() {
        Tenis tenis = Fabrica.tenis(2);
        socio.inscribirseEnDisciplina(tenis, "Principiante");
        Instalacion cancha = tenis.getInstalaciones().get(0);
        assertThrows(IllegalArgumentException.class, () -> socio.reservarInstalacion(cancha, BLOQUE, BLOQUE.plusHours(1), 3, "sencillos"));
        assertThrows(IllegalArgumentException.class, () -> socio.reservarInstalacion(cancha, BLOQUE, BLOQUE.plusHours(1), 3, "dobles"));
        assertTrue(cancha.getReservas().isEmpty());
    }

    @Test
    public void s03_rechazoPorTraslape_bloqueQueSeCruzaConOtraReserva() {
        Tenis tenis = Fabrica.tenis(2);
        Socio otro = Fabrica.registrar(sistema, Fabrica.socio("socio2"));
        socio.inscribirseEnDisciplina(tenis, "Principiante");
        otro.inscribirseEnDisciplina(tenis, "Principiante");
        Instalacion cancha = tenis.getInstalaciones().get(0);
        socio.reservarInstalacion(cancha, BLOQUE, BLOQUE.plusHours(1), 2, "sencillos");

        assertThrows(IllegalStateException.class, () ->
                otro.reservarInstalacion(cancha, BLOQUE.plusMinutes(30), BLOQUE.plusMinutes(90), 2, "sencillos"));
        assertEquals(1, cancha.getReservas().size());
    }

    @Test
    public void s03_rechazoPorInscripcion_socioNoInscritoEnLaDisciplina() {
        Tenis tenis = Fabrica.tenis(2);
        Instalacion cancha = tenis.getInstalaciones().get(0);
        assertThrows(IllegalStateException.class, () -> socio.reservarInstalacion(cancha, BLOQUE, BLOQUE.plusHours(1), 2, "sencillos"));
        assertTrue(socio.getReservas().isEmpty());
    }

    // ===================================================================== HU-S13 · Comprar en la tienda
    @Test
    public void s13_exito_variosArticulosConIvaYDescuentoDeExistencias() {
        ProductoTienda balon = Fabrica.productoTienda("Balón", 50000, 5);
        ProductoTienda gorra = Fabrica.productoTienda("Gorra", 20000, 3);
        Transaccion compra = socio.iniciarTransaccion(null, "tienda");
        compra.agregarDetalle(balon, 2);
        compra.agregarDetalle(gorra, 1);

        compra.confirmar();

        assertTrue(compra.isConfirmada());
        assertEquals("tienda", compra.getRubro());
        assertNotNull(compra.getFechaHora());
        assertEquals(120000, compra.getSubtotal(), 0.001);
        assertEquals(22800, compra.getImpuestos(), 0.001);
        assertEquals(142800, compra.getTotal(), 0.001);
        assertEquals(3, balon.getCantidadTotal());
        assertEquals(2, gorra.getCantidadTotal());
    }

    @Test
    public void s13_rechazo_faltaExistenciaDeUnArticuloYNoCambiaNingunInventario() {
        ProductoTienda balon = Fabrica.productoTienda("Balón", 50000, 5);
        ProductoTienda gorra = Fabrica.productoTienda("Gorra", 20000, 1);
        Transaccion compra = socio.iniciarTransaccion(null, "tienda");
        compra.agregarDetalle(balon, 2);
        compra.agregarDetalle(gorra, 2);

        IllegalStateException e = assertThrows(IllegalStateException.class, compra::confirmar);

        assertTrue("El mensaje debe indicar el artículo", e.getMessage().contains("Gorra"));
        assertFalse(compra.isConfirmada());
        assertEquals(5, balon.getCantidadTotal());
        assertEquals(1, gorra.getCantidadTotal());
        assertEquals(0, socio.getPuntosFidelidad());
    }

    @Test
    public void s13_rechazo_cantidadMenorOIgualACeroOArticuloInexistente() {
        ProductoTienda balon = Fabrica.productoTienda("Balón", 50000, 5);
        Transaccion compra = socio.iniciarTransaccion(null, "tienda");
        assertThrows(IllegalArgumentException.class, () -> compra.agregarDetalle(balon, 0));
        assertThrows(IllegalArgumentException.class, () -> compra.agregarDetalle(balon, -1));
        assertThrows(IllegalArgumentException.class, () -> compra.agregarDetalle(null, 1));
        assertTrue(compra.getDetalles().isEmpty());
    }
}
