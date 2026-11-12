package club.modelo.tienda;

import java.time.LocalDateTime;
import java.time.YearMonth;

import club.modelo.usuarios.Socio;

/** Pago de la mensualidad de un socio para un mes, registrado por el administrador en el rubro mensualidades (HU-A16). */
public class PagoMensualidad implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public static final String RUBRO = "mensualidad";

    private Socio socio;
    // Se guardan año y mes por separado porque la persistencia en CSV no maneja YearMonth.
    private int anio;
    private int mes;
    private double valor;
    private LocalDateTime fechaRegistro;

    public PagoMensualidad(Socio socio, YearMonth mes, double valor) {
        if (socio == null) throw new IllegalArgumentException("El socio es obligatorio.");
        if (mes == null) throw new IllegalArgumentException("El mes es obligatorio.");
        if (!(valor > 0) || Double.isInfinite(valor)) throw new IllegalArgumentException("El valor de la mensualidad debe ser mayor que cero.");
        this.socio = socio;
        this.anio = mes.getYear();
        this.mes = mes.getMonthValue();
        this.valor = valor;
        this.fechaRegistro = LocalDateTime.now();
    }

    public boolean corresponde(Socio otro, YearMonth otroMes) {
        return socio == otro && getMes().equals(otroMes);
    }

    public Socio getSocio() { return socio; }
    public YearMonth getMes() { return YearMonth.of(anio, mes); }
    public double getValor() { return valor; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public String getRubro() { return RUBRO; }
}
