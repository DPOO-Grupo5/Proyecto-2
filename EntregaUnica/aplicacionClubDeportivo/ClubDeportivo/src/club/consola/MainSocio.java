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

/** Aplicación de consola para los socios del club. */
public class MainSocio extends ConsolaComprador {

    public static void main(String[] args) {
        new MainSocio().iniciar();
    }

    @Override protected String nombreAplicacion() { return "socios"; }

    @Override protected boolean puedeIngresar(Usuario usuario) { return usuario instanceof Socio; }

    @Override
    protected Menu construirMenu(Usuario usuario) {
        Socio socio = (Socio) usuario;
        return new Menu("Menú del socio")
                .seccion("Deportes")
                .opcion("1", "Mis equipos, partidos y sanciones", () -> verMisEquipos(socio))
                .opcion("2", "Inscribirme en una disciplina individual", () -> inscribirseEnDisciplina(socio))
                .opcion("3", "Reservar una instalación", () -> reservarInstalacion(socio))
                .opcion("4", "Programar un partido de práctica sobre mi reserva", () -> programarPractica(socio))
                .opcion("5", "Historial de mis partidos de práctica", () -> verHistorialPracticas(socio))
                .opcion("6", "Mi desempeño (asistencia, evaluaciones, estadísticas y ranking)", () -> verDesempeno(socio))
                .opcion("7", "Mis convocatorias a entrenamiento", () -> verConvocatorias(socio))
                .seccion("Tienda y cafetería")
                .opcion("8", "Comprar en la tienda", () -> comprar(socio, "tienda"))
                .opcion("9", "Comprar en la cafetería", () -> comprar(socio, "cafeteria"))
                .opcion("10", "Mis compras y puntos de fidelidad", () -> verCompras(socio))
                .seccion("Más consultas")
                .opcion("11", "Iniciar el uso de mi reserva", () -> iniciarUso(socio))
                .opcion("12", "Mi ranking por disciplina", () -> verRanking(socio))
                .opcion("13", "Competencias: tabla, goleadores y destacados", this::consultarCompetencias)
                .opcion("14", "Mi código de descuento (guardar o quitar)", () -> codigoGuardado(socio));
    }

    private void verMisEquipos(Socio socio) {
        System.out.println("--- Mis equipos ---");
        boolean alguno = false;
        for (InscripcionEquipo i : socio.getInscripcionesEquipo()) {
            if (!i.isActiva()) continue;
            alguno = true;
            Equipo e = i.getEquipo();
            System.out.printf("%s | %s | %s | posición: %s | camiseta: %d%n", e.getNombre(), e.getDeporte().getNombre(),
                    e.getCategoria().getNombre(), i.getPosicion(), i.getNumeroCamiseta());
            int[] r = e.resumenResultados();
            System.out.printf("   Resultados: PJ %d | G %d | E %d | P %d | GF %d | GC %d%s%n", r[0], r[1], r[2], r[3], r[4], r[5],
                    r[0] == 0 ? " (no hay partidos jugados)" : "");
        }
        if (!alguno) System.out.println("No pertenece a ningún equipo.");
        System.out.println("--- Calendario y resultados ---");
        boolean hayPartidos = false;
        for (InscripcionEquipo i : socio.getInscripcionesEquipo()) {
            if (!i.isActiva()) continue;
            Equipo e = i.getEquipo();
            for (PartidoInterno p : e.getPartidosInternos()) {
                hayPartidos = true;
                System.out.printf("[Interno] %s: %s vs %s | marcador: %s%n", p.getFechaHora(), p.getEquipoLocal().getNombre(),
                        p.getEquipoVisitante().getNombre(), p.getMarcadorFinal());
            }
            for (Competencia c : sistema.getCompetencias()) {
                for (PartidoCompetencia p : c.getPartidos()) {
                    if (!p.getEquipos().contains(e)) continue;
                    hayPartidos = true;
                    System.out.printf("[%s] %s: %s | marcador: %s | escenario: %s%n", c.getNombre(),
                            p.getFecha() == null ? "sin fecha" : p.getFecha().toString(), Formatos.nombresEquipos(p), p.getMarcador(), p.getEscenario());
                }
            }
        }
        if (!hayPartidos) System.out.println("Sin partidos registrados.");
        System.out.println("--- Sanciones vigentes ---");
        if (socio.getSancionesVigentes().isEmpty()) System.out.println("Ninguna.");
        for (Sancion s : socio.getSancionesVigentes())
            System.out.printf("%s (suspensión de %d partido(s))%n", s.getMotivo(), s.getPartidosSuspension());
    }

