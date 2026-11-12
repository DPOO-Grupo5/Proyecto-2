package club.modelo.competencias;

import club.modelo.usuarios.Socio;

public class EstadisticaJugadorVoleibol extends EstadisticaJugadorPartido implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int aces;
    private int bloqueos;

    public EstadisticaJugadorVoleibol(int aces, int bloqueos) { setAces(aces); setBloqueos(bloqueos); }
    public EstadisticaJugadorVoleibol(Socio jugador, int aces, int bloqueos) { super(jugador); setAces(aces); setBloqueos(bloqueos); }
    public int getAces() { return aces; }
    public void setAces(int aces) { if (aces < 0) throw new IllegalArgumentException("Los aces no pueden ser negativos."); this.aces = aces; }
    public int getBloqueos() { return bloqueos; }
    public void setBloqueos(int bloqueos) { if (bloqueos < 0) throw new IllegalArgumentException("Los bloqueos no pueden ser negativos."); this.bloqueos = bloqueos; }
}
