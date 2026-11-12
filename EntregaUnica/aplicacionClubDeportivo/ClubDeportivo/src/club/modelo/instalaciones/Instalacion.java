package club.modelo.instalaciones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.SesionEntrenamiento;

public abstract class Instalacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int jugadoresMin;
    protected int jugadoresMax;
    protected String nombre;
    protected boolean techada;
    private DisciplinaIndividual disciplina;
    private List<Reserva> reservas = new ArrayList<>();
    private List<SesionEntrenamiento> sesiones = new ArrayList<>();

    public Instalacion(int jugadoresMin, int jugadoresMax, String nombre, boolean techada) {
        if (jugadoresMin <= 0 || jugadoresMax < jugadoresMin || nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("Nombre y cupos válidos son obligatorios.");
        this.jugadoresMin = jugadoresMin;
        this.jugadoresMax = jugadoresMax;
        this.nombre = nombre.trim();
        this.techada = techada;
    }

    // --- MÉTODOS ABSTRACTOS Y DE LÓGICA ---

    
    public abstract boolean admiteJugadores(int n, String modalidad);

    
    public boolean estaDisponible(LocalDateTime ini, LocalDateTime fin, List<Reserva> reservasGlobales) {
        List<Reserva> todas = reservasGlobales == null ? reservas : reservasGlobales;
        for (Reserva r : todas) {
            if (r.getInstalacion() != null && r.getInstalacion().equals(this) && r.seTraslapaCon(ini, fin)) {
                return false; // Hay cruce de horario, no está disponible
            }
        }
        for (SesionEntrenamiento sesion : sesiones) {
            if (sesion.seTraslapaCon(ini, fin)) return false;
        }
        return true;
    }

    public void registrarReserva(Reserva reserva) {
        if (reserva != null && !reservas.contains(reserva)) reservas.add(reserva);
    }

    public void registrarSesion(SesionEntrenamiento sesion) {
        if (sesion != null && !sesiones.contains(sesion)) sesiones.add(sesion);
    }

    public DisciplinaIndividual getDisciplina() { return disciplina; }
    public void setDisciplina(DisciplinaIndividual disciplina) {
        if (disciplina == null) throw new IllegalArgumentException("La disciplina es obligatoria.");
        if (this.disciplina != null && this.disciplina != disciplina) throw new IllegalStateException("La instalación ya pertenece a otra disciplina.");
        this.disciplina = disciplina;
        if (!disciplina.getInstalaciones().contains(this)) disciplina.registrarInstalacion(this);
    }
    public List<Reserva> getReservas() { return reservas; }
    public List<SesionEntrenamiento> getSesiones() { return sesiones; }

    // --- GETTERS (Se eliminaron la mayoría de setters por encapsulamiento) ---

    public int getJugadoresMin() { return jugadoresMin; }

    public int getJugadoresMax() { return jugadoresMax; }

    public String getNombre() { return nombre; }

    public boolean isTechada() { return techada; }

    public void setTechada(boolean techada) { this.techada = techada; }
}
