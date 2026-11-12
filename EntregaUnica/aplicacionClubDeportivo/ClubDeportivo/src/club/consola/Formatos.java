package club.consola;

import club.modelo.competencias.Equipo;
import club.modelo.competencias.PartidoCompetencia;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.PartidoPractica;
import club.modelo.tienda.Bebida;
import club.modelo.tienda.Producto;
import club.modelo.tienda.ProductoTienda;
import club.modelo.tienda.Snack;
import club.modelo.usuarios.Administrador;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Fisioterapeuta;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Turno;
import club.modelo.usuarios.Usuario;

/** Textos con los que la consola presenta los objetos del modelo. No contiene reglas del club. */
public final class Formatos {
    private Formatos() { }

    public static String nombreRol(Usuario usuario) {
        if (usuario instanceof Administrador) return "administrador";
        if (usuario instanceof Entrenador) return "entrenador";
        if (usuario instanceof Fisioterapeuta) return "fisioterapeuta";
        if (usuario instanceof Socio) return "socio";
        return "usuario";
    }

    public static String persona(Usuario u) {
        return u.getNombre() + " (" + u.getLogin() + ")";
    }

    public static String etiquetaEquipo(Equipo e) {
        return e.getNombre() + " (" + e.getDeporte().getNombre() + ", " + e.getCategoria().getNombre() + ", "
                + e.getInscripcionesActivas().size() + "/" + e.getCupoMaximo() + " jugadores)";
    }

    public static String nombresEquipos(PartidoCompetencia p) {
        if (p.getEquipos().size() < 2) return "partido sin definir";
        return p.getEquipos().get(0).getNombre() + " vs " + p.getEquipos().get(1).getNombre();
    }

    public static String etiquetaPractica(PartidoPractica p) {
        StringBuilder sb = new StringBuilder(nombreDisciplina(p.getDisciplina())).append(" (").append(p.getModalidad()).append("): ");
        for (int i = 0; i < p.getParticipantes().size(); i++) {
            if (i > 0) sb.append(" vs ");
            sb.append(p.getParticipantes().get(i).getSocio().getNombre()).append(" [").append(p.getParticipantes().get(i).getNivel()).append("]");
        }
        return sb.toString();
    }

    public static String estadisticaIndividual(club.modelo.entrenamiento.EstadisticaIndividual e) {
        if (e instanceof club.modelo.entrenamiento.EstadisticaTenisPadel) {
            club.modelo.entrenamiento.EstadisticaTenisPadel t = (club.modelo.entrenamiento.EstadisticaTenisPadel) e;
            return "games " + t.getGamesGanados() + ", aces " + t.getAces() + ", errores no forzados " + t.getErroresNoForzados();
        }
        if (e instanceof club.modelo.entrenamiento.EstadisticaTenisMesa)
            return "puntos " + ((club.modelo.entrenamiento.EstadisticaTenisMesa) e).getPuntosTotales();
        return "";
    }

    public static String nombreDisciplina(DisciplinaIndividual d) { return d.getClass().getSimpleName(); }

    public static String etiquetaTurno(Turno t) {
        return t.toString();
    }

    public static String describirProducto(Producto p) {
        String extra = "";
        if (p instanceof ProductoTienda) extra = " [" + ((ProductoTienda) p).getCategoria() + "]";
        else if (p instanceof Bebida) extra = ((Bebida) p).esCaliente() ? " [bebida caliente]" : " [bebida fría]";
        else if (p instanceof Snack) extra = " [snack, alérgenos: " + (((Snack) p).getAlergenos().isEmpty() ? "ninguno" : String.join(", ", ((Snack) p).getAlergenos())) + "]";
        return String.format("%s%s - $%.0f (disponibles: %d)", p.getNombre(), extra, p.getPrecio(), p.getCantidadTotal());
    }
}
