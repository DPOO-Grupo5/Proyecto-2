package club.modelo.tienda;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Usuario;

/**
 * Compra de un usuario en la tienda o la cafetería.
 *
 * Cálculo: subtotal − descuento por código = base; impuestos sobre la base (IVA 19 % en tienda, impuesto al
 * consumo 8 % en cafetería); los puntos redimidos restan $1 cada uno; la propina se calcula sobre la base y
 * se suma al final. Una compra admite a lo sumo una promoción ({@link TipoPromocion}).
 */
public class Transaccion implements java.io.Serializable {
    private static final long serialVersionUID = 2L;
    public static final double TASA_PUNTOS = 0.02;

    private LocalDateTime fechaHora;
    private String rubro;
    private double subtotal;
    /** Descuento por código de empleado, sobre el subtotal antes de impuestos. */
    private double descuento;
    private double porcentajeCodigo;
    private TipoPromocion promocion;
    private Empleado duenoCodigo;
    private double impuestos;
    private double porcentajePropina;
    private double propina;
    private double total;
    private int puntosFidelidad;
    private int puntosRedimidos;
    private boolean confirmada;
    private Usuario comprador;
    private List<DetalleTransaccion> detalles = new ArrayList<>();

    public Transaccion() { this(LocalDateTime.now(), "general"); }

    public Transaccion(LocalDateTime fechaHora, String rubro) {
        if (fechaHora == null || rubro == null || rubro.trim().isEmpty()) throw new IllegalArgumentException("La fecha y el rubro son obligatorios.");
        this.fechaHora = fechaHora;
        this.rubro = rubro.trim().toLowerCase();
    }

    private void validarPendiente() {
        if (confirmada) throw new IllegalStateException("La transacción ya fue confirmada.");
    }

    public DetalleTransaccion agregarDetalle(Producto producto, int cantidad) {
        validarPendiente();
        DetalleTransaccion detalle = new DetalleTransaccion(producto, cantidad);
        detalles.add(detalle);
        recalcular();
        return detalle;
    }

    // ===================== PROMOCIONES (no acumulables) =====================
    private void validarSinPromocion() {
        if (promocion != null)
            throw new IllegalStateException("Las promociones no se acumulan: esta compra ya tiene " + describir(promocion) + ".");
    }

    private static String describir(TipoPromocion p) {
        switch (p) {
            case CODIGO_PROPIO: return "el descuento de empleado";
            case CODIGO_COMPARTIDO: return "el descuento por código de empleado";
            case PUNTOS: return "puntos redimidos";
            default: return "un bono de torneo";
        }
    }

    /**
     * Aplica el código de descuento de un empleado (HU-S17, HU-E06): 15 % si el comprador es el dueño del código,
     * 8 % para cualquier otra persona. Rechaza códigos inexistentes o inactivos y la combinación con otra promoción.
     */
    public void aplicarCodigoDescuento(String codigo, Collection<? extends Empleado> empleados) {
        validarPendiente();
        validarSinPromocion();
        if (codigo == null || codigo.trim().isEmpty()) throw new IllegalArgumentException("El código de descuento es obligatorio.");
        Empleado dueno = null;
        if (empleados != null) for (Empleado e : empleados) if (e.validarCodigo(codigo.trim())) dueno = e;
        if (dueno == null || !dueno.isCodigoActivo()) throw new IllegalArgumentException("El código de descuento no existe o no está activo.");
        boolean propio = comprador == dueno;
        porcentajeCodigo = propio ? dueno.getPorcentajeDescuentoPropio() : dueno.getPorcentajeDescuentoCompartido();
        promocion = propio ? TipoPromocion.CODIGO_PROPIO : TipoPromocion.CODIGO_COMPARTIDO;
        duenoCodigo = dueno;
        recalcular();
    }

    /** Redime puntos de fidelidad (HU-S16): cada punto descuenta $1 del total, sin superar el saldo ni el total. */
    public void redimirPuntos(int puntos) {
        validarPendiente();
        validarSinPromocion();
        if (comprador == null) throw new IllegalStateException("La compra debe tener un comprador.");
        int saldo = comprador.getPuntosFidelidad();
        if (puntos <= 0) throw new IllegalArgumentException("La cantidad de puntos debe ser positiva. Saldo disponible: " + saldo + " puntos.");
        if (puntos > saldo) throw new IllegalArgumentException("No tiene puntos suficientes. Saldo disponible: " + saldo + " puntos.");
        double tope = totalAntesDePuntos();
        if (puntos > tope) throw new IllegalArgumentException(String.format(
                "No puede redimir más puntos que el total de la compra ($%.0f). Saldo disponible: %d puntos.", tope, saldo));
        puntosRedimidos = puntos;
        promocion = TipoPromocion.PUNTOS;
        recalcular();
    }

    // ===================== CÁLCULOS =====================
    public double calcularSubtotal() {
        subtotal = 0.0;
        for (DetalleTransaccion detalle : detalles) subtotal += detalle.calcularSubtotal();
        return subtotal;
    }

    private double tasaImpuesto() {
        if ("tienda".equals(rubro)) return 0.19;
        if ("cafeteria".equals(rubro)) return 0.08;
        if ("general".equals(rubro) || "mensualidad".equals(rubro)) return 0.0;
        throw new IllegalArgumentException("El rubro no está reconocido.");
    }

    private double base() {
        return Math.max(0.0, calcularSubtotal() - descuento);
    }

    private double totalAntesDePuntos() {
        return base() + calcularImpuestos();
    }

    private void recalcular() {
        descuento = calcularSubtotal() * porcentajeCodigo;
        calcularImpuestos();
        propina = base() * porcentajePropina / 100.0;
        total = Math.max(0.0, totalAntesDePuntos() - puntosRedimidos) + propina;
    }

