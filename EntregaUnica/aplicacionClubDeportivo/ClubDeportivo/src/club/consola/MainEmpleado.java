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

/** Aplicación de consola para los empleados: entrenadores y fisioterapeutas. */
public class MainEmpleado extends ConsolaComprador {

    public static void main(String[] args) {
        new MainEmpleado().iniciar();
    }

    @Override protected String nombreAplicacion() { return "empleados"; }

    @Override protected boolean puedeIngresar(Usuario usuario) { return usuario instanceof Empleado; }

    @Override
    protected Menu construirMenu(Usuario usuario) {
        Empleado empleado = (Empleado) usuario;
        Menu menu = new Menu("Menú del " + Formatos.nombreRol(empleado))
                .seccion("Empleado")
                .opcion("1", "Consultar mis turnos y solicitudes", () -> verTurnos(empleado))
                .opcion("2", "Solicitar cambio de turno", () -> solicitarCambioTurno(empleado))
                .opcion("3", "Comprar en la tienda", () -> comprar(empleado, "tienda"))
                .opcion("4", "Comprar en la cafetería", () -> comprar(empleado, "cafeteria"))
                .opcion("5", "Ver mi código de descuento", () -> verMiCodigo(empleado))
                .opcion("6", "Sugerir un producto", () -> sugerirProducto(empleado))
                .opcion("7", "Mis compras y puntos de fidelidad", () -> verCompras(empleado));
        if (empleado instanceof Entrenador) {
            Entrenador entrenador = (Entrenador) empleado;
            menu.seccion("Entrenamiento")
                .opcion("8", "Planear una sesión de entrenamiento", () -> planearSesion(entrenador))
                .opcion("9", "Registrar asistencia de una sesión finalizada", () -> registrarAsistencia(entrenador))
                .opcion("10", "Evaluar el desempeño de un jugador", () -> evaluarJugador(entrenador))
                .opcion("11", "Asignar nivel a un socio", () -> asignarNivel(entrenador))
                .opcion("12", "Registrar resultado de un partido de práctica", () -> registrarResultadoPractica(entrenador))
                .seccion("Partidos de equipos")
                .opcion("13", "Programar partido interno", () -> programarPartidoInterno(entrenador))
                .opcion("14", "Definir alineación (partido interno u oficial)", () -> definirAlineacion(entrenador))
                .opcion("15", "Registrar resultado de un partido interno", () -> registrarResultadoInterno(entrenador))
                .opcion("16", "Registrar resultado de un partido oficial a mi cargo", () -> registrarPartidoCompetencia(entrenador))
                .opcion("17", "Consultar analítica de un jugador", () -> consultarAnalitica(entrenador));
        }
        return menu;
    }

    private void verTurnos(Empleado empleado) {
        System.out.println("--- Mis turnos ---");
        if (empleado.consultarTurnos().isEmpty()) System.out.println("No tiene turnos programados.");
        for (Turno t : empleado.consultarTurnos()) System.out.println("  " + t);
        System.out.println("--- Mis solicitudes de cambio ---");
        if (empleado.getSolicitudes().isEmpty()) System.out.println("No tiene solicitudes de cambio.");
        for (SolicitudCambioTurno s : empleado.getSolicitudes())
            System.out.printf("  %s -> %s | %s%s%n", s.getTurnoActual(), s.getTurnoDeseado(), s.getEstado(),
                    s.getMotivoRechazo() == null ? "" : " (motivo: " + s.getMotivoRechazo() + ")");
    }

    private void solicitarCambioTurno(Empleado empleado) {
        System.out.println("Horario de atención del club: " + Turno.descripcionHorarioDeAtencion() + ".");
        Turno actual = elegir("Su turno a cambiar:", empleado.getTurnos(), Formatos::etiquetaTurno);
        DayOfWeek dia = dia("Día deseado (lunes..sábado): ");
        LocalTime ini = hora("Hora de inicio deseada (HH:mm): ");
        LocalTime fin = hora("Hora de fin deseada (HH:mm): ");
        empleado.solicitarCambioTurno(actual, new Turno(dia, ini, fin));
        System.out.println("Solicitud registrada. Queda pendiente de aprobación del administrador.");
    }

    private void verMiCodigo(Empleado empleado) {
        System.out.println("Su código de descuento: " + empleado.getCodigoDescuento() + (empleado.isCodigoActivo() ? " (activo)" : " (inactivo)"));
        System.out.println("Usted obtiene 15 % al usarlo en sus compras; cualquier otra persona que lo use obtiene 8 %.");
        System.out.println("El descuento no se acumula con puntos de fidelidad ni con bonos de torneo.");
    }

