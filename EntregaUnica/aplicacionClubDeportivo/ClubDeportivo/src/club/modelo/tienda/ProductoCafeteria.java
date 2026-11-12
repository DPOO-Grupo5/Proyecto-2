package club.modelo.tienda;

public abstract class ProductoCafeteria extends Producto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public ProductoCafeteria(String nombre, double precio) {
        super(nombre, precio);
    }
}