    private void inscribirseEnDisciplina(Socio socio) {
        DisciplinaIndividual d = elegir("Disciplinas disponibles:", sistema.getDisciplinas(), Formatos::nombreDisciplina);
        String nivel = elegir("Nivel inicial:", Arrays.asList("Principiante", "Intermedio", "Avanzado"), x -> x);
        InscripcionDisciplina i = socio.inscribirseEnDisciplina(d, nivel);
        System.out.printf("Inscripción exitosa en %s. Nivel: %s | ranking: %d%n", Formatos.nombreDisciplina(d), i.getNivel(), i.getRanking());
    }

    private void reservarInstalacion(Socio socio) {
        Instalacion inst = elegir("Instalaciones:", sistema.getInstalaciones(),
                i -> i.getNombre() + " (" + i.getClass().getSimpleName() + (i.isTechada() ? ", techada" : "") + ", "
                        + i.getJugadoresMin() + " a " + i.getJugadoresMax() + " jugadores)");
        LocalDateTime ini = fechaHora("Inicio (AAAA-MM-DD HH:mm): ");
        LocalDateTime fin = fechaHora("Fin (AAAA-MM-DD HH:mm): ");
        int jugadores = entero("Número de jugadores: ");
        String modalidad = texto("Modalidad (sencillos/dobles): ");
        Reserva r = socio.reservarInstalacion(inst, ini, fin, jugadores, modalidad);
        System.out.printf("Reserva creada en %s de %s a %s (%d jugadores, %s).%n", inst.getNombre(),
                r.getFechaHoraInicio(), r.getFechaHoraFin(), r.getCantidadJugadores(), r.getModalidad());
    }

    /** HU-S04: el partido se programa sobre una reserva activa del socio, con tantos jugadores como tenga la reserva. */
    private void programarPractica(Socio socio) {
        List<Reserva> disponibles = new ArrayList<>();
        for (Reserva r : socio.getReservas())
            if (r.getSocioResponsable() == socio && r.estaActiva(club.model.Reloj.ahora()) && r.getPartidoPractica() == null
                    && r.getInstalacion().getDisciplina() != null) disponibles.add(r);
        Reserva r = elegir("Sus reservas activas sin partido:", disponibles,
                x -> x.getInstalacion().getNombre() + " | " + x.getFechaHoraInicio() + " | " + x.getModalidad() + " | " + x.getCantidadJugadores() + " jugadores");
        DisciplinaIndividual d = r.getInstalacion().getDisciplina();
        List<Socio> candidatos = new ArrayList<>();
        for (InscripcionDisciplina i : d.getInscripciones()) if (i.isActiva() && i.getSocio() != socio) candidatos.add(i.getSocio());
        List<Socio> otros = new ArrayList<>();
        int faltan = r.getCantidadJugadores() - 1;
        if ("dobles".equalsIgnoreCase(r.getModalidad())) System.out.println("En dobles, el primero que elija será su compañero; los otros dos, la pareja rival.");
        for (int k = 1; k <= faltan; k++) {
            List<Socio> restantes = new ArrayList<>(candidatos);
            restantes.removeAll(otros);
            otros.add(elegir("Participante " + k + " de " + faltan + ":", restantes,
                    s -> s.getNombre() + " (" + s.getLogin() + ", " + s.inscripcionEn(d).getNivel() + ")"));
        }
        Entrenador entrenador = null;
        if (confirmar("¿Participa un entrenador? (permite niveles no adyacentes)"))
            entrenador = elegir("Entrenadores:", sistema.getEntrenadores(), Formatos::persona);
        PartidoPractica p = socio.programarPartidoPractica(r, otros, entrenador);
        System.out.println("Partido de práctica programado: " + Formatos.etiquetaPractica(p) + " el " + p.getFechaHora() + ".");
        System.out.println("El resultado lo registra un entrenador.");
    }

