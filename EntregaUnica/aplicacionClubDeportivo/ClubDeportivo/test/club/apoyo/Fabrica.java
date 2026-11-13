package club.apoyo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import club.model.SistemaClub;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.Futbol;
import club.modelo.entrenamiento.Padel;
import club.modelo.entrenamiento.Tenis;
import club.modelo.instalaciones.CanchaPadel;
import club.modelo.instalaciones.CanchaTenis;
import club.modelo.tienda.Existencia;
import club.modelo.tienda.ProductoTienda;
import club.modelo.usuarios.Socio;

/** Objetos de prueba reutilizables. Cada llamada crea instancias nuevas e independientes. */
public final class Fabrica {
    private Fabrica() { }

    /** Bloque horario fijo en el futuro, para que las pruebas no dependan de la fecha actual. */
    public static final LocalDateTime BLOQUE = LocalDateTime.of(2030, 5, 10, 10, 0);

    public static Socio socio(String login) {
        return socioConEdad(login, 25);
    }

    public static Socio socioConEdad(String login, int edad) {
        Socio s = new Socio(login, "clave123", "Socio " + login, LocalDate.now().minusYears(edad).minusDays(1));
        s.setDocumento("DOC-" + login);
        return s;
    }

    public static Socio registrar(SistemaClub sistema, Socio socio) {
        sistema.registrarUsuario(socio);
        return socio;
    }

    /** Tenis con dos canchas y un máximo de instalaciones simultáneas configurable. */
    public static Tenis tenis(int maxSimultaneas) {
        Tenis tenis = new Tenis(maxSimultaneas);
        tenis.registrarInstalacion(new CanchaTenis(2, 4, "Tenis 1", false));
        tenis.registrarInstalacion(new CanchaTenis(2, 4, "Tenis 2", false));
        return tenis;
    }

    public static Padel padel() {
        Padel padel = new Padel(2);
        padel.registrarInstalacion(new CanchaPadel(4, 4, "Pádel 1", true));
        return padel;
    }

    public static Equipo equipoFutbol(String nombre, Categoria categoria, int cupo) {
        return new Equipo(nombre, new Futbol("Fútbol", Math.min(7, Math.max(cupo, 1))), categoria, cupo);
    }

    public static Categoria abierta() { return new Categoria("Abierta", 0, 150); }

    /** Equipo activo del deporte y categoría dados, con la cantidad de jugadores indicada. */
    public static Equipo equipoConJugadores(String nombre, int jugadores, club.modelo.competencias.DeporteConjunto deporte, Categoria categoria) {
        Equipo e = new Equipo(nombre, deporte, categoria, Math.max(jugadores, 1) + 5);
        String[] pos = posiciones(deporte);
        for (int i = 1; i <= jugadores; i++) e.inscribirJugador(socio(nombre.replace(' ', '.') + ".j" + i), pos[(i - 1) % pos.length], i);
        return e;
    }

    private static String[] posiciones(club.modelo.competencias.DeporteConjunto deporte) {
        if (deporte instanceof club.modelo.competencias.Baloncesto) return new String[] { "base", "escolta", "alero", "pivot" };
        if (deporte instanceof club.modelo.competencias.Voleibol) return new String[] { "colocador", "opuesto", "central", "punta" };
        return new String[] { "portero", "defensa", "mediocampista", "delantero" };
    }

    /** Equipo de fútbol activo con la cantidad de jugadores indicada (posiciones y camisetas válidas). */
    public static Equipo equipoConJugadores(String nombre, int jugadores) {
        Equipo e = equipoFutbol(nombre, abierta(), Math.max(jugadores, 1) + 5);
        for (int i = 1; i <= jugadores; i++) e.inscribirJugador(socio(nombre.replace(' ', '.') + ".j" + i), i == 1 ? "portero" : "defensa", i);
        return e;
    }

    public static ProductoTienda productoTienda(String nombre, double precio, int existencia) {
        ProductoTienda p = new ProductoTienda(nombre, precio, "deporte");
        p.agregarExistencia(new Existencia(existencia, "tienda"));
        return p;
    }
}