    public double calcularImpuestos() {
        impuestos = Math.max(0.0, calcularSubtotal() - descuento) * tasaImpuesto();
        return impuestos;
    }

    /** Fija la propina como porcentaje de la base (HU-S14: sugerida 10 %, modificable, incluso 0). */
    public double calcularPropina(double porcentaje) {
        validarPendiente();
        if (porcentaje < 0 || porcentaje > 100 || Double.isNaN(porcentaje)) throw new IllegalArgumentException("El porcentaje de propina debe estar entre 0 y 100.");
        porcentajePropina = porcentaje;
        recalcular();
        return propina;
    }

    /** Puntos que genera la compra (HU-S15): 2 % del total pagado sin propina, truncado a entero. */
    public int calcularPuntosGenerados() {
        double pagadoSinPropina = Math.max(0.0, totalAntesDePuntos() - puntosRedimidos);
        puntosFidelidad = (int) Math.floor(pagadoSinPropina * TASA_PUNTOS + 1e-9);
        return puntosFidelidad;
    }

    public void confirmar() {
        validarPendiente();
        if (comprador == null) throw new IllegalStateException("La transacción debe tener un comprador.");
        if (detalles.isEmpty()) throw new IllegalStateException("La transacción no tiene productos.");
        recalcular();
        validarProductosDelRubro();
        validarExistencias();
        if (contieneBebidaCaliente() && comprador instanceof Socio
                && ((Socio) comprador).estaEnZonaRestringidaParaBebidasCalientes(fechaHora)) {
            throw new IllegalStateException("No se puede vender una bebida caliente para consumo en esta instalación.");
        }
        if (puntosRedimidos > comprador.getPuntosFidelidad()) throw new IllegalStateException("El saldo de puntos ya no es suficiente.");
        if (puntosRedimidos > totalAntesDePuntos()) throw new IllegalStateException("Los puntos redimidos superan el total de la compra.");
        calcularPuntosGenerados();
        descontarExistencias();
        if (puntosRedimidos > 0) comprador.redimirPuntos(puntosRedimidos);
        comprador.acumularPuntos(puntosFidelidad);
        confirmada = true;
    }

    private void validarProductosDelRubro() {
        for (DetalleTransaccion detalle : detalles) {
            Producto producto = detalle.getProducto();
            if ("tienda".equals(rubro) && !(producto instanceof ProductoTienda)) throw new IllegalStateException("La tienda solo puede vender productos de tienda.");
            if ("cafeteria".equals(rubro) && !(producto instanceof ProductoCafeteria)) throw new IllegalStateException("La cafetería solo puede vender productos de cafetería.");
        }
    }

    private void validarExistencias() {
        for (int i = 0; i < detalles.size(); i++) {
            DetalleTransaccion detalle = detalles.get(i);
            boolean yaValidado = false;
            for (int anterior = 0; anterior < i; anterior++) {
                if (detalles.get(anterior).getProducto() == detalle.getProducto()) { yaValidado = true; break; }
            }
            if (yaValidado) continue;
            int requerida = 0;
            for (DetalleTransaccion repetido : detalles) if (repetido.getProducto() == detalle.getProducto()) requerida += repetido.getCantidad();
            int disponible = 0;
            for (Existencia existencia : detalle.getProducto().getExistencias()) disponible += existencia.getCantidad();
            if (disponible < requerida) throw new IllegalStateException("No hay existencia suficiente de " + detalle.getProducto().getNombre() + ".");
        }
    }

    private void descontarExistencias() {
        for (DetalleTransaccion detalle : detalles) {
            int pendiente = detalle.getCantidad();
            for (Existencia existencia : detalle.getProducto().getExistencias()) {
                int cantidad = Math.min(pendiente, existencia.getCantidad());
                if (cantidad > 0) existencia.disminuir(cantidad);
                pendiente -= cantidad;
                if (pendiente == 0) break;
            }
        }
    }
    public double getPropinaSugerida() {
        return Math.max(0.0, calcularSubtotal() - descuento) * 0.10;
    }

    public List<String> consultarAlergenos() {
        List<String> alergenos = new ArrayList<>();
        for (DetalleTransaccion detalle : detalles) {
            if (detalle.getProducto() instanceof Snack) {
                for (String a : ((Snack) detalle.getProducto()).getAlergenos())
                    if (!alergenos.contains(a)) alergenos.add(a);
            }
        }
        return alergenos;
    }

    public boolean contieneBebidaCaliente() {
        for (DetalleTransaccion detalle : detalles) {
            if (detalle.getProducto() instanceof Bebida && ((Bebida) detalle.getProducto()).esCaliente()) return true;
        }
        return false;
    }


    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { validarPendiente(); if (fechaHora == null) throw new IllegalArgumentException("La fecha es obligatoria."); this.fechaHora = fechaHora; }
    public String getRubro() { return rubro; }
    public double getSubtotal() { return subtotal; }
    public double getDescuento() { return descuento; }
    public TipoPromocion getPromocion() { return promocion; }
    public Empleado getDuenoCodigo() { return duenoCodigo; }
    public double getImpuestos() { return impuestos; }
    public double getPropina() { return propina; }
    public double getTotal() { return total; }
    public int getPuntosFidelidad() { return puntosFidelidad; }
    public int getPuntosRedimidos() { return puntosRedimidos; }
    public boolean isConfirmada() { return confirmada; }
    public Usuario getComprador() { return comprador; }
    public void setComprador(Usuario comprador) { validarPendiente(); if (comprador == null) throw new IllegalArgumentException("El comprador es obligatorio."); this.comprador = comprador; }
    public List<DetalleTransaccion> getDetalles() { return detalles; }
}
