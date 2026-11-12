package club.historias;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.modelo.tienda.Bebida;
import club.modelo.tienda.Existencia;
import club.modelo.tienda.ProductoTienda;
import club.modelo.tienda.TipoPromocion;
import club.modelo.tienda.Transaccion;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Fisioterapeuta;
import club.modelo.usuarios.Socio;

/**
 * Historias de compras y beneficios: HU-S15, HU-S16, HU-S17 y HU-E06.
 * Regla del grupo (Proyecto 2): una compra admite una sola promoción (código de empleado, puntos o bono de torneo).
 */
public class HistoriasComprasTest {
    private Socio socio;
    private Entrenador entrenador;
    private Fisioterapeuta fisio;
    private List<Empleado> empleados;
    private ProductoTienda balon;

    @Before
    public void preparar() {
        socio = Fabrica.socio("socio1");
        entrenador = new Entrenador("ent1", "clave", "Entrenador", "ENT15");
        fisio = new Fisioterapeuta("fis1", "clave", "Fisio", "FIS15");
        empleados = Arrays.asList(entrenador, fisio);
        balon = Fabrica.productoTienda("Balón", 50000, 10);
    }

    private Transaccion compraDeDosBalones(club.modelo.usuarios.Usuario comprador) {
        Transaccion t = comprador.iniciarTransaccion(null, "tienda");
        t.agregarDetalle(balon, 2);
        return t;
    }

    // ===================================================================== HU-S15 · Acumular puntos
    @Test
    public void s15_exito_sumaElDosPorCientoDelTotalPagadoTruncado() {
        ProductoTienda gorra = Fabrica.productoTienda("Gorra", 9235, 5);   // total 10.989,65 → 2 % = 219,79
        Transaccion t = socio.iniciarTransaccion(null, "tienda");
        t.agregarDetalle(gorra, 1);
        t.confirmar();
        assertEquals(219, t.getPuntosFidelidad());
        assertEquals(219, socio.getPuntosFidelidad());
    }

    @Test
    public void s15_exito_laPropinaNoGeneraPuntos() {
        Bebida agua = new Bebida("Agua", 10000, false);
        agua.agregarExistencia(new Existencia(5, "cafeteria"));
        Transaccion t = socio.iniciarTransaccion(null, "cafeteria");
        t.agregarDetalle(agua, 1);
        t.calcularPropina(10);                 // propina 1.000 sobre 10.000
        t.confirmar();
        assertEquals(11800, t.getTotal(), 0.001);   // 10.000 + 8 % + 1.000
        assertEquals(216, t.getPuntosFidelidad());  // 2 % de 10.800, sin propina
    }

    @Test
    public void s15_rechazo_compraRechazadaNoCambiaElSaldo() {
        ProductoTienda escaso = Fabrica.productoTienda("Escaso", 1000, 0);
        Transaccion t = socio.iniciarTransaccion(null, "tienda");
        t.agregarDetalle(escaso, 1);
        assertThrows(IllegalStateException.class, t::confirmar);
        assertEquals(0, socio.getPuntosFidelidad());
    }

    // ===================================================================== HU-S16 · Redimir puntos
    @Test
    public void s16_exito_cadaPuntoDescuentaUnPesoDelTotal() {
        socio.acumularPuntos(1000);
        Transaccion t = compraDeDosBalones(socio);
        t.redimirPuntos(500);
        t.confirmar();
        assertEquals(118500, t.getTotal(), 0.001);              // 119.000 − 500
        assertEquals(500, t.getPuntosRedimidos());
        assertEquals(1000 - 500 + 2370, socio.getPuntosFidelidad());   // gana el 2 % de 118.500
        assertEquals(TipoPromocion.PUNTOS, t.getPromocion());
    }

