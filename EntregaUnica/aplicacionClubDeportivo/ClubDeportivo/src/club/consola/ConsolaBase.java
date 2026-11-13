package club.consola;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

import club.model.SistemaClub;
import club.modelo.usuarios.Usuario;
import club.persistencia.ArchivoPlano;
import club.persistencia.DatosIniciales;

/**
 * Comportamiento común de las tres aplicaciones de consola (socio, empleado y administrador):
 * carga automática de los datos, inicio de sesión restringido al rol, ciclo del menú,
 * manejo de errores de la lógica y guardado automático.
 *
 * Esta clase y sus subclases solo leen datos y muestran resultados; las reglas del club
 * están en el paquete club.modelo y se comunican con la interfaz mediante excepciones.
 */
public abstract class ConsolaBase {
    /** Carpeta de datos predeterminada; se puede cambiar con -Dclub.datos=ruta en los argumentos de la VM. */
    public static final String CARPETA_DATOS = System.getProperty("club.datos", "datos_seneca");

    private final ArchivoPlano archivo;
    protected SistemaClub sistema;
    protected EntradaConsola entrada;

    protected ConsolaBase() {
        this.archivo = new ArchivoPlano(CARPETA_DATOS);
    }

    /** Nombre del rol que muestra la aplicación (p. ej. "socio"). */
    protected abstract String nombreAplicacion();

    /** Indica si el usuario autenticado puede usar esta aplicación. */
    protected abstract boolean puedeIngresar(Usuario usuario);

    /** Construye el menú del usuario autenticado. */
    protected abstract Menu construirMenu(Usuario usuario);

    // ===================================================================== CICLO DE LA APLICACIÓN
    public final void iniciar() {
        sistema = archivo.existeEstado() ? archivo.cargarEstado() : new SistemaClub();
        DatosIniciales.completar(sistema);
        sistema.completarDatosFaltantes();
        guardar();
        try (Scanner scanner = new Scanner(System.in)) {
            entrada = new EntradaConsola(scanner, System.out);
            System.out.println("=== Club Deportivo Séneca · Aplicación de " + nombreAplicacion() + " ===");
            System.out.println("Escriba 'cancelar' en cualquier pregunta para abandonar la operación en curso.");
            while (true) {
                Usuario usuario = iniciarSesion();
                if (usuario == null) break;
                atender(usuario);
            }
        } catch (EntradaConsola.FinDeEntrada e) {
            System.out.println();
        } finally {
            guardar();
            System.out.println("Datos guardados en " + archivo.getCarpeta().getPath() + ". Hasta pronto.");
        }
    }

    /** Pide credenciales hasta que sean válidas para este rol. Devuelve null si el usuario escribe 'salir'. */
    private Usuario iniciarSesion() {
        while (true) {
            System.out.println("\nInicio de sesión (escriba 'salir' para terminar)");
            String login;
            String password;
            try {
                login = entrada.texto("Login: ");
                if ("salir".equalsIgnoreCase(login)) return null;
                password = entrada.texto("Contraseña: ");
            } catch (EntradaConsola.OperacionCancelada e) {
                continue;
            }
            Usuario usuario = sistema.autenticar(login, password);
            // No se indica qué dato falló, ni si el usuario existe con otro rol.
            if (usuario == null || !puedeIngresar(usuario)) {
                System.out.println("Credenciales incorrectas.");
                continue;
            }
            System.out.println("Bienvenido, " + usuario.getNombre() + ".");
            return usuario;
        }
    }

    private void atender(Usuario usuario) {
        Menu menu = construirMenu(usuario);
        menu.opcion("P", "Cambiar mi contraseña", () -> cambiarPassword(usuario));
        while (true) {
            System.out.print(menu.mostrar());
            String opcion;
            try {
                opcion = entrada.texto("Opción: ");
            } catch (EntradaConsola.OperacionCancelada e) {
                continue;
            }
            if ("0".equals(opcion)) return;
            Runnable accion = menu.accion(opcion);
            if (accion == null) System.out.println("Opción no válida.");
            else ejecutar(accion);
        }
    }

    /** Ejecuta una acción del menú: muestra el motivo si la lógica la rechaza y guarda el estado al terminar. */
    protected void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (EntradaConsola.OperacionCancelada e) {
            System.out.println(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException | DateTimeException | UnsupportedOperationException e) {
            System.out.println("No se completó la operación: " + e.getMessage());
        }
        guardar();
    }

    private void guardar() {
        if (sistema != null) archivo.guardarEstado(sistema);
    }

    private void cambiarPassword(Usuario usuario) {
        String actual = entrada.texto("Contraseña actual: ");
        String nueva = entrada.texto("Nueva contraseña: ");
        usuario.actualizarPassword(actual, nueva);
        System.out.println("Contraseña actualizada.");
    }

    // ===================================================================== ATAJOS DE ENTRADA
    protected String texto(String mensaje) { return entrada.texto(mensaje); }
    protected String textoOpcional(String mensaje) { return entrada.textoOpcional(mensaje); }
    protected int entero(String mensaje) { return entrada.entero(mensaje); }
    protected double decimal(String mensaje) { return entrada.decimal(mensaje); }
    protected boolean confirmar(String mensaje) { return entrada.confirmar(mensaje); }
    protected LocalDate fecha(String mensaje) { return entrada.fecha(mensaje); }
    protected LocalDateTime fechaHora(String mensaje) { return entrada.fechaHora(mensaje); }
    protected LocalTime hora(String mensaje) { return entrada.hora(mensaje); }
    protected DayOfWeek dia(String mensaje) { return entrada.dia(mensaje); }
    protected <T> T elegir(String titulo, List<T> opciones, Function<T, String> etiqueta) { return entrada.elegir(titulo, opciones, etiqueta); }
}
