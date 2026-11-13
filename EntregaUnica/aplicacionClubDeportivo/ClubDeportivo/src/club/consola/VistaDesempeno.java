package club.consola;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import club.modelo.competencias.*;
import club.modelo.entrenamiento.*;
import club.modelo.instalaciones.*;
import club.modelo.tienda.*;
import club.modelo.usuarios.*;

/** Presenta el desempeño de un socio; lo usan la aplicación del socio y la analítica del entrenador. */
public final class VistaDesempeno {
    private VistaDesempeno() { }

    public static void mostrar(Socio socio) {
        System.out.printf("Porcentaje de asistencia: %.1f%%%n", socio.getPorcentajeAsistencia());
        System.out.println("--- Asistencias y evaluaciones ---");
        java.util.List<SesionEntrenamiento> pendientes = socio.sesionesPendientesDeAsistencia();
        if (socio.getAsistencias().isEmpty() && pendientes.isEmpty()) System.out.println("No hay registros de asistencia.");
        for (SesionEntrenamiento p : pendientes)
            System.out.printf("%s | pendiente (asistencia aún no registrada)%n", p.getFechaHora());
        for (Asistencia a : socio.getAsistencias()) {
            System.out.printf("%s | %s | nota: %s | %s%n", a.getSesion() == null ? "-" : a.getSesion().getFechaHora(), a.getEstado(),
                    a.getNotaDesempeno() == null ? "-" : a.getNotaDesempeno().toString(), a.getComentario() == null ? "" : a.getComentario());
        }
        System.out.println("--- Estadísticas de partidos de conjunto ---");
        int goles = 0, asistencias = 0, amarillas = 0, rojas = 0;
        for (EstadisticaJugadorPartido e : socio.getEstadisticas()) {
            if (e instanceof EstadisticaJugadorFutbol) {
                EstadisticaJugadorFutbol f = (EstadisticaJugadorFutbol) e;
                goles += f.getGoles(); asistencias += f.getAsistencias(); amarillas += f.getTarjetasAmarillas(); rojas += f.getTarjetasRojas();
            } else if (e instanceof EstadisticaJugadorBaloncesto) {
                System.out.printf("Baloncesto: %d puntos, %d rebotes%n", ((EstadisticaJugadorBaloncesto) e).getPuntos(), ((EstadisticaJugadorBaloncesto) e).getRebotes());
            } else if (e instanceof EstadisticaJugadorVoleibol) {
                System.out.printf("Voleibol: %d aces, %d bloqueos%n", ((EstadisticaJugadorVoleibol) e).getAces(), ((EstadisticaJugadorVoleibol) e).getBloqueos());
            }
        }
        System.out.printf("Fútbol: %d goles, %d asistencias, %d amarillas, %d rojas%n", goles, asistencias, amarillas, rojas);
        System.out.println("--- Disciplinas individuales ---");
        boolean alguna = false;
        for (InscripcionDisciplina i : socio.getInscripcionesDisciplina()) {
            if (!i.isActiva()) continue;
            alguna = true;
            System.out.printf("%s | nivel: %s | ranking: %d%n", Formatos.nombreDisciplina(i.getDisciplina()), i.getNivel(), i.getRanking());
        }
        if (!alguna) System.out.println("Sin inscripciones.");
        for (EstadisticaIndividual e : socio.getEstadisticasIndividuales()) {
            if (e instanceof EstadisticaTenisPadel) {
                EstadisticaTenisPadel t = (EstadisticaTenisPadel) e;
                System.out.printf("Partido: sets %d, games %d, aces %d, errores %d%n", t.getSets(), t.getGamesGanados(), t.getAces(), t.getErroresNoForzados());
            } else if (e instanceof EstadisticaTenisMesa) {
                EstadisticaTenisMesa t = (EstadisticaTenisMesa) e;
                System.out.printf("Partido: sets %d, puntos totales %d%n", t.getSets(), t.getPuntosTotales());
            }
        }
    }
}