    private void sugerirProducto(Empleado empleado) {
        String tipo = texto("Tipo (tienda/bebida/snack): ").toLowerCase();
        String nombre = texto("Nombre: ");
        double precio = decimal("Precio: ");
        Producto p;
        switch (tipo) {
            case "tienda": p = new ProductoTienda(nombre, precio, texto("Categoría: ")); break;
            case "bebida": p = new Bebida(nombre, precio, confirmar("¿Es caliente?")); break;
            case "snack":
                String lista = textoOpcional("Alérgenos separados por coma (Enter si no tiene): ");
                List<String> alergenos = new ArrayList<>();
                if (!lista.isEmpty()) for (String a : lista.split(",")) alergenos.add(a.trim());
                p = new Snack(nombre, precio, alergenos);
                break;
            default: throw new IllegalArgumentException("Tipo de producto no reconocido.");
        }
        empleado.sugerirProducto(p);
        System.out.println("Sugerencia enviada al administrador.");
    }

    /** HU-T01: sesión de un equipo (cancha de conjunto) o de una academia (instalación de la disciplina). */
    private void planearSesion(Entrenador entrenador) {
        LocalDateTime fecha = fechaHora("Fecha y hora (AAAA-MM-DD HH:mm): ");
        int duracion = entrada.enteroEnRango("Duración en minutos: ", 1, 600);
        SesionEntrenamiento s;
        if (confirmar("¿Es una sesión de un equipo? (n = academia individual)")) {
            Equipo e = elegir("Equipo:", sistema.getEquiposActivos(), Formatos::etiquetaEquipo);
            List<Instalacion> canchas = new ArrayList<>();
            for (Instalacion i : sistema.getInstalaciones()) if (i instanceof CanchaConjunto) canchas.add(i);
            Instalacion inst = elegir("Instalación:", canchas, Instalacion::getNombre);
            s = entrenador.planearSesionEquipo(fecha, duracion, inst, e);
        } else {
            DisciplinaIndividual d = elegir("Disciplina:", sistema.getDisciplinas(), Formatos::nombreDisciplina);
            Instalacion inst = elegir("Instalación:", d.getInstalaciones(), Instalacion::getNombre);
            List<Socio> inscritos = new ArrayList<>();
            for (InscripcionDisciplina i : d.getInscripciones()) if (i.isActiva()) inscritos.add(i.getSocio());
            List<Socio> convocados = new ArrayList<>();
            do {
                List<Socio> restantes = new ArrayList<>(inscritos);
                restantes.removeAll(convocados);
                convocados.add(elegir("Convocar a:", restantes, x -> x.getNombre() + " (" + x.inscripcionEn(d).getNivel() + ")"));
            } while (convocados.size() < inscritos.size() && confirmar("¿Convocar a otro socio?"));
            s = entrenador.planearSesionAcademia(fecha, duracion, inst, d, convocados);
        }
        System.out.printf("Sesión planeada el %s en %s con %d convocado(s).%n", s.getFechaHora(), s.getInstalacion().getNombre(), s.getConvocados().size());
    }

    private List<SesionEntrenamiento> sesionesFinalizadas(Entrenador entrenador) {
        List<SesionEntrenamiento> lista = new ArrayList<>();
        for (SesionEntrenamiento s : entrenador.getSesiones()) if (s.haFinalizado(club.model.Reloj.ahora())) lista.add(s);
        return lista;
    }

