package club.modelo.tienda;

public class DetalleTransaccion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int cantidad;
    private double subtotal;
    private Producto producto;

    public DetalleTransaccion(Producto producto, int cantidad) {
        setProducto(producto);
        setCantidad(cantidad);
    }

    public double calcularSubtotal() {
        subtotal = producto.getPrecio() * cantidad;
        return subtotal;
    }

    public int getCantidad() { return cantidad; }

    public void setCantidad(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva.");
        this.cantidad = cantidad;
        if (producto != null) calcularSubtotal();
    }

    public double getSubtotal() { return subtotal; }

    public Producto getProducto() { return producto; }

    public void setProducto(Producto producto) {
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio.");
        this.producto = producto;
        if (cantidad > 0) calcularSubtotal();
    }
}
