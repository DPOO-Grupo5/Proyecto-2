package club.modelo.tienda;

import java.time.LocalDateTime;
import club.modelo.usuarios.Socio;

public class Bebida extends ProductoCafeteria implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private boolean esCaliente;

    public Bebida(String nombre, double precio, boolean esCaliente) {
        super(nombre, precio);
        this.esCaliente = esCaliente;
    }

    public boolean esCaliente() { return esCaliente; }

    public boolean puedeDespacharseA(Socio socio, LocalDateTime ahora) {
        if (esCaliente && ahora == null) throw new IllegalArgumentException("La fecha de validación es obligatoria.");
        return !esCaliente || socio == null || !socio.estaEnZonaRestringidaParaBebidasCalientes(ahora);
    }

    public boolean isEsCaliente() { return esCaliente; }
    public void setEsCaliente(boolean esCaliente) { this.esCaliente = esCaliente; }
}
