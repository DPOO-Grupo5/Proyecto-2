package club.modelo.competencias;

import club.modelo.usuarios.Socio;

public abstract class EstadisticaJugadorPartido implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Socio jugador;
    private PartidoInterno partidoInterno;
    private PartidoCompetencia partidoCompetencia;

    protected EstadisticaJugadorPartido() { }
    protected EstadisticaJugadorPartido(Socio jugador) { setJugador(jugador); }

    public Socio getJugador() { return jugador; }
    public void setJugador(Socio jugador) {
        if (jugador == null) throw new IllegalArgumentException("El jugador es obligatorio.");
        if (this.jugador != null && this.jugador != jugador) throw new IllegalStateException("La estadística ya está asociada a otro jugador.");
        this.jugador = jugador;
    }
    public PartidoInterno getPartidoInterno() { return partidoInterno; }
    public void setPartidoInterno(PartidoInterno partidoInterno) { if (this.partidoInterno != null && this.partidoInterno != partidoInterno) throw new IllegalStateException("La estadística ya pertenece a otro partido interno."); this.partidoInterno = partidoInterno; }
    public PartidoCompetencia getPartidoCompetencia() { return partidoCompetencia; }
    public void setPartidoCompetencia(PartidoCompetencia partidoCompetencia) { if (this.partidoCompetencia != null && this.partidoCompetencia != partidoCompetencia) throw new IllegalStateException("La estadística ya pertenece a otra competencia."); this.partidoCompetencia = partidoCompetencia; }
}
