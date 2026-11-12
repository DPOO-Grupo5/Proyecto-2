package club.modelo.competencias;

import club.modelo.usuarios.Socio;

public class EstadisticaJugadorBaloncesto extends EstadisticaJugadorPartido implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int puntos;
    private int rebotes;

    public EstadisticaJugadorBaloncesto(int puntos, int rebotes) { setPuntos(puntos); setRebotes(rebotes); }
    public EstadisticaJugadorBaloncesto(Socio jugador, int puntos, int rebotes) { super(jugador); setPuntos(puntos); setRebotes(rebotes); }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { if (puntos < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos."); this.puntos = puntos; }
    public int getRebotes() { return rebotes; }
    public void setRebotes(int rebotes) { if (rebotes < 0) throw new IllegalArgumentException("Los rebotes no pueden ser negativos."); this.rebotes = rebotes; }
}