    /** HU-S07 */
    private void verHistorialPracticas(Socio socio) {
        List<DisciplinaIndividual> mias = new ArrayList<>();
        for (InscripcionDisciplina i : socio.getInscripcionesDisciplina()) if (i.isActiva()) mias.add(i.getDisciplina());
        DisciplinaIndividual d = elegir("Disciplina:", mias, Formatos::nombreDisciplina);
        LocalDate desde = null, hasta = null;
        if (confirmar("¿Filtrar por rango de fechas?")) {
            desde = fecha("Desde (AAAA-MM-DD): ");
            hasta = fecha("Hasta (AAAA-MM-DD): ");
        }
        List<PartidoPractica> historial = socio.historialPracticas(d, desde, hasta);
        System.out.println("--- Historial de " + Formatos.nombreDisciplina(d) + " | ELO actual: " + socio.inscripcionEn(d).getRanking() + " ---");
        if (historial.isEmpty()) System.out.println("No hay partidos con resultado en ese periodo.");
        for (PartidoPractica p : historial) {
            List<String> rivales = new ArrayList<>();
            for (Socio s : p.oponentesDe(socio)) rivales.add(s.getNombre());
            String stats = "";
            for (EstadisticaIndividual e : p.getEstadisticas()) if (e.getInscripcion().getSocio() == socio) stats = Formatos.estadisticaIndividual(e);
            System.out.printf("%s | vs %s | %s | %s%n", p.getFechaHora().toLocalDate(), String.join(" y ", rivales), p.resultadoPara(socio), stats);
        }
    }

    /** HU-S08 */
    private void verConvocatorias(Socio socio) {
        List<SesionEntrenamiento> sesiones = socio.consultarConvocatorias();
        if (sesiones.isEmpty()) { System.out.println("No tiene convocatorias a entrenamientos."); return; }
        LocalDateTime ahora = club.model.Reloj.ahora();
        for (SesionEntrenamiento s : sesiones)
            System.out.printf("%s %s | %s | %d min | %s | entrenador: %s%n", s.getFechaHora().isAfter(ahora) ? "[próxima]" : "[pasada] ",
                    s.getFechaHora().toLocalDate(), s.getFechaHora().toLocalTime(), s.getDuracionMinutos(), s.getInstalacion().getNombre(), s.getEntrenador().getNombre());
    }

    private void verDesempeno(Socio socio) {
        VistaDesempeno.mostrar(socio);
    }

    /** HU-S05: valida el acceso a la reserva (bebida caliente en pádel o mesa techada). */
    private void iniciarUso(Socio socio) {
        List<Reserva> activas = new ArrayList<>();
        for (Reserva r : socio.getReservas()) if (r.estaActiva(club.model.Reloj.ahora())) activas.add(r);
        if (activas.isEmpty()) { System.out.println("No tiene reservas activas."); return; }
        Reserva r = elegir("Su reserva:", activas,
                x -> x.getInstalacion().getNombre() + " | " + x.getFechaHoraInicio() + " a " + x.getFechaHoraFin());
        r.iniciarUso(socio, club.model.Reloj.ahora());
        System.out.println("Uso de la reserva iniciado. ¡Buen juego!");
    }

    /** HU-S06: ELO y posición en una disciplina; se rechaza si el socio no está inscrito. */
    private void verRanking(Socio socio) {
        DisciplinaIndividual d = elegir("Disciplina:", sistema.getDisciplinas(), Formatos::nombreDisciplina);
        int posicion = sistema.posicionRanking(d, socio);
        InscripcionDisciplina i = socio.inscripcionEn(d);
        System.out.printf("%s | ELO: %d | posición: %d%n", Formatos.nombreDisciplina(d), i.getRanking(), posicion);
        if (!socio.tienePartidosEn(d)) System.out.println("Aún no tiene partidos jugados en esta disciplina.");
    }