    /** HU-T02 (y nota opcional de HU-T03) */
    private void registrarAsistencia(Entrenador entrenador) {
        SesionEntrenamiento sesion = elegir("Sus sesiones finalizadas:", sesionesFinalizadas(entrenador),
                s -> s.getFechaHora() + " en " + s.getInstalacion().getNombre() + " (" + s.getConvocados().size() + " convocados)");
        for (Socio socio : sesion.getConvocados()) {
            System.out.println("Convocado: " + Formatos.persona(socio));
            try {
                String estado = elegir("  Estado:", Arrays.asList("asistió", "faltó", "llegó tarde"), x -> x);
                Integer nota = null;
                String comentario = null;
                if (!"faltó".equals(estado) && confirmar("  ¿Registrar evaluación ahora?")) {
                    nota = entrada.enteroEnRango("  Nota de 1 a 5: ", 1, 5);
                    comentario = textoOpcional("  Comentario: ");
                }
                entrenador.registrarAsistencia(sesion, socio, estado, nota, comentario);
                System.out.println("  Registrado.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("  No se registró: " + e.getMessage());
            }
        }
    }

    /** HU-T03: evaluación posterior a la asistencia. */
    private void evaluarJugador(Entrenador entrenador) {
        SesionEntrenamiento sesion = elegir("Sus sesiones finalizadas:", sesionesFinalizadas(entrenador),
                s -> s.getFechaHora() + " en " + s.getInstalacion().getNombre());
        List<Socio> conAsistencia = new ArrayList<>();
        for (Asistencia a : sesion.getAsistencias()) if (!"faltó".equals(a.getEstado())) conAsistencia.add(a.getSocio());
        Socio socio = elegir("Jugadores que asistieron o llegaron tarde:", conAsistencia, Formatos::persona);
        entrenador.evaluar(sesion, socio, entrada.enteroEnRango("Nota de 1 a 5: ", 1, 5), textoOpcional("Comentario: "));
        System.out.println("Evaluación registrada.");
    }

    /** HU-T10 */
    private void registrarResultadoPractica(Entrenador entrenador) {
        List<PartidoPractica> pendientes = new ArrayList<>();
        for (Socio s : sistema.getSocios())
            for (PartidoPractica p : s.getPartidosPractica())
                if (!p.isResultadoRegistrado() && !pendientes.contains(p)) pendientes.add(p);
        PartidoPractica p = elegir("Partidos de práctica sin resultado:", pendientes, x -> Formatos.etiquetaPractica(x) + " | " + x.getFechaHora());
        boolean mesa = p.getDisciplina() instanceof TenisMesa;
        System.out.println(mesa ? "Tenis de mesa: al mejor de 5 sets." : "Tenis y pádel: al mejor de 3 sets.");
        int setsA = entrada.enteroEnRango("Sets ganados por " + nombres(p.getLadoA()) + ": ", 0, 3);
        int setsB = entrada.enteroEnRango("Sets ganados por " + nombres(p.getLadoB()) + ": ", 0, 3);
        List<EstadisticaIndividual> stats = new ArrayList<>();
        for (InscripcionDisciplina i : p.getParticipantes()) {
            int sets = p.getLadoA().contains(i) ? setsA : setsB;
            System.out.println("Estadísticas de " + i.getSocio().getNombre() + ":");
            if (mesa) stats.add(new EstadisticaTenisMesa(sets, entrada.enteroEnRango("  Puntos totales: ", 0, 999)));
            else stats.add(new EstadisticaTenisPadel(sets, entrada.enteroEnRango("  Games ganados: ", 0, 99),
                    entrada.enteroEnRango("  Aces: ", 0, 99), entrada.enteroEnRango("  Errores no forzados: ", 0, 999)));
        }
        entrenador.registrarResultadoPractica(p, stats);
        System.out.println("Resultado registrado. ELO actualizado:");
        for (InscripcionDisciplina i : p.getParticipantes()) System.out.printf("  %s: %d%n", i.getSocio().getNombre(), i.getRanking());
    }

    private static String nombres(List<InscripcionDisciplina> lado) {
        List<String> n = new ArrayList<>();
        for (InscripcionDisciplina i : lado) n.add(i.getSocio().getNombre());
        return String.join(" y ", n);
    }

    private void asignarNivel(Entrenador entrenador) {
        DisciplinaIndividual d = elegir("Disciplina:", sistema.getDisciplinas(), Formatos::nombreDisciplina);
        List<Socio> inscritos = new ArrayList<>();
        for (InscripcionDisciplina i : d.getInscripciones()) if (i.isActiva()) inscritos.add(i.getSocio());
        Socio socio = elegir("Socio:", inscritos, s -> s.getNombre() + " (" + s.getLogin() + ")");
        String nivel = elegir("Nivel:", Arrays.asList("Principiante", "Intermedio", "Avanzado"), x -> x);
        entrenador.asignarNivel(socio, d, nivel);
        System.out.println("Nivel actualizado a " + nivel + ".");
    }

    /** HU-T04 */
    private void programarPartidoInterno(Entrenador entrenador) {
        Equipo a = elegir("Equipo local:", sistema.getEquiposActivos(), Formatos::etiquetaEquipo);
        List<Equipo> rivales = new ArrayList<>();
        for (Equipo e : sistema.getEquiposActivos()) if (e != a && e.getDeporte() == a.getDeporte() && e.getCategoria() == a.getCategoria()) rivales.add(e);
        Equipo b = elegir("Equipo visitante (mismo deporte y categoría):", rivales, Formatos::etiquetaEquipo);
        PartidoInterno p = entrenador.programarPartidoInterno(a, b, fechaHora("Fecha y hora (AAAA-MM-DD HH:mm): "));
        System.out.printf("Partido interno programado: %s vs %s el %s.%n", a.getNombre(), b.getNombre(), p.getFechaHora());
    }

    /** HU-T05: la alineación se elige entre los jugadores activos; la lógica rechaza suspendidos y, en oficiales, a quien no esté en la nómina. */
    private void definirAlineacion(Entrenador entrenador) {
        if (confirmar("¿Es un partido interno? (n = partido oficial)")) {
            List<PartidoInterno> pendientes = new ArrayList<>();
            for (Equipo e : sistema.getEquiposActivos())
                for (PartidoInterno p : e.getPartidosInternos()) if (!p.isFinalizado() && !pendientes.contains(p)) pendientes.add(p);
            PartidoInterno p = elegir("Partidos internos sin resultado:", pendientes, x -> x.getEquipoLocal().getNombre() + " vs " + x.getEquipoVisitante().getNombre() + " | " + x.getFechaHora());
            Equipo equipo = elegir("Equipo:", Arrays.asList(p.getEquipoLocal(), p.getEquipoVisitante()), Equipo::getNombre);
            p.definirAlineacion(equipo, elegirJugadores(equipo));
            System.out.println("Alineación registrada: " + p.getAlineacion(equipo).size() + " jugadores.");
        } else {
            List<PartidoCompetencia> pendientes = new ArrayList<>();
            for (Competencia c : sistema.getCompetencias())
                for (PartidoCompetencia p : c.getPartidos()) if (p.isProgramado() && !p.isFinalizado()) pendientes.add(p);
            PartidoCompetencia p = elegir("Partidos oficiales programados:", pendientes, x -> x.getCompetencia().getNombre() + " | " + Formatos.nombresEquipos(x) + " | " + x.getFecha());
            List<Equipo> delClub = new ArrayList<>();
            for (EquipoCompetencia e : p.getEquipos()) if (e instanceof Equipo) delClub.add((Equipo) e);
            Equipo equipo = elegir("Equipo del club:", delClub, Equipo::getNombre);
            p.definirAlineacion(equipo, elegirJugadores(equipo));
            System.out.println("Alineación registrada: " + p.getAlineacion(equipo).size() + " jugadores.");
        }
    }

    private List<Socio> elegirJugadores(Equipo equipo) {
        System.out.println("Jugadores activos de " + equipo.getNombre() + " (mínimo " + equipo.getDeporte().getMinJugadores() + "):");
        List<Socio> elegidos = new ArrayList<>();
        for (InscripcionEquipo i : equipo.getInscripcionesActivas()) {
            String marca = i.getSocio().tieneSancionVigente() ? " [suspendido]" : "";
            if (confirmar("  ¿Alinear a #" + i.getNumeroCamiseta() + " " + i.getSocio().getNombre() + " (" + i.getPosicion() + ")" + marca + "?")) elegidos.add(i.getSocio());
        }
        return elegidos;
    }

    /** HU-T06, T07 y T08 */
    private void registrarResultadoInterno(Entrenador entrenador) {
        List<PartidoInterno> mios = new ArrayList<>();
        for (Equipo e : sistema.getEquipos())
            for (PartidoInterno p : e.getPartidosInternos())
                if (p.getEntrenador() == entrenador && !p.isFinalizado() && !p.getFechaHora().isAfter(club.model.Reloj.ahora()) && !mios.contains(p)) mios.add(p);
        PartidoInterno p = elegir("Partidos internos ya disputados que usted programó:", mios, x -> x.getEquipoLocal().getNombre() + " vs " + x.getEquipoVisitante().getNombre() + " | " + x.getFechaHora());
        int local = entrada.enteroEnRango("Marcador de " + p.getEquipoLocal().getNombre() + ": ", 0, 999);
        int visitante = entrada.enteroEnRango("Marcador de " + p.getEquipoVisitante().getNombre() + ": ", 0, 999);
        List<Socio> alineados = new ArrayList<>(p.getAlineacion(p.getEquipoLocal()));
        alineados.addAll(p.getAlineacion(p.getEquipoVisitante()));
        entrenador.registrarResultadoInterno(p, local, visitante, pedirEstadisticas(alineados, p.getEquipoLocal().getDeporte()));
        System.out.printf("Partido registrado: %s vs %s, marcador %s.%n", p.getEquipoLocal().getNombre(), p.getEquipoVisitante().getNombre(), p.getMarcadorFinal());
        for (Socio s : alineados) if (s.tieneSancionVigente()) System.out.println("Queda suspendido: " + s.getNombre());
    }

    private void registrarPartidoCompetencia(Entrenador entrenador) {
        List<PartidoCompetencia> aCargo = new ArrayList<>();
        for (Competencia c : sistema.getCompetencias())
            for (PartidoCompetencia p : c.getPartidos())
                if (p.isProgramado() && !p.isFinalizado() && p.getEntrenadorResponsable() == entrenador) aCargo.add(p);
        PartidoCompetencia p = elegir("Partidos oficiales a su cargo:", aCargo,
                x -> x.getCompetencia().getNombre() + " | " + x.getFecha() + " | " + Formatos.nombresEquipos(x) + " | " + x.getEscenario());
        Competencia c = p.getCompetencia();
        int local = entrada.enteroEnRango("Marcador de " + p.getEquipos().get(0).getNombre() + ": ", 0, Integer.MAX_VALUE);
        int visitante = entrada.enteroEnRango("Marcador de " + p.getEquipos().get(1).getNombre() + ": ", 0, Integer.MAX_VALUE);
        List<Socio> jugadores = new ArrayList<>();
        for (EquipoCompetencia e : p.getEquipos()) jugadores.addAll(p.getAlineacion(e));
        if (jugadores.isEmpty()) System.out.println("No hay alineación registrada; el resultado se guarda sin estadísticas individuales.");
        List<EstadisticaJugadorPartido> stats = pedirEstadisticas(jugadores, c.getDeporte());
        entrenador.registrarResultadoOficial(p, local, visitante);
        for (EstadisticaJugadorPartido e : stats) p.agregarEstadistica(e);
        if (local == visitante && p.getGrupo() == null && c.getFormato() instanceof FormatoPorFases) {
            EquipoCompetencia ganador = elegir("Empate en eliminación directa. ¿Quién avanza?", p.getEquipos(), EquipoCompetencia::getNombre);
            p.resolverEmpate(ganador);
        }
        System.out.printf("Partido registrado: %s, marcador %s.%n", Formatos.nombresEquipos(p), p.getMarcador());
        for (Socio s : jugadores) if (s.tieneSancionVigente()) System.out.println("Sanción vigente: " + s.getNombre());
    }

    /** Pide las estadísticas de los jugadores alineados (HU-T07); en fútbol incluye las tarjetas (HU-T08). */
    private List<EstadisticaJugadorPartido> pedirEstadisticas(List<Socio> jugadores, DeporteConjunto deporte) {
        List<EstadisticaJugadorPartido> stats = new ArrayList<>();
        for (Socio s : jugadores) {
            if (!confirmar("¿Registrar estadísticas de " + s.getNombre() + "?")) continue;
            if (deporte instanceof Futbol)
                stats.add(new EstadisticaJugadorFutbol(s, entrada.enteroEnRango("  Goles: ", 0, 99), entrada.enteroEnRango("  Asistencias: ", 0, 99),
                        entrada.enteroEnRango("  Tarjetas amarillas: ", 0, 2), entrada.enteroEnRango("  Tarjetas rojas: ", 0, 1)));
            else if (deporte instanceof Baloncesto)
                stats.add(new EstadisticaJugadorBaloncesto(s, entrada.enteroEnRango("  Puntos: ", 0, 999), entrada.enteroEnRango("  Rebotes: ", 0, 999)));
            else
                stats.add(new EstadisticaJugadorVoleibol(s, entrada.enteroEnRango("  Aces: ", 0, 999), entrada.enteroEnRango("  Bloqueos: ", 0, 999)));
        }
        return stats;
    }

    private void consultarAnalitica(Entrenador entrenador) {
        Socio socio = elegir("Jugador:", sistema.getSocios(), s -> s.getNombre() + " (" + s.getLogin() + ")");
        System.out.println(entrenador.consultarAnalitica(socio));
        verDesempeno(socio);
    }

    /** Mismo reporte de desempeño que ve el socio, usado en la analítica del entrenador. */
    private void verDesempeno(Socio socio) {
        VistaDesempeno.mostrar(socio);
    }
}
