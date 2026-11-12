package club.modelo.competencias;

import java.time.LocalDate;
import club.modelo.usuarios.Socio;

public class InscripcionEquipo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Socio socio;
    private Equipo equipo;
    private String posicion;
    private int numeroCamiseta;
    private LocalDate fechaInscripcion;
    private LocalDate fechaRetiro;

    public InscripcionEquipo(Socio socio, Equipo equipo, String posicion, int numeroCamiseta) {
        if (socio == null || equipo == null || posicion == null || posicion.trim().isEmpty() || numeroCamiseta <= 0) throw new IllegalArgumentException("Socio, equipo, posición y número válidos son obligatorios.");
        this.socio = socio;
        this.equipo = equipo;
        this.posicion = posicion;
        this.numeroCamiseta = numeroCamiseta;
        this.fechaInscripcion = LocalDate.now();
    }

    public Socio getSocio() { return socio; }
    public void setSocio(Socio socio) { this.socio = socio; }

    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }

    public String getPosicion() { return posicion; }
    public void setPosicion(String posicion) { this.posicion = posicion; }

    public int getNumeroCamiseta() { return numeroCamiseta; }
    public void setNumeroCamiseta(int numeroCamiseta) { this.numeroCamiseta = numeroCamiseta; }

    public LocalDate getFechaInscripcion() { return fechaInscripcion; }
    public LocalDate getFechaRetiro() { return fechaRetiro; }
    public boolean isActiva() { return fechaRetiro == null; }

    public void retirar(LocalDate fecha) {
        if (fechaRetiro == null) fechaRetiro = fecha == null ? LocalDate.now() : fecha;
    }
}
