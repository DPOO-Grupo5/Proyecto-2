package club.modelo.competencias;

import club.modelo.usuarios.Socio;

public class Sancion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String motivo;
    private int partidosSuspension;
    private boolean cumplida;
    private Socio socio;

    public Sancion(String motivo, int partidosSuspension, boolean cumplida) {
        this(motivo, partidosSuspension, cumplida, null);
    }

    public Sancion(String motivo, int partidosSuspension, boolean cumplida, Socio socio) {
        if (motivo == null || motivo.trim().isEmpty() || partidosSuspension < 0) throw new IllegalArgumentException("Motivo y cantidad de partidos válidos son obligatorios.");
        this.motivo = motivo.trim();
        this.partidosSuspension = cumplida ? 0 : partidosSuspension;
        this.cumplida = cumplida || partidosSuspension == 0;
        this.socio = socio;
    }

    public boolean estaVigente() { return !cumplida && partidosSuspension > 0; }

    public void consumirPartido() {
        if (!estaVigente()) return;
        partidosSuspension--;
        if (partidosSuspension == 0) cumplida = true;
    }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { if (motivo == null || motivo.trim().isEmpty()) throw new IllegalArgumentException("El motivo es obligatorio."); this.motivo = motivo.trim(); }
    public int getPartidosSuspension() { return partidosSuspension; }
    public void setPartidosSuspension(int partidosSuspension) { if (partidosSuspension < 0) throw new IllegalArgumentException("La cantidad no puede ser negativa."); this.partidosSuspension = partidosSuspension; if (partidosSuspension == 0) cumplida = true; }
    public boolean isCumplida() { return cumplida; }
    public void setCumplida(boolean cumplida) { this.cumplida = cumplida; if (cumplida) partidosSuspension = 0; }
    public Socio getSocio() { return socio; }
    public void setSocio(Socio socio) { this.socio = socio; }
}
