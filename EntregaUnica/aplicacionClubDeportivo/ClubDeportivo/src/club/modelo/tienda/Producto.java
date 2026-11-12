package club.modelo.tienda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Producto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected String nombre;
    protected double precio;
    private List<Existencia> existencias = new ArrayList<>();

    public Producto(String nombre, double precio) {
        setNombre(nombre);
        setPrecio(precio);
    }

    public Existencia buscarExistencia(String ubicacion) {
        for (Existencia existencia : existencias) {
            if (existencia.getUbicacionInventario().equalsIgnoreCase(ubicacion)) return existencia;
        }
        return null;
    }

    public void agregarExistencia(Existencia existencia) {
        if (existencia == null) throw new IllegalArgumentException("La existencia es obligatoria.");
        if (buscarExistencia(existencia.getUbicacionInventario()) != null) {
            throw new IllegalArgumentException("Ya existe inventario para esa ubicación.");
        }
        existencias.add(existencia);
    }

    public int getCantidadTotal() {
        int total = 0;
        for (Existencia existencia : existencias) total += existencia.getCantidad();
        return total;
    }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio.");
        this.nombre = nombre.trim();
    }

    public double getPrecio() { return precio; }

    public void setPrecio(double precio) {
        if (precio < 0 || Double.isNaN(precio) || Double.isInfinite(precio)) throw new IllegalArgumentException("El precio no puede ser negativo ni no finito.");
        this.precio = precio;
    }

    // Compatibilidad con el modelo original de una sola ubicación.
    public Existencia getExistencia() { return existencias.isEmpty() ? null : existencias.get(0); }

    public void setExistencia(Existencia existencia) {
        existencias.clear();
        if (existencia != null) existencias.add(existencia);
    }

    public List<Existencia> getExistencias() { return Collections.unmodifiableList(existencias); }
}
