package club.persistencia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import club.model.SistemaClub;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.Competencia;
import club.modelo.competencias.DeporteConjunto;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.FormatoLiga;
import club.modelo.competencias.InstitucionRival;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.competencias.ReglaFutbol;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.Padel;
import club.modelo.entrenamiento.Tenis;
import club.modelo.entrenamiento.TenisMesa;
import club.modelo.instalaciones.CanchaConjunto;
import club.modelo.instalaciones.CanchaPadel;
import club.modelo.instalaciones.CanchaTenis;
import club.modelo.instalaciones.Instalacion;
import club.modelo.instalaciones.MesaTenisMesa;
import club.modelo.tienda.Bebida;
import club.modelo.tienda.Existencia;
import club.modelo.tienda.Producto;
import club.modelo.tienda.ProductoTienda;
import club.modelo.tienda.Snack;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Fisioterapeuta;
import club.modelo.usuarios.ProgramacionSemanal;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Turno;

/**
 * Datos de demostración, coherentes con las reglas implementadas, para que cada historia se pueda recorrer desde
 * las aplicaciones de consola sin preparar datos a mano. Solo se crean si el sistema está vacío (primera ejecución
 * o carpeta de datos borrada); sobre un estado ya guardado no se hace nada.
 *
 * Contraseñas: admin123 (administrador), entre123 (entrenadores), fisio123 (fisioterapeutas), demo123 (socios).
 */
public final class DatosIniciales {
    private DatosIniciales() { }

    public static void completar(SistemaClub sistema) {
        if (!sistema.getUsuarios().isEmpty()) return;
        Administrador admin = new Administrador("admin", "admin123", "Administrador del club");
        sistema.registrarUsuario(admin);
        crearEmpleados(sistema, admin);
        List<Socio> socios = crearSocios(sistema);
        crearDisciplinas(sistema, socios);
        List<Equipo> equipos = crearEquipos(sistema, socios);
        crearCompetencia(sistema, admin, equipos.get(0));
        crearProductos(sistema);
    }

    /**
     * Programación que cumple la cobertura mínima de lunes a sábado (05:00–23:00): dos entrenadores y un
     * fisioterapeuta en la mañana (05–14) y lo mismo en la tarde (14–23). Un quinto entrenador refuerza las mañanas
     * del lunes y el miércoles, de modo que un cambio de turno en esos días se puede aprobar sin romper la cobertura.
     */
    private static void crearEmpleados(SistemaClub sistema, Administrador admin) {
        Entrenador e1 = registrar(sistema, new Entrenador("entrenador.demo", "entre123", "Carlos Rojas", "ENTRE15"));
        Entrenador e2 = registrar(sistema, new Entrenador("entrenador2.demo", "entre123", "Laura Gómez", "ENTRE2"));
        Entrenador e3 = registrar(sistema, new Entrenador("entrenador3.demo", "entre123", "Andrés Pérez", "ENTRE3"));
        Entrenador e4 = registrar(sistema, new Entrenador("entrenador4.demo", "entre123", "Sofía Martínez", "ENTRE4"));
        Entrenador e5 = registrar(sistema, new Entrenador("entrenador5.demo", "entre123", "Diego Castro", "ENTRE5"));
        Fisioterapeuta f1 = registrar(sistema, new Fisioterapeuta("fisio.demo", "fisio123", "Valentina Ruiz", "FISIO15"));
        Fisioterapeuta f2 = registrar(sistema, new Fisioterapeuta("fisio2.demo", "fisio123", "Mateo Herrera", "FISIO2"));
        ProgramacionSemanal p = new ProgramacionSemanal(Arrays.<Empleado>asList(e1, e2, e3, e4, e5, f1, f2));
        for (DayOfWeek dia : Turno.DIAS_DE_ATENCION) {
            for (Empleado e : Arrays.<Empleado>asList(e1, e2, f1)) p.asignar(e, turno(dia, 5, 14));
            for (Empleado e : Arrays.<Empleado>asList(e3, e4, f2)) p.asignar(e, turno(dia, 14, 23));
        }
        p.asignar(e5, turno(DayOfWeek.MONDAY, 5, 14));
        p.asignar(e5, turno(DayOfWeek.WEDNESDAY, 5, 14));
        admin.publicarProgramacion(p);
    }

    private static List<Socio> crearSocios(SistemaClub sistema) {
        List<Socio> socios = new ArrayList<>();
        socios.add(registrar(sistema, new Socio("socio.demo", "demo123", "Ana Torres", LocalDate.of(2000, 1, 1))));
        socios.add(registrar(sistema, new Socio("socio.demo2", "demo123", "Juan Medina", LocalDate.of(2001, 5, 20))));
        String[] nombres = { "Pablo Ríos", "Camila Vargas", "Felipe León", "Daniela Mora", "Santiago Gil", "Isabela Cruz",
                "Tomás Reyes", "Mariana Díaz", "Nicolás Peña", "Lucía Romero", "Emilio Suárez", "Paula Ortiz",
                "Gabriel Rincón", "Sara Molina" };
        for (int i = 0; i < nombres.length; i++) {
            String login = String.format("jugador%02d", i + 1);
            socios.add(registrar(sistema, new Socio(login, "demo123", nombres[i], LocalDate.of(1998 + i % 6, 1 + i % 12, 10))));
        }
        return socios;
    }

