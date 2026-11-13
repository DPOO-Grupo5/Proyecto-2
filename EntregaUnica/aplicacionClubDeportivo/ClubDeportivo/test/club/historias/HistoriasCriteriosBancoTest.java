package club.historias;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.model.SistemaClub;
import club.modelo.competencias.Competencia;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.FormatoLiga;
import club.modelo.competencias.InscripcionEquipo;
import club.modelo.competencias.ReglaFutbol;
import club.modelo.entrenamiento.Tenis;
import club.modelo.instalaciones.Reserva;
import club.modelo.tienda.Bebida;
import club.modelo.tienda.Existencia;
import club.modelo.tienda.ProductoTienda;
import club.modelo.tienda.SugerenciaProducto;
import club.modelo.tienda.Transaccion;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Fisioterapeuta;
import club.modelo.usuarios.Socio;

/** Criterios del banco que no tenían una prueba propia: A03, A06 (competencia), A10, E06, S05 (tenis), S14. */
public class HistoriasCriteriosBancoTest {
    private SistemaClub sistema;
    private Administrador admin;

    @Before
    public void preparar() {
        sistema = new SistemaClub();
        admin = new Administrador("admin", "admin123", "Administrador");
        sistema.registrarUsuario(admin);
    }

    // ---------------------------------------------------------------- HU-A03
    @Test
    public void a03_exito_equipoQuedaActivoYSinJugadores() {
        Equipo e = admin.crearEquipo("Séneca", new club.modelo.competencias.Futbol("Fútbol", 7), Fabrica.abierta(), 15, sistema);
        assertTrue(e.isActivo());
        assertTrue(e.getInscripcionesActivas().isEmpty());
        assertTrue(sistema.getEquipos().contains(e));
    }

    @Test
    public void a03_rechazo_cupoCeroONegativoYDatosFaltantes() {
        club.modelo.competencias.Futbol futbol = new club.modelo.competencias.Futbol("Fútbol", 7);
        assertThrows(IllegalArgumentException.class, () -> admin.crearEquipo("Séneca", futbol, Fabrica.abierta(), 0, sistema));
        assertThrows(IllegalArgumentException.class, () -> admin.crearEquipo("Séneca", futbol, Fabrica.abierta(), -3, sistema));
        assertThrows(RuntimeException.class, () -> admin.crearEquipo(" ", futbol, Fabrica.abierta(), 15, sistema));
        assertThrows(RuntimeException.class, () -> admin.crearEquipo("Séneca", null, Fabrica.abierta(), 15, sistema));
        assertThrows(RuntimeException.class, () -> admin.crearEquipo("Séneca", futbol, null, 15, sistema));
        assertTrue(sistema.getEquipos().isEmpty());
    }

