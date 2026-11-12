package club.persistencia;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import club.model.SistemaClub;
import club.modelo.competencias.Equipo;
import club.modelo.tienda.Producto;
import club.modelo.tienda.TipoPromocion;
import club.modelo.tienda.Transaccion;
import club.modelo.usuarios.Socio;

/**
 * Ciclo completo de persistencia: crear y modificar información, guardarla, cargarla en un sistema nuevo,
 * comprobar que se conservan los datos y las relaciones, y seguir operando sobre el estado recuperado.
 */
public class ArchivoPlanoTest {
    private File carpeta;
    private ArchivoPlano archivo;

    @Before
    public void preparar() throws Exception {
        carpeta = Files.createTempDirectory("seneca-prueba-").toFile();
        archivo = new ArchivoPlano(carpeta.getPath());
    }

    @After
    public void limpiar() throws Exception {
        Files.walk(carpeta.toPath()).sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
    }

    private static Producto producto(SistemaClub sistema, String nombre) {
        for (Producto p : sistema.getProductos()) if (p.getNombre().equals(nombre)) return p;
        throw new AssertionError("No existe el producto " + nombre);
    }

    @Test
    public void sinArchivosNoHayEstadoGuardado() {
        assertFalse(archivo.existeEstado());
        assertThrows(IllegalStateException.class, () -> archivo.cargarEstado());
    }

    @Test
    public void conservaDatosYRelacionesTrasGuardarYCargar() {
        SistemaClub original = new SistemaClub();
        DatosIniciales.completar(original);
        Socio socio = (Socio) original.buscarUsuario("socio.demo");
        Transaccion compra = socio.iniciarTransaccion(null, "tienda");
        compra.agregarDetalle(producto(original, "Balón demo"), 2);
        compra.confirmar();
        archivo.guardarEstado(original);

        assertTrue(archivo.existeEstado());
        SistemaClub cargado = archivo.cargarEstado();

        Socio recuperado = (Socio) cargado.autenticar("socio.demo", "demo123");
        assertNotNull("El socio debe poder iniciar sesión con su contraseña", recuperado);
        assertEquals(socio.getPuntosFidelidad(), recuperado.getPuntosFidelidad());
        assertEquals(1, recuperado.getTransacciones().size());
        assertEquals(3, producto(cargado, "Balón demo").getCantidadTotal());

        // Las relaciones apuntan al mismo objeto, no a copias.
        Equipo equipo = cargado.getEquipos().get(0);
        assertSame(recuperado, equipo.getInscripcionesActivas().get(0).getSocio());
        assertSame(equipo, recuperado.getInscripcionesEquipo().get(0).getEquipo());
        assertSame(producto(cargado, "Balón demo"), recuperado.getTransacciones().get(0).getDetalles().get(0).getProducto());
    }

    @Test
    public void permiteSeguirOperandoSobreElEstadoRecuperado() {
        SistemaClub original = new SistemaClub();
        DatosIniciales.completar(original);
        archivo.guardarEstado(original);

        SistemaClub cargado = archivo.cargarEstado();
        Socio socio = (Socio) cargado.buscarUsuario("socio.demo");
        Transaccion compra = socio.iniciarTransaccion(null, "tienda");
        compra.agregarDetalle(producto(cargado, "Balón demo"), 1);
        compra.confirmar();
        archivo.guardarEstado(cargado);

        SistemaClub otraVez = archivo.cargarEstado();
        assertEquals(4, producto(otraVez, "Balón demo").getCantidadTotal());
        assertEquals(1, ((Socio) otraVez.buscarUsuario("socio.demo")).getTransacciones().size());
    }

    @Test
    public void guardaLosEnumPorNombreYConservaLaIdentidad() {
        SistemaClub original = new SistemaClub();
        DatosIniciales.completar(original);
        Socio socio = (Socio) original.buscarUsuario("socio.demo");
        socio.acumularPuntos(1000);
        Transaccion compra = socio.iniciarTransaccion(null, "tienda");
        compra.agregarDetalle(producto(original, "Balón demo"), 1);
        compra.redimirPuntos(100);
        compra.confirmar();
        archivo.guardarEstado(original);

        Transaccion cargada = ((Socio) archivo.cargarEstado().buscarUsuario("socio.demo")).getTransacciones().get(0);
        assertSame(TipoPromocion.PUNTOS, cargada.getPromocion());
        assertFalse("Un enum no debe guardarse como objeto del modelo", new File(carpeta, "TipoPromocion.csv").exists());
    }

    @Test
    public void unaColeccionQueNoVieneEnLosArchivosSeCargaVacia() throws Exception {
        SistemaClub original = new SistemaClub();
        DatosIniciales.completar(original);
        archivo.guardarEstado(original);
        // Simula datos guardados antes de que existiera el atributo: se renombra la columna en el CSV.
        java.nio.file.Path csv = new File(carpeta, "SistemaClub.csv").toPath();
        String contenido = new String(Files.readAllBytes(csv), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(contenido.contains("SistemaClub.pagosMensualidad"));
        Files.write(csv, contenido.replace("SistemaClub.pagosMensualidad", "SistemaClub.columnaAntigua").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        SistemaClub cargado = archivo.cargarEstado();
        assertNotNull(cargado.getPagosMensualidad());
        assertTrue(cargado.getPagosMensualidad().isEmpty());
    }

    @Test
    public void completarLosDatosInicialesNoDuplicaNada() {
        SistemaClub sistema = new SistemaClub();
        DatosIniciales.completar(sistema);
        int usuarios = sistema.getUsuarios().size();
        int productos = sistema.getProductos().size();
        int instalaciones = sistema.getInstalaciones().size();
        DatosIniciales.completar(sistema);
        assertEquals(usuarios, sistema.getUsuarios().size());
        assertEquals(productos, sistema.getProductos().size());
        assertEquals(instalaciones, sistema.getInstalaciones().size());
    }
}
