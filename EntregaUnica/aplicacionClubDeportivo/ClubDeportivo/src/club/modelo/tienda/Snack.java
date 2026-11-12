package club.modelo.tienda;

import java.util.ArrayList;
import java.util.List;

public class Snack extends ProductoCafeteria implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<String> alergenos;

    public Snack(String nombre, double precio, List<String> alergenos) {
        super(nombre, precio);
        this.alergenos = new ArrayList<String>();
    }

    public List<String> getAlergenos() { return alergenos; }

    public void setAlergenos(ArrayList<String> alergenos) 
    { this.alergenos = alergenos; }
}