    // ---------------------------------------------------------------- HU-A06 (competencia en curso)
    @Test
    public void a06_rechazo_equipoInscritoEnUnaCompetenciaEnCurso() {
        Equipo e = Fabrica.equipoConJugadores("Séneca", 8);
        sistema.registrarEquipo(e);
        Competencia c = admin.crearCompetencia("Copa", "ASCUN", "2030", e.getDeporte(), e.getCategoria(),
                new FormatoLiga("Liga", new ReglaFutbol(3, 0)), sistema);
        List<Socio> nomina = new ArrayList<>();
        for (InscripcionEquipo i : e.getInscripcionesActivas()) nomina.add(i.getSocio());
        c.inscribirEquipoConNomina(e, nomina);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> admin.darDeBajaEquipo(e, sistema));
        assertTrue(ex.getMessage(), ex.getMessage().contains("Copa"));
        assertTrue(e.isActivo());
        assertEquals(8, e.getInscripcionesActivas().size());
    }

    // ---------------------------------------------------------------- HU-A10 (aprobación)
    @Test
    public void a10_aprobacion_elProductoEntraAlCatalogoConCantidadCero() {
        Entrenador emp = new Entrenador("ent", "x", "Carlos", "E1");
        SugerenciaProducto s = emp.sugerirProducto(new ProductoTienda("Cuerda", 8000, "deporte"), "Para calentamiento");
        admin.resolverSugerencia(s, true, sistema);
        assertEquals("APROBADO", s.getEstado());
        assertTrue(sistema.getProductos().contains(s.getProducto()));
        assertEquals(0, s.getProducto().getCantidadTotal());
        assertThrows(IllegalStateException.class, () -> admin.resolverSugerencia(s, true, sistema));   // ya resuelta
    }

    // ---------------------------------------------------------------- HU-E06
    @Test
    public void e06_rechazo_otraPromocionYaAplicadaNoSeAcumula() {
        Entrenador ent = new Entrenador("ent1", "x", "Entrenador", "ENT15");
        Fisioterapeuta fis = new Fisioterapeuta("fis1", "x", "Fisio", "FIS15");
        List<Empleado> empleados = Arrays.<Empleado>asList(ent, fis);
        ProductoTienda balon = Fabrica.productoTienda("Balón", 50000, 10);
        Transaccion t = ent.iniciarTransaccion(null, "tienda");
        t.agregarDetalle(balon, 2);
        t.aplicarCodigoDescuento("ENT15", empleados);                       // 15 % con su propio código
        assertThrows(IllegalStateException.class, () -> t.aplicarCodigoDescuento("FIS15", empleados));
        assertEquals(15000, t.getDescuento(), 0.001);
    }

    // ---------------------------------------------------------------- HU-S05 (tenis no techado)
    @Test
    public void s05_exito_tenisConBebidaCalienteRecienteSePermite() {
        Tenis tenis = Fabrica.tenis(2);
        Socio socio = Fabrica.socio("uno");
        socio.inscribirseEnDisciplina(tenis, "Principiante");
        Reserva r = socio.reservarInstalacion(tenis.getInstalaciones().get(0), Fabrica.BLOQUE, Fabrica.BLOQUE.plusHours(1), 2, "sencillos");
        Bebida cafe = new Bebida("Café", 4000, true);
        cafe.agregarExistencia(new Existencia(5, "cafeteria"));
        Transaccion t = socio.iniciarTransaccion(null, "cafeteria");
        t.setFechaHora(Fabrica.BLOQUE.minusMinutes(10));
        t.agregarDetalle(cafe, 1);
        t.confirmar();
        r.iniciarUso(socio, Fabrica.BLOQUE.plusMinutes(5));   // no lanza excepción
    }

    // ---------------------------------------------------------------- HU-S14
    private Transaccion cafe(Socio socio) {
        Bebida agua = new Bebida("Agua", 10000, false);
        agua.agregarExistencia(new Existencia(5, "cafeteria"));
        Transaccion t = socio.iniciarTransaccion(null, "cafeteria");
        t.agregarDetalle(agua, 1);
        return t;
    }

    @Test
    public void s14_exito_impuestoAlConsumoOchoPorCientoYPropinaSugeridaDiezPorCiento() {
        Transaccion t = cafe(Fabrica.socio("uno"));
        t.confirmar();
        assertEquals(800, t.getImpuestos(), 0.001);
        assertEquals(1000, t.getPropinaSugerida(), 0.001);
    }

    @Test
    public void s14_propina_seUsaElValorDelSocioIncluidoCero() {
        Transaccion sinPropina = cafe(Fabrica.socio("uno"));
        sinPropina.calcularPropina(0);
        sinPropina.confirmar();
        assertEquals(0, sinPropina.getPropina(), 0.001);
        assertEquals(10800, sinPropina.getTotal(), 0.001);
        Transaccion cincoPorCiento = cafe(Fabrica.socio("dos"));
        cincoPorCiento.calcularPropina(5);
        cincoPorCiento.confirmar();
        assertEquals(500, cincoPorCiento.getPropina(), 0.001);
    }

    @Test
    public void s14_rechazo_bebidaCalienteConReservaActivaEnPadel() {
        club.modelo.entrenamiento.Padel padel = Fabrica.padel();
        Socio socio = Fabrica.socio("uno");
        socio.inscribirseEnDisciplina(padel, "Principiante");
        socio.reservarInstalacion(padel.getInstalaciones().get(0), Fabrica.BLOQUE, Fabrica.BLOQUE.plusHours(1), 4, "dobles");
        Bebida cafe = new Bebida("Café", 4000, true);
        cafe.agregarExistencia(new Existencia(5, "cafeteria"));
        Transaccion t = socio.iniciarTransaccion(null, "cafeteria");
        t.setFechaHora(Fabrica.BLOQUE.plusMinutes(5));
        t.agregarDetalle(cafe, 1);
        assertThrows(IllegalStateException.class, t::confirmar);
        assertEquals(5, cafe.buscarExistencia("cafeteria").getCantidad());
    }

    @Test
    public void s14_rechazo_productoSinExistencia() {
        Bebida agua = new Bebida("Agua", 10000, false);
        agua.agregarExistencia(new Existencia(0, "cafeteria"));
        Transaccion t = Fabrica.socio("uno").iniciarTransaccion(null, "cafeteria");
        t.agregarDetalle(agua, 1);
        assertThrows(IllegalStateException.class, t::confirmar);
    }

    // ---------------------------------------------------------------- Código de descuento guardado (S17/E06)
    @Test
    public void codigoGuardado_seGuardaSoloSiElCodigoExisteYEstaActivo() {
        Entrenador ent = new Entrenador("ent1", "x", "Entrenador", "ENT15");
        List<Empleado> empleados = Arrays.<Empleado>asList(ent);
        Socio socio = Fabrica.socio("uno");
        assertNull(socio.getCodigoGuardado());
        assertThrows(IllegalArgumentException.class, () -> socio.guardarCodigo("XX", empleados));
        assertNull(socio.getCodigoGuardado());
        ent.desactivarCodigo();
        assertThrows(IllegalArgumentException.class, () -> socio.guardarCodigo("ENT15", empleados));
        ent.activarCodigo();
        socio.guardarCodigo("ENT15", empleados);
        assertEquals("ENT15", socio.getCodigoGuardado());
        // El código guardado se aplica en la compra como código compartido (8 %).
        ProductoTienda balon = Fabrica.productoTienda("Balón", 50000, 10);
        Transaccion t = socio.iniciarTransaccion(null, "tienda");
        t.agregarDetalle(balon, 2);
        t.aplicarCodigoDescuento(socio.getCodigoGuardado(), empleados);
        assertEquals(8000, t.getDescuento(), 0.001);
        socio.olvidarCodigoGuardado();
        assertNull(socio.getCodigoGuardado());
    }

    @Test
    public void codigoGuardado_sobreviveAGuardarYCargarElEstado() throws Exception {
        java.nio.file.Path carpeta = java.nio.file.Files.createTempDirectory("seneca-codigo");
        Entrenador ent = new Entrenador("ent1", "x", "Entrenador", "ENT15");
        sistema.registrarUsuario(ent);
        Socio socio = Fabrica.registrar(sistema, Fabrica.socio("uno"));
        socio.guardarCodigo("ENT15", sistema.getEmpleados());
        club.persistencia.ArchivoPlano archivo = new club.persistencia.ArchivoPlano(carpeta.toString());
        archivo.guardarEstado(sistema);
        SistemaClub recargado = archivo.cargarEstado();
        assertEquals("ENT15", ((Socio) recargado.buscarUsuario("uno")).getCodigoGuardado());
    }
}
