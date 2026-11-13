package club.modelo.competencias;

import java.util.ArrayList;
import java.util.List;

import club.modelo.usuarios.Socio;

public class Equipo extends EquipoCompetencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int cupoMaximo;
    private boolean activo = true; 

    private Categoria categoria;
    private DeporteConjunto deporte;
    private List<InscripcionEquipo> inscripciones = new ArrayList<>();
    private List<PartidoInterno> partidosInternos = new ArrayList<>();

    public Equipo(String nombre, int cupoMaximo) {
        super(nombre);
        if (cupoMaximo <= 0) throw new IllegalArgumentException("El cupo debe ser positivo.");
        this.cupoMaximo = cupoMaximo;
    }

    public Equipo(String nombre, DeporteConjunto deporte, Categoria categoria, int cupoMaximo) {
        this(nombre, cupoMaximo);
        setDeporte(deporte);
        setCategoria(categoria);
        if (deporte != null && cupoMaximo < deporte.getMinJugadores())
            throw new IllegalArgumentException("El cupo es menor al mínimo de habilitados del deporte (" + deporte.getMinJugadores() + ").");
    }
    
    public int[] resumenResultados() { 
        int[] r = new int[6];
        for (PartidoInterno p : getPartidosInternos()) {
            if (!p.isFinalizado()) continue;
            boolean local = p.getEquipoLocal() == this;
            int a = local ? p.getMarcadorLocal() : p.getMarcadorVisitante();
            int b = local ? p.getMarcadorVisitante() : p.getMarcadorLocal();
            r[0]++; r[4] += a; r[5] += b;
            if (a > b) r[1]++; else if (a == b) r[2]++; else r[3]++;
        }
        return r;
    }

    public void darDeBaja() {
        if (!activo) throw new IllegalStateException("El equipo ya está dado de baja.");
        for (PartidoInterno p : getPartidosInternos())
            if (!p.isFinalizado()) throw new IllegalStateException("El equipo tiene partidos pendientes.");
        for (InscripcionEquipo i : inscripcionesActivas()) i.retirar(java.time.LocalDate.now());
        this.activo = false;
    }
    
    public InscripcionEquipo inscribirJugador(Socio socio, String posicion, int numeroCamiseta) {
        if (!activo) throw new IllegalStateException("No se pueden inscribir jugadores en un equipo dado de baja.");
        if (numeroCamiseta <= 0) throw new IllegalArgumentException("El número de camiseta debe ser positivo.");
        if (socio == null || deporte == null || categoria == null) {
            throw new IllegalStateException("El socio, deporte y categoría son obligatorios.");
        }
        if (inscripcionesActivas().size() >= cupoMaximo) throw new IllegalStateException("El equipo alcanzó su cupo máximo.");
        if (!deporte.posicionValida(posicion)) throw new IllegalArgumentException("La posición no corresponde al deporte.");
        int edad = socio.calcularEdad(java.time.LocalDate.now());
        if (edad < categoria.getEdadMinima() || edad > categoria.getEdadMaxima()) {
            throw new IllegalArgumentException("La edad del socio no corresponde a la categoría.");
        }
        for (InscripcionEquipo existente : inscripciones) {
            if (existente.isActiva() && existente.getNumeroCamiseta() == numeroCamiseta) {
                throw new IllegalArgumentException("El número de camiseta ya está ocupado en este equipo.");
            }
        }
        for (InscripcionEquipo existente : socio.getInscripcionesEquipo()) {
            if (existente.isActiva() && existente.getEquipo().getDeporte() == deporte) {
                throw new IllegalArgumentException("El socio ya pertenece a un equipo de este deporte.");
            }
        }
        InscripcionEquipo inscripcion = new InscripcionEquipo(socio, this, posicion, numeroCamiseta);
        this.inscripciones.add(inscripcion);
        socio.registrarInscripcionEquipo(inscripcion);
        return inscripcion;
    }

    private List<InscripcionEquipo> inscripcionesActivas() {
        List<InscripcionEquipo> activas = new ArrayList<>();
        for (InscripcionEquipo inscripcion : inscripciones) if (inscripcion.isActiva()) activas.add(inscripcion);
        return activas;
    }

    public List<InscripcionEquipo> getInscripcionesActivas() { return inscripcionesActivas(); }

    /** Retira al jugador: libera su cupo y su camiseta y conserva el historial de la inscripción. */
    public void retirarJugador(Socio socio) {
        for (InscripcionEquipo inscripcion : inscripciones) {
            if (inscripcion.isActiva() && inscripcion.getSocio() == socio) {
                inscripcion.retirar(java.time.LocalDate.now());
                return;
            }
        }
        throw new IllegalStateException("El jugador no tiene una inscripción activa en " + getNombre() + " (ya fue dado de baja o nunca perteneció).");
    }

    public int getCupoMaximo() { return cupoMaximo; }

    public void setCupoMaximo(int cupoMaximo) { if (cupoMaximo <= 0 || cupoMaximo < inscripcionesActivas().size()) throw new IllegalArgumentException("El cupo debe ser positivo y cubrir las inscripciones activas."); this.cupoMaximo = cupoMaximo; }

    public boolean isActivo() { return activo; }

    public void setActivo(boolean activo) { this.activo = activo; }

    public Categoria getCategoria() { return categoria; }

    public void setCategoria(Categoria categoria) {
        if (categoria == null) throw new IllegalArgumentException("La categoría es obligatoria.");
        for (InscripcionEquipo inscripcion : inscripcionesActivas()) {
            int edad = inscripcion.getSocio().calcularEdad(java.time.LocalDate.now());
            if (edad < categoria.getEdadMinima() || edad > categoria.getEdadMaxima()) throw new IllegalStateException("La categoría nueva no corresponde a un jugador inscrito.");
        }
        this.categoria = categoria;
    }

    public DeporteConjunto getDeporte() { return deporte; }

    public List<InscripcionEquipo> getInscripciones() { return inscripciones; }

    public void setInscripciones(List<InscripcionEquipo> inscripciones) { this.inscripciones = inscripciones; }

    public List<PartidoInterno> getPartidosInternos() { return partidosInternos; }

    public void setPartidosInternos(List<PartidoInterno> partidosInternos) { this.partidosInternos = partidosInternos; }

    public void setDeporte(DeporteConjunto deporte) {
        if (deporte == null) throw new IllegalArgumentException("El deporte es obligatorio.");
        for (InscripcionEquipo inscripcion : inscripcionesActivas()) {
            if (!deporte.posicionValida(inscripcion.getPosicion())) throw new IllegalStateException("El deporte nuevo no admite la posición de un jugador inscrito.");
        }
        this.deporte = deporte;
    }
}
