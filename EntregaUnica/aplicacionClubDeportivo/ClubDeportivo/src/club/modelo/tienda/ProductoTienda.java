package club.modelo.tienda;

public class ProductoTienda extends Producto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String categoria;

    public ProductoTienda(String nombre, double precio, String categoria) {
        super(nombre, precio);
        this.categoria = categoria;
    }

    public String getCategoria() { return categoria; }

    public void setCategoria(String categoria) { this.categoria = categoria; }
}
