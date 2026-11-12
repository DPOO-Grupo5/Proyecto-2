package club.modelo.competencias;

import club.modelo.usuarios.Socio;

public class EstadisticaJugadorFutbol extends EstadisticaJugadorPartido implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int goles;
    private int asistencias;
    private int tarjetasAmarillas;
    private int tarjetasRojas;
    private Sancion sancion;

    public EstadisticaJugadorFutbol(int goles, int asistencias, int tarjetasAmarillas, int tarjetasRojas) {
        setGoles(goles); setAsistencias(asistencias); setTarjetasAmarillas(tarjetasAmarillas); setTarjetasRojas(tarjetasRojas);
    }

    public EstadisticaJugadorFutbol(Socio jugador, int goles, int asistencias, int tarjetasAmarillas, int tarjetasRojas) {
        super(jugador);
        setGoles(goles); setAsistencias(asistencias); setTarjetasAmarillas(tarjetasAmarillas); setTarjetasRojas(tarjetasRojas);
    }

    private void validarNoNegativo(int valor) { if (valor < 0) throw new IllegalArgumentException("Las estadísticas no pueden ser negativas."); }
    public int getGoles() { return goles; }
    public void setGoles(int goles) { validarNoNegativo(goles); this.goles = goles; }
    public int getAsistencias() { return asistencias; }
    public void setAsistencias(int asistencias) { validarNoNegativo(asistencias); this.asistencias = asistencias; }
    public int getTarjetasAmarillas() { return tarjetasAmarillas; }
    public void setTarjetasAmarillas(int tarjetasAmarillas) { validarNoNegativo(tarjetasAmarillas); this.tarjetasAmarillas = tarjetasAmarillas; }
    public int getTarjetasRojas() { return tarjetasRojas; }
    public void setTarjetasRojas(int tarjetasRojas) { validarNoNegativo(tarjetasRojas); this.tarjetasRojas = tarjetasRojas; }
    public Sancion getSancion() { return sancion; }
    public void setSancion(Sancion sancion) { this.sancion = sancion; }
}
