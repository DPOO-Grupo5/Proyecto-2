package club.modelo.competencias;

import java.util.ArrayList;
import java.util.List;

public class RondaEliminacion implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String nombre;
    private int orden;

    private List<PartidoCompetencia> partidos = new ArrayList<>();
    private List<EquipoCompetencia> pasesDirectos = new ArrayList<>();

    public RondaEliminacion(String nombre, int orden) {
        this.nombre = nombre;
        this.orden = orden;
    }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getOrden() { return orden; }

    public void setOrden(int orden) { this.orden = orden; }

    public List<PartidoCompetencia> getPartidos() { return partidos; }

    public void setPartidos(List<PartidoCompetencia> partidos) { this.partidos = partidos; }

    public List<EquipoCompetencia> getPasesDirectos() { return pasesDirectos; }

    public void setPasesDirectos(List<EquipoCompetencia> pasesDirectos) { this.pasesDirectos = pasesDirectos == null ? new ArrayList<>() : pasesDirectos; }
}