    @Test
    public void s16_rechazo_masPuntosQueElSaldoInformaElSaldoDisponible() {
        socio.acumularPuntos(300);
        Transaccion t = compraDeDosBalones(socio);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> t.redimirPuntos(301));
        assertTrue(e.getMessage(), e.getMessage().contains("300"));
        assertEquals(0, t.getPuntosRedimidos());
        assertNull(t.getPromocion());
    }

    @Test
    public void s16_rechazo_masPuntosQueElTotalDeLaCompra() {
        socio.acumularPuntos(500000);
        Transaccion t = compraDeDosBalones(socio);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> t.redimirPuntos(119001));
        assertTrue(e.getMessage(), e.getMessage().contains("500000"));
        t.redimirPuntos(119000);   // exactamente el total sí se permite
        t.confirmar();
        assertEquals(0, t.getTotal(), 0.001);
    }

    // ===================================================================== HU-S17 · Código de un empleado (socio)
    @Test
    public void s17_exito_ochoPorCientoSobreElSubtotalAntesDeImpuestos() {
        Transaccion t = compraDeDosBalones(socio);
        t.aplicarCodigoDescuento("ENT15", empleados);
        t.confirmar();
        assertEquals(8000, t.getDescuento(), 0.001);
        assertEquals(17480, t.getImpuestos(), 0.001);      // 19 % de 92.000
        assertEquals(109480, t.getTotal(), 0.001);
        assertEquals(TipoPromocion.CODIGO_COMPARTIDO, t.getPromocion());
    }

    @Test
    public void s17_exito_tambienEnLaCafeteria() {
        Bebida agua = new Bebida("Agua", 10000, false);
        agua.agregarExistencia(new Existencia(5, "cafeteria"));
        Transaccion t = socio.iniciarTransaccion(null, "cafeteria");
        t.agregarDetalle(agua, 1);
        t.aplicarCodigoDescuento("FIS15", empleados);
        t.calcularPropina(0);
        t.confirmar();
        assertEquals(800, t.getDescuento(), 0.001);
        assertEquals(9936, t.getTotal(), 0.001);           // 9.200 + 8 % de consumo
    }

    @Test
    public void s17_rechazo_codigoInexistenteOInactivo() {
        Transaccion t = compraDeDosBalones(socio);
        assertThrows(IllegalArgumentException.class, () -> t.aplicarCodigoDescuento("NOEXISTE", empleados));
        fisio.desactivarCodigo();
        assertThrows(IllegalArgumentException.class, () -> t.aplicarCodigoDescuento("FIS15", empleados));
        assertNull(t.getPromocion());
        assertEquals(0, t.getDescuento(), 0.001);
    }

    @Test
    public void s17_rechazo_noSeAcumulaConOtraPromocion() {
        socio.acumularPuntos(1000);
        Transaccion conPuntos = compraDeDosBalones(socio);
        conPuntos.redimirPuntos(100);
        assertThrows(IllegalStateException.class, () -> conPuntos.aplicarCodigoDescuento("ENT15", empleados));

        Transaccion conCodigo = compraDeDosBalones(socio);
        conCodigo.aplicarCodigoDescuento("ENT15", empleados);
        assertThrows(IllegalStateException.class, () -> conCodigo.redimirPuntos(100));
        assertThrows(IllegalStateException.class, () -> conCodigo.aplicarCodigoDescuento("FIS15", empleados));
        assertEquals(8000, conCodigo.getDescuento(), 0.001);
    }

    // ===================================================================== HU-E06 · Comprar con mi código de empleado
    @Test
    public void e06_exito_codigoPropioDaQuincePorCientoYGeneraPuntos() {
        Transaccion t = compraDeDosBalones(entrenador);
        t.aplicarCodigoDescuento("ENT15", empleados);
        t.confirmar();
        assertEquals(15000, t.getDescuento(), 0.001);
        assertEquals(101150, t.getTotal(), 0.001);          // 85.000 + 19 %
        assertEquals(2023, entrenador.getPuntosFidelidad()); // 2 % de 101.150, truncado
        assertEquals(TipoPromocion.CODIGO_PROPIO, t.getPromocion());
    }

    @Test
    public void e06_exito_codigoDeOtroEmpleadoDaOchoPorCiento() {
        Transaccion t = compraDeDosBalones(entrenador);
        t.aplicarCodigoDescuento("FIS15", empleados);
        assertEquals(8000, t.getDescuento(), 0.001);
        assertEquals(TipoPromocion.CODIGO_COMPARTIDO, t.getPromocion());
    }

    @Test
    public void e06_rechazo_codigoInexistente() {
        Transaccion t = compraDeDosBalones(entrenador);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> t.aplicarCodigoDescuento("XX", empleados));
        assertTrue(e.getMessage().contains("código"));
    }
}