    /** HU-S11 y HU-S12: tabla de posiciones, goleadores y jugador destacado de una competencia. */
    private void consultarCompetencias() {
        String deporte = textoOpcional("Filtrar por deporte (Enter = todos): ");
        String categoria = textoOpcional("Filtrar por categoría (Enter = todas): ");
        List<Competencia> lista = new ArrayList<>();
        for (Competencia c : sistema.getCompetencias()) {
            if (!deporte.isEmpty() && !c.getDeporte().getNombre().equalsIgnoreCase(deporte)) continue;
            if (!categoria.isEmpty() && !c.getCategoria().getNombre().equalsIgnoreCase(categoria)) continue;
            lista.add(c);
        }
        if (lista.isEmpty()) { System.out.println("No hay información de competencias para ese filtro."); return; }
        Competencia c = elegir("Competencia:", lista, x -> x.getNombre() + " " + x.getTemporada() + " (" + x.getDeporte().getNombre() + ", " + x.getCategoria().getNombre() + ")");
        int op = entero("1) Tabla de posiciones   2) Goleadores   3) Jugador destacado: ");
        if (!c.tieneResultados()) { System.out.println("No hay información: la competencia aún no tiene resultados."); return; }
        if (op == 1) {
            if (c.getFormato() instanceof FormatoLiga) {
                FormatoLiga liga = (FormatoLiga) c.getFormato();
                liga.actualizarPosiciones();
                Formatos.imprimirTabla("Tabla de posiciones", liga.getPosiciones());
            } else if (c.getFormato() instanceof FormatoPorFases) {
                FormatoPorFases fases = (FormatoPorFases) c.getFormato();
                if (fases.getFaseGrupos() != null) for (Grupo g : fases.getFaseGrupos().getGrupos()) {
                    g.actualizarPosiciones();
                    Formatos.imprimirTabla(g.getNombre(), g.getPosiciones());
                }
                if (fases.getFaseEliminacion() != null && !fases.getFaseEliminacion().getRondas().isEmpty()) {
                    RondaEliminacion primera = fases.getFaseEliminacion().getRondas().get(0);
                    System.out.println("Clasificados a eliminación (" + primera.getNombre() + "):");
                    for (PartidoCompetencia p : primera.getPartidos()) System.out.println("   " + Formatos.nombresEquipos(p));
                } else System.out.println("La fase de eliminación aún no se ha generado.");
            }
        } else if (op == 2) {
            List<Map.Entry<Socio, Integer>> goleadores = c.clasificacionGoleadores();
            if (goleadores.isEmpty()) System.out.println("No hay información de goleadores en esta competencia.");
            for (Map.Entry<Socio, Integer> e : goleadores) System.out.printf("%s: %d goles%n", e.getKey().getNombre(), e.getValue());
        } else if (op == 3) {
            String criterio = c.getDeporte() instanceof Futbol ? "asistencias" : c.getDeporte() instanceof Baloncesto ? "puntos" : "aces";
            List<Map.Entry<Socio, Integer>> destacados = c.clasificacionDestacados();
            if (destacados.isEmpty()) System.out.println("No hay información de jugadores destacados en esta competencia.");
            for (Map.Entry<Socio, Integer> e : destacados) System.out.printf("%s: %d %s%n", e.getKey().getNombre(), e.getValue(), criterio);
        } else throw new IllegalArgumentException("Opción no válida.");
    }

    /** Guarda el código que un empleado compartió con el socio, para aplicarlo sin escribirlo en cada compra. */
    private void codigoGuardado(Socio socio) {
        System.out.println(socio.getCodigoGuardado() == null ? "No tiene un código guardado."
                : "Código guardado: " + socio.getCodigoGuardado() + " (8 % en tienda y cafetería).");
        int op = entero("1) Guardar un código   2) Quitar el código guardado   0) Volver: ");
        if (op == 1) {
            socio.guardarCodigo(texto("Código que le compartió un empleado: "), sistema.getEmpleados());
            System.out.println("Código guardado. Se le ofrecerá en cada compra.");
        } else if (op == 2) {
            socio.olvidarCodigoGuardado();
            System.out.println("Código quitado.");
        } else if (op != 0) throw new IllegalArgumentException("Opción no válida.");
    }
}
