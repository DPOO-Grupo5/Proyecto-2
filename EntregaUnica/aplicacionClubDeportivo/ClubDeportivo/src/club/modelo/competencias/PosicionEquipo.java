package club.modelo.competencias;

public class PosicionEquipo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int puntuacion;
    private int posicion;
    private int partidosJugados;
    private int victorias;
    private int empates;
    private int derrotas;
    private int tantosFavor;
    private int tantosContra;
    private EquipoCompetencia equipo;

    public PosicionEquipo(EquipoCompetencia equipo) {
        if (equipo == null) throw new IllegalArgumentException("El equipo es obligatorio.");
        this.equipo = equipo;
    }

    public PosicionEquipo(int puntuacion, int posicion) { this.puntuacion = puntuacion; this.posicion = posicion; }

    void registrarResultado(int favor, int contra, int puntosVictoria, int puntosEmpate, int puntosDerrota) {
        partidosJugados++;
        tantosFavor += favor;
        tantosContra += contra;
        if (favor > contra) { victorias++; puntuacion += puntosVictoria; }
        else if (favor < contra) { derrotas++; puntuacion += puntosDerrota; }
        else { empates++; puntuacion += puntosEmpate; }
    }

    public int getPuntuacion() { return puntuacion; }
    public void setPuntuacion(int puntuacion) { this.puntuacion = puntuacion; }
    public int getPosicion() { return posicion; }
    public void setPosicion(int posicion) { this.posicion = posicion; }
    public int getPartidosJugados() { return partidosJugados; }
    public int getVictorias() { return victorias; }
    public int getEmpates() { return empates; }
    public int getDerrotas() { return derrotas; }
    public int getTantosFavor() { return tantosFavor; }
    public int getTantosContra() { return tantosContra; }
    public EquipoCompetencia getEquipo() { return equipo; }
    public void setEquipo(EquipoCompetencia equipo) { if (equipo == null) throw new IllegalArgumentException("El equipo es obligatorio."); this.equipo = equipo; }
}
