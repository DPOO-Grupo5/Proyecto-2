package club.modelo.entrenamiento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import club.modelo.instalaciones.Reserva;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;

/**
 * Partido de práctica entre socios de una academia, jugado sobre una reserva (HU-S04).
 * Los participantes forman dos lados: en sencillos, uno contra uno; en dobles, los dos primeros contra los dos
 * últimos. El resultado se registra por sets (tenis y pádel al mejor de 3, tenis de mesa al mejor de 5) y actualiza
 * el ELO de cada participante frente al promedio del lado rival (HU-T10).
 */
public class PartidoPractica implements java.io.Serializable {
    private static final long serialVersionUID = 2L;

    private String modalidad;
    private Reserva reserva;
    private DisciplinaIndividual disciplina;
    private List<EstadisticaIndividual> estadisticas = new ArrayList<>();
    private List<InscripcionDisciplina> participantes = new ArrayList<>();
    /** Entrenador que participa y habilita niveles no adyacentes (excepción de HU-S04). No ocupa cupo. */
    private Entrenador entrenadorParticipante;
    private boolean resultadoRegistrado;
    private int setsLadoA;
    private int setsLadoB;

    public PartidoPractica(Reserva reserva, List<InscripcionDisciplina> participantes, Entrenador entrenadorParticipante) {
        if (reserva == null) throw new IllegalArgumentException("El partido de práctica requiere una reserva.");
        if (participantes == null || participantes.isEmpty()) throw new IllegalArgumentException("Debe indicar los participantes.");
        this.reserva = reserva;
        this.disciplina = reserva.getInstalacion().getDisciplina();
        this.modalidad = reserva.getModalidad();
        if (disciplina == null) throw new IllegalArgumentException("La instalación reservada no pertenece a una academia individual.");
        if (participantes.size() != reserva.getCantidadJugadores())
            throw new IllegalArgumentException("La reserva es para " + reserva.getCantidadJugadores() + " jugadores y se indicaron " + participantes.size() + ".");
        Set<Socio> distintos = new HashSet<>();
        for (InscripcionDisciplina i : participantes) {
            if (i == null || !i.isActiva() || i.getDisciplina() != disciplina)
                throw new IllegalArgumentException("Todos los participantes deben estar inscritos en " + disciplina.getClass().getSimpleName() + ".");
            if (!distintos.add(i.getSocio())) throw new IllegalArgumentException("El participante " + i.getSocio().getNombre() + " está repetido.");
        }
        if (entrenadorParticipante == null) {
            for (InscripcionDisciplina x : participantes)
                for (InscripcionDisciplina y : participantes)
                    if (!x.puedeJugarCon(y))
                        throw new IllegalArgumentException("Los niveles " + x.getNivel() + " y " + y.getNivel()
                                + " no son iguales ni adyacentes; se requiere un entrenador entre los participantes.");
        }
        this.participantes.addAll(participantes);
        this.entrenadorParticipante = entrenadorParticipante;
    }

    public List<InscripcionDisciplina> getLadoA() { return participantes.subList(0, participantes.size() / 2); }
    public List<InscripcionDisciplina> getLadoB() { return participantes.subList(participantes.size() / 2, participantes.size()); }

    private int setsParaGanar() { return disciplina instanceof TenisMesa ? 3 : 2; }