    /** Academias individuales; los dos socios de demostración están en tenis con niveles adyacentes. */
    private static void crearDisciplinas(SistemaClub sistema, List<Socio> socios) {
        Tenis tenis = new Tenis(2);
        tenis.registrarInstalacion(new CanchaTenis(2, 4, "Cancha de tenis 1", false));
        tenis.registrarInstalacion(new CanchaTenis(2, 4, "Cancha de tenis 2", false));
        Padel padel = new Padel(1);
        padel.registrarInstalacion(new CanchaPadel(4, 4, "Cancha de pádel 1", true));
        TenisMesa mesa = new TenisMesa(2);
        mesa.registrarInstalacion(new MesaTenisMesa(2, 4, "Mesa techada 1", true));
        mesa.registrarInstalacion(new MesaTenisMesa(2, 4, "Mesa exterior 1", false));
        for (DisciplinaIndividual d : new DisciplinaIndividual[] { tenis, padel, mesa })
            for (Instalacion i : d.getInstalaciones()) sistema.registrarInstalacion(i);
        sistema.registrarInstalacion(new CanchaConjunto(10, 22, "Cancha de fútbol", false));
        socios.get(0).inscribirseEnDisciplina(tenis, "Intermedio");
        socios.get(1).inscribirseEnDisciplina(tenis, "Avanzado");
        socios.get(2).inscribirseEnDisciplina(tenis, "Principiante");
        socios.get(0).inscribirseEnDisciplina(padel, "Principiante");
        for (int i = 1; i <= 3; i++) socios.get(i).inscribirseEnDisciplina(padel, "Principiante");
    }

    /** Dos equipos de fútbol de mayores con 8 jugadores cada uno (el mínimo para jugar es 7). */
    private static List<Equipo> crearEquipos(SistemaClub sistema, List<Socio> socios) {
        DeporteConjunto futbol = sistema.obtenerDeporte("Fútbol");
        Categoria mayores = sistema.obtenerCategoria("Mayores");
        String[] posiciones = { "portero", "defensa", "defensa", "defensa", "mediocampista", "mediocampista", "delantero", "delantero" };
        Equipo a = new Equipo("Séneca Fútbol A", futbol, mayores, 12);
        sistema.registrarEquipo(a);
        Equipo b = new Equipo("Séneca Fútbol B", futbol, mayores, 12);
        sistema.registrarEquipo(b);
        // Equipo A: socio.demo (delantero, camiseta 10) y jugador01–jugador07.
        a.inscribirJugador(socios.get(0), "delantero", 10);
        for (int i = 0; i < 7; i++) a.inscribirJugador(socios.get(2 + i), posiciones[i], i + 1);
        // Equipo B: jugador08–jugador14 y socio.demo2 (delantero, camiseta 9).
        for (int i = 0; i < 7; i++) b.inscribirJugador(socios.get(9 + i), posiciones[i], i + 1);
        b.inscribirJugador(socios.get(1), "delantero", 9);
        return Arrays.asList(a, b);
    }

    /**
     * Liga con el equipo A y dos instituciones rivales. El calendario propone tres partidos; uno ya tiene el visto
     * bueno del administrador (responsable: entrenador.demo) y los otros dos quedan propuestos.
     */
    private static void crearCompetencia(SistemaClub sistema, Administrador admin, Equipo equipo) {
        Competencia liga = admin.crearCompetencia("Copa Universitaria", "ASCUN", "2026", equipo.getDeporte(), equipo.getCategoria(),
                new FormatoLiga("Liga", new ReglaFutbol(3, 0)), sistema);
        liga.agregarEquipo(equipo);
        List<club.modelo.usuarios.Socio> nomina = new ArrayList<>();
        equipo.getInscripcionesActivas().forEach(i -> nomina.add(i.getSocio()));
        liga.registrarNomina(equipo, nomina);
        liga.agregarEquipo(new InstitucionRival("Universidad Javeriana"));
        liga.agregarEquipo(new InstitucionRival("Universidad del Rosario"));
        liga.generarCalendario();
        PartidoCompetencia primero = liga.getPartidos().get(0);
        admin.programarPartidoOficial(primero, LocalDate.of(2026, 11, 21), "Estadio Séneca", (Entrenador) sistema.buscarUsuario("entrenador.demo"));
    }

    private static void crearProductos(SistemaClub sistema) {
        agregarProducto(sistema, new ProductoTienda("Balón demo", 50000, "deporte"), 5, "tienda");
        agregarProducto(sistema, new ProductoTienda("Raqueta de tenis demo", 180000, "raquetas"), 8, "tienda");
        agregarProducto(sistema, new ProductoTienda("Camiseta del club", 65000, "ropa"), 12, "tienda");
        agregarProducto(sistema, new Bebida("Agua demo", 3000, false), 30, "cafeteria");
        agregarProducto(sistema, new Bebida("Café demo", 4000, true), 30, "cafeteria");
        agregarProducto(sistema, new Snack("Barra de maní demo", 3500, new ArrayList<>(Collections.singletonList("maní"))), 20, "cafeteria");
        agregarProducto(sistema, new Snack("Galletas de avena", 2500, new ArrayList<>(Arrays.asList("gluten", "leche"))), 15, "cafeteria");
    }

    private static Turno turno(DayOfWeek dia, int desde, int hasta) {
        return new Turno(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0));
    }

    private static <T extends club.modelo.usuarios.Usuario> T registrar(SistemaClub sistema, T usuario) {
        sistema.registrarUsuario(usuario);
        return usuario;
    }

    private static void agregarProducto(SistemaClub sistema, Producto producto, int cantidad, String ubicacion) {
        producto.agregarExistencia(new Existencia(cantidad, ubicacion));
        sistema.registrarProducto(producto);
    }
}
