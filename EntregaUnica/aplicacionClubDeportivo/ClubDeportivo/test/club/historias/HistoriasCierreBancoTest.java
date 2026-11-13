package club.historias;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import club.apoyo.Fabrica;
import club.model.Reloj;
import club.model.SistemaClub;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.Futbol;
import club.modelo.competencias.PartidoInterno;
import club.modelo.entrenamiento.InscripcionDisciplina;
import club.modelo.entrenamiento.Tenis;
import club.modelo.instalaciones.Reserva;
import club.modelo.tienda.Bebida;
import club.modelo.tienda.Existencia;
import club.modelo.tienda.ProductoTienda;
import club.modelo.tienda.SugerenciaProducto;
import club.modelo.tienda.Snack;
import club.modelo.tienda.Transaccion;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;

/** Pruebas de las historias que estaban parciales en la matriz (S05, S06, S10, S14, E05, A02, A03, A06-A10, A13, A14, A15). */
public class HistoriasCierreBancoTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2030, 3, 1, 9, 0);
    private SistemaClub sistema;
    private Administrador admin;

    @Before
    public void preparar() {
        Reloj.fijar(AHORA);
        sistema = new SistemaClub();
        admin = new Administrador("admin", "admin123", "Administrador");
        sistema.registrarUsuario(admin);
    }

    @After
    public void restablecerReloj() { Reloj.usarRelojDelSistema(); }

    // ---------------------------------------------------------------- HU-A02
    @Test
    public void a02_rechazo_documentoRepetido() {
        sistema.registrarUsuario(Fabrica.socio("uno"));
        Socio otro = Fabrica.socio("dos");
        otro.setDocumento("DOC-uno");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sistema.registrarUsuario(otro));
        assertTrue(e.getMessage().contains("documento"));
        assertNull(sistema.buscarUsuario("dos"));
    }

    @Test
    public void a02_rechazo_documentoFaltante() {
        Socio sinDoc = new Socio("tres", "clave123", "Tres", LocalDate.of(2000, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> sistema.registrarUsuario(sinDoc));
        assertThrows(IllegalArgumentException.class, () -> sinDoc.setDocumento("  "));
    }

    @Test
    public void a02_completarDatosFaltantes_noDejaSociosSinDocumento() {
        sistema.registrarUsuario(Fabrica.socio("uno"));
        sistema.completarDatosFaltantes();
        assertEquals("DOC-uno", ((Socio) sistema.buscarUsuario("uno")).getDocumento());
    }

    // ---------------------------------------------------------------- HU-A03
    @Test
    public void a03_rechazo_cupoMenorAlMinimoDelDeporte() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> admin.crearEquipo("Corto", new Futbol("Fútbol", 7), new Categoria("Abierta", 0, 150), 6, sistema));
        assertTrue(e.getMessage().contains("7"));
        assertTrue(sistema.getEquipos().isEmpty());
    }

    @Test
    public void a03_duplicado_seValidaPorNombreDeporteYCategoria() {
        Futbol futbol = new Futbol("Fútbol", 7);
        Categoria abierta = new Categoria("Abierta", 0, 150), sub15 = new Categoria("Sub-15", 13, 14);
        admin.crearEquipo("Séneca", futbol, abierta, 10, sistema);
        assertThrows(IllegalArgumentException.class, () -> admin.crearEquipo("SÉNECA", futbol, abierta, 10, sistema));
        // Mismo nombre pero otra categoría: se acepta.
        admin.crearEquipo("Séneca", futbol, sub15, 10, sistema);
        assertEquals(2, sistema.getEquipos().size());
    }

    // ---------------------------------------------------------------- HU-A06
    @Test
    public void a06_exito_liberaIntegrantesYConservaElHistorial() {
        Equipo e = Fabrica.equipoConJugadores("Equipo A", 8);
        Socio jugador = e.getInscripcionesActivas().get(0).getSocio();
        admin.darDeBajaEquipo(e, sistema);
        assertFalse(e.isActivo());
        assertTrue(e.getInscripcionesActivas().isEmpty());
        assertEquals(1, jugador.getInscripcionesEquipo().size());
        assertFalse(jugador.getInscripcionesEquipo().get(0).isActiva());
    }

    @Test
    public void a06_rechazo_equipoYaInactivo() {
        Equipo e = Fabrica.equipoConJugadores("Equipo A", 8);
        admin.darDeBajaEquipo(e, sistema);
        assertThrows(IllegalStateException.class, () -> admin.darDeBajaEquipo(e, sistema));
    }

    @Test
    public void a06_rechazo_equipoConPartidoPendiente() {
        Futbol futbol = new Futbol("Fútbol", 7);
        Categoria mayores = new Categoria("Mayores", 18, 150);
        Equipo a = Fabrica.equipoConJugadores("Equipo A", 8, futbol, mayores), b = Fabrica.equipoConJugadores("Equipo B", 8, futbol, mayores);
        new Entrenador("ent", "x", "Ent", "E1").programarPartidoInterno(a, b, AHORA.plusDays(2));
        assertThrows(IllegalStateException.class, () -> admin.darDeBajaEquipo(a, sistema));
        assertTrue(a.isActivo());
        assertEquals(8, a.getInscripcionesActivas().size());
    }

    // ---------------------------------------------------------------- HU-A07, A08, A09
    private ProductoTienda productoEn(String ubicacion, int cantidad) {
        ProductoTienda p = new ProductoTienda("Balón", 50000, "deporte");
        p.agregarExistencia(new Existencia(cantidad, ubicacion));
        sistema.registrarProducto(p);
        return p;
    }

    @Test
    public void a07_exito_muestraTotalPorArticuloYFiltraPorUbicacion() {
        ProductoTienda p = productoEn("tienda", 5);
        p.agregarExistencia(new Existencia(3, "bodega"));
        String todo = admin.estadoInventario(sistema, null);
        assertTrue(todo.contains("total: 8"));
        assertFalse("La disciplina no debe imprimirse como objeto", todo.contains("@"));
        String soloBodega = admin.estadoInventario(sistema, "bodega");
        assertTrue(soloBodega.contains("bodega: 3"));
        assertFalse(soloBodega.contains("tienda: 5"));
    }

    @Test
    public void a07_rechazo_ubicacionInexistente() {
        productoEn("tienda", 5);
        assertThrows(IllegalArgumentException.class, () -> admin.estadoInventario(sistema, "luna"));
    }

    @Test
    public void a08_exito_mueveEntreUbicaciones() {
        ProductoTienda p = productoEn("tienda", 5);
        admin.moverInventario(p, "tienda", "bodega", 2);
        assertEquals(3, p.buscarExistencia("tienda").getCantidad());
        assertEquals(2, p.buscarExistencia("bodega").getCantidad());
    }

    @Test
    public void a08_rechazo_ubicacionInexistenteExistenciaInsuficienteYOrigenIgualDestino() {
        ProductoTienda p = productoEn("tienda", 5);
        assertThrows(IllegalArgumentException.class, () -> admin.moverInventario(p, "tienda", "luna", 1));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> admin.moverInventario(p, "tienda", "bodega", 9));
        assertTrue(e.getMessage().contains("5"));
        assertThrows(IllegalArgumentException.class, () -> admin.moverInventario(p, "tienda", "tienda", 1));
        assertEquals(5, p.buscarExistencia("tienda").getCantidad());
        assertNull(p.buscarExistencia("bodega"));
    }

    @Test
    public void a09_exito_yRechazos() {
        ProductoTienda p = productoEn("tienda", 5);
        admin.reabastecer(p, "tienda", 4);
        assertEquals(9, p.buscarExistencia("tienda").getCantidad());
        assertThrows(IllegalArgumentException.class, () -> admin.reabastecer(p, "tienda", 0));
        assertThrows(IllegalArgumentException.class, () -> admin.reabastecer(p, "luna", 3));
        assertNull(p.buscarExistencia("luna"));
    }

    // ---------------------------------------------------------------- HU-E05 y A10
    @Test
    public void e05_exito_quedaPendienteConAutorYDescripcion() {
        Entrenador emp = new Entrenador("ent", "x", "Carlos", "E1");
        SugerenciaProducto s = emp.sugerirProducto(new ProductoTienda("Cuerda", 8000, "deporte"), "Para calentamiento");
        assertEquals("Pendiente", s.getEstado());
        assertEquals("Carlos", s.getAutor());
        assertEquals("Para calentamiento", s.getDescripcion());
    }

    @Test
    public void e05_rechazo_descripcionVaciaONombreVacio() {
        Entrenador emp = new Entrenador("ent", "x", "Carlos", "E1");
        assertThrows(IllegalArgumentException.class, () -> emp.sugerirProducto(new ProductoTienda("Cuerda", 8000, "deporte"), " "));
        assertThrows(IllegalArgumentException.class, () -> emp.sugerirProducto(null, "algo"));
    }

    @Test
    public void a10_rechazo_exigeMotivoYNoSePuedeResolverDosVeces() {
        Entrenador emp = new Entrenador("ent", "x", "Carlos", "E1");
        SugerenciaProducto s = emp.sugerirProducto(new ProductoTienda("Cuerda", 8000, "deporte"), "Para calentamiento");
        assertThrows(IllegalArgumentException.class, () -> admin.rechazarSugerencia(s, ""));
        assertEquals("Pendiente", s.getEstado());
        admin.rechazarSugerencia(s, "Ya existe uno similar");
        assertEquals("Ya existe uno similar", s.getMotivoRechazo());
        assertThrows(IllegalStateException.class, () -> admin.rechazarSugerencia(s, "otro motivo"));
    }

    // ---------------------------------------------------------------- HU-S14
    @Test
    public void s14_rechazo_snackSinAlergenosRegistrados() {
        Snack barra = new Snack("Barra", 3000, Collections.<String>emptyList());
        barra.agregarExistencia(new Existencia(5, "cafeteria"));
        Socio socio = Fabrica.socio("uno");
        Transaccion t = socio.iniciarTransaccion(null, "cafeteria");
        t.agregarDetalle(barra, 1);
        assertThrows(IllegalStateException.class, t::confirmar);
        assertEquals(5, barra.buscarExistencia("cafeteria").getCantidad());
    }

    @Test
    public void s14_exito_snackConAlergenosGuardaLaListaRecibida() {
        Snack barra = new Snack("Barra", 3000, Arrays.asList("Maní"));
        barra.agregarExistencia(new Existencia(5, "cafeteria"));
        assertEquals(Arrays.asList("Maní"), barra.getAlergenos());
        Socio socio = Fabrica.socio("uno");
        Transaccion t = socio.iniciarTransaccion(null, "cafeteria");
        t.agregarDetalle(barra, 1);
        t.confirmar();
        assertTrue(t.isConfirmada());
        assertEquals(Arrays.asList("Maní"), t.consultarAlergenos());
    }

    // ---------------------------------------------------------------- HU-S05
    private Reserva reservaDePadel(Socio socio) {
        club.modelo.entrenamiento.Padel padel = Fabrica.padel();
        socio.inscribirseEnDisciplina(padel, "Principiante");
        return socio.reservarInstalacion(padel.getInstalaciones().get(0), Fabrica.BLOQUE, Fabrica.BLOQUE.plusHours(1), 4, "dobles");
    }

    @Test
    public void s05_exito_padelSinBebidaCalientePermiteIniciarElUso() {
        Socio socio = Fabrica.socio("uno");
        Reserva r = reservaDePadel(socio);
        r.iniciarUso(socio, Fabrica.BLOQUE.plusMinutes(5));
    }

    @Test
    public void s05_rechazo_padelConBebidaCalienteReciente() {
        Socio socio = Fabrica.socio("uno");
        Reserva r = reservaDePadel(socio);
        Bebida cafe = new Bebida("Café", 4000, true);
        cafe.agregarExistencia(new Existencia(5, "cafeteria"));
        Transaccion t = socio.iniciarTransaccion(null, "cafeteria");
        t.setFechaHora(Fabrica.BLOQUE.minusMinutes(10));
        t.agregarDetalle(cafe, 1);
        t.confirmar();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> r.iniciarUso(socio, Fabrica.BLOQUE.plusMinutes(5)));
        assertTrue(e.getMessage().contains("bebida"));
    }

    @Test
    public void s05_rechazo_socioAjenoALaReserva() {
        Socio socio = Fabrica.socio("uno");
        Reserva r = reservaDePadel(socio);
        assertThrows(IllegalArgumentException.class, () -> r.iniciarUso(Fabrica.socio("otro"), Fabrica.BLOQUE.plusMinutes(5)));
    }

    // ---------------------------------------------------------------- HU-S06
    @Test
    public void s06_posicionEnElRankingYRechazoPorNoInscrito() {
        Tenis tenis = Fabrica.tenis(2);
        Socio a = Fabrica.registrar(sistema, Fabrica.socio("a")), b = Fabrica.registrar(sistema, Fabrica.socio("b"));
        Socio c = Fabrica.registrar(sistema, Fabrica.socio("c"));
        InscripcionDisciplina ia = a.inscribirseEnDisciplina(tenis, "Principiante");
        b.inscribirseEnDisciplina(tenis, "Principiante");
        assertFalse(a.tienePartidosEn(tenis));
        assertEquals(1000, ia.getRanking());
        ia.actualizarRanking(true, 1000);
        assertEquals(1, sistema.posicionRanking(tenis, a));
        assertEquals(2, sistema.posicionRanking(tenis, b));
        assertThrows(IllegalStateException.class, () -> sistema.posicionRanking(tenis, c));
    }

    // ---------------------------------------------------------------- HU-S10
    @Test
    public void s10_sinPartidosDevuelveCeros() {
        Equipo e = Fabrica.equipoConJugadores("Equipo A", 8);
        assertArrayEquals(new int[6], e.resumenResultados());
    }

    @Test
    public void s10_resumenConVictoria() {
        Futbol futbol = new Futbol("Fútbol", 7);
        Categoria mayores = new Categoria("Mayores", 18, 150);
        Equipo a = Fabrica.equipoConJugadores("Equipo A", 8, futbol, mayores), b = Fabrica.equipoConJugadores("Equipo B", 8, futbol, mayores);
        Entrenador ent = new Entrenador("ent", "x", "Ent", "E1");
        PartidoInterno p = ent.programarPartidoInterno(a, b, AHORA.plusDays(1));
        java.util.List<Socio> la = new java.util.ArrayList<>(), lb = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) { la.add(a.getInscripcionesActivas().get(i).getSocio()); lb.add(b.getInscripcionesActivas().get(i).getSocio()); }
        p.definirAlineacion(a, la);
        p.definirAlineacion(b, lb);
        Reloj.fijar(AHORA.plusDays(2));
        ent.registrarResultadoInterno(p, 3, 1, new java.util.ArrayList<club.modelo.competencias.EstadisticaJugadorPartido>());
        assertArrayEquals(new int[] { 1, 1, 0, 0, 3, 1 }, a.resumenResultados());
        assertArrayEquals(new int[] { 1, 0, 0, 1, 1, 3 }, b.resumenResultados());
    }

    // ---------------------------------------------------------------- HU-A13
    @Test
    public void a13_rechazo_rangoInvertido() {
        assertThrows(IllegalArgumentException.class,
                () -> admin.historialPracticas(sistema, LocalDate.of(2030, 2, 1), LocalDate.of(2030, 1, 1), null));
    }

    @Test
    public void a13_vacio_periodoSinRegistros() {
        assertTrue(admin.historialPracticas(sistema, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31), null).isEmpty());
    }

    // ---------------------------------------------------------------- HU-A14
    @Test
    public void a14_rechazo_rubroOPeriodoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> admin.reporteVentasDetallado(Collections.<Transaccion>emptyList(), "gimnasio", "diario", AHORA.toLocalDate()));
        assertThrows(IllegalArgumentException.class, () -> admin.reporteVentasDetallado(Collections.<Transaccion>emptyList(), "tienda", "anual", AHORA.toLocalDate()));
    }

    @Test
    public void a14_sinDatos_yDesgloseDeTienda() {
        assertEquals("No hay datos para ese periodo.", admin.reporteVentasDetallado(Collections.<Transaccion>emptyList(), "tienda", "diario", AHORA.toLocalDate()));
        Socio socio = Fabrica.socio("uno");
        Transaccion t = socio.iniciarTransaccion(null, "tienda");
        t.setFechaHora(AHORA);
        t.agregarDetalle(Fabrica.productoTienda("Gorra", 10000, 5), 2);
        t.confirmar();
        String reporte = admin.reporteVentasDetallado(Arrays.asList(t), "tienda", "diario", AHORA.toLocalDate());
        assertTrue(reporte, reporte.contains("Ventas: 1"));
        assertTrue(reporte, reporte.contains("Neto: 20000"));
        assertTrue(reporte, reporte.contains("IVA: 3800"));
    }

    // ---------------------------------------------------------------- HU-A15
    @Test
    public void a15_historialDeEloGuardaUnaEntradaPorCambioSinDuplicados() {
        Socio a = Fabrica.socio("a");
        InscripcionDisciplina ia = a.inscribirseEnDisciplina(Fabrica.tenis(2), "Principiante");
        assertEquals(1, ia.getHistorialRanking().size());          // valor inicial (ELO 1000)
        ia.actualizarRanking(true, 1000);
        ia.actualizarRanking(false, 1000);
        assertEquals(3, ia.getHistorialRanking().size());
    }
}
