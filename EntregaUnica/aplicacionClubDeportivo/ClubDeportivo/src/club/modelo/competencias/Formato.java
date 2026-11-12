package club.modelo.competencias;

import java.util.List;

public abstract class Formato implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String nombreFormato;

    public Formato(String nombreFormato) {
        this.nombreFormato = nombreFormato;
    }

    
    public abstract void generarEncuentros(List<EquipoCompetencia> equipos);

    public String getNombreFormato() { 
        return nombreFormato; 
    }

    public void setNombreFormato(String nombreFormato) { 
        this.nombreFormato = nombreFormato; 
    }
}