    /**
     * Registra el resultado con una estadística por participante, en el mismo orden de {@link #getParticipantes()}.
     * Los compañeros de un mismo lado deben tener los mismos sets y el marcador debe ser posible para la disciplina.
     */
    public void registrarResultado(List<EstadisticaIndividual> stats) {
        if (resultadoRegistrado) throw new IllegalStateException("El resultado ya fue registrado.");
        if (stats == null || stats.size() != participantes.size())
            throw new IllegalArgumentException("Se requiere una estadística por cada uno de los " + participantes.size() + " participantes.");
        for (EstadisticaIndividual e : stats) {
            boolean tipoCorrecto = (disciplina instanceof TenisMesa && e instanceof EstadisticaTenisMesa)
                    || ((disciplina instanceof Tenis || disciplina instanceof Padel) && e instanceof EstadisticaTenisPadel);
            if (!tipoCorrecto) throw new IllegalArgumentException("Las estadísticas no corresponden a la disciplina " + disciplina.getClass().getSimpleName() + ".");
        }
        int mitad = participantes.size() / 2;
        int a = stats.get(0).getSets();
        int b = stats.get(mitad).getSets();
        for (int i = 0; i < stats.size(); i++)
            if (stats.get(i).getSets() != (i < mitad ? a : b)) throw new IllegalArgumentException("Los compañeros de un mismo lado deben tener los mismos sets.");
        int n = setsParaGanar();
        if (Math.max(a, b) != n || Math.min(a, b) >= n)
            throw new IllegalArgumentException("Marcador de sets imposible (" + a + "-" + b + ") en un partido al mejor de " + (2 * n - 1) + ".");

        double eloA = promedioElo(getLadoA());
        double eloB = promedioElo(getLadoB());
        for (int i = 0; i < stats.size(); i++) {
            InscripcionDisciplina insc = participantes.get(i);
            EstadisticaIndividual e = stats.get(i);
            e.setInscripcion(insc);
            estadisticas.add(e);
            insc.getSocio().registrarEstadisticaIndividual(e);
        }
        for (InscripcionDisciplina i : getLadoA()) i.actualizarRanking(a > b ? 1.0 : 0.0, (int) Math.round(eloB));
        for (InscripcionDisciplina i : getLadoB()) i.actualizarRanking(b > a ? 1.0 : 0.0, (int) Math.round(eloA));
        setsLadoA = a;
        setsLadoB = b;
        resultadoRegistrado = true;
    }

    private static double promedioElo(List<InscripcionDisciplina> lado) {
        double suma = 0;
        for (InscripcionDisciplina i : lado) suma += i.getRanking();
        return suma / lado.size();
    }

    public boolean incluye(Socio socio) {
        for (InscripcionDisciplina i : participantes) if (i.getSocio() == socio) return true;
        return false;
    }

    private boolean estaEnLadoA(Socio socio) {
        for (InscripcionDisciplina i : getLadoA()) if (i.getSocio() == socio) return true;
        return false;
    }

    public List<Socio> oponentesDe(Socio socio) {
        List<Socio> oponentes = new ArrayList<>();
        for (InscripcionDisciplina i : estaEnLadoA(socio) ? getLadoB() : getLadoA()) oponentes.add(i.getSocio());
        return oponentes;
    }

    public boolean ganoLadoA() { return resultadoRegistrado && setsLadoA > setsLadoB; }

    /** Resultado desde el punto de vista del socio, por ejemplo "Ganó 2-1"; "Pendiente" si no hay resultado. */
    public String resultadoPara(Socio socio) {
        if (!resultadoRegistrado) return "Pendiente";
        boolean ladoA = estaEnLadoA(socio);
        int propios = ladoA ? setsLadoA : setsLadoB;
        int rival = ladoA ? setsLadoB : setsLadoA;
        return (propios > rival ? "Ganó " : "Perdió ") + propios + "-" + rival;
    }

    public LocalDateTime getFechaHora() { return reserva == null ? null : reserva.getFechaHoraInicio(); }
    public LocalDateTime getFechaHoraFin() { return reserva == null ? null : reserva.getFechaHoraFin(); }
    public String getModalidad() { return modalidad; }
    public Reserva getReserva() { return reserva; }
    public DisciplinaIndividual getDisciplina() { return disciplina; }
    public List<EstadisticaIndividual> getEstadisticas() { return estadisticas; }
    public List<InscripcionDisciplina> getParticipantes() { return participantes; }
    public Entrenador getEntrenadorParticipante() { return entrenadorParticipante; }
    public boolean isResultadoRegistrado() { return resultadoRegistrado; }
    public int getSetsLadoA() { return setsLadoA; }
    public int getSetsLadoB() { return setsLadoB; }
}
