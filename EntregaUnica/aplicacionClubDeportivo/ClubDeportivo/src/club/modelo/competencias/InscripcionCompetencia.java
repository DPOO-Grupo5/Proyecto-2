package club.modelo.competencias;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import club.modelo.usuarios.Socio;

public class InscripcionCompetencia implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Competencia competencia;
    private EquipoCompetencia equipo;
    private LocalDate fechaInscripcion = LocalDate.now();
    private boolean activa = true;

    public InscripcionCompetencia(Competencia competencia, EquipoCompetencia equipo) {
        if (competencia == null || equipo == null) throw new IllegalArgumentException("La competencia y el equipo son obligatorios.");
        this.competencia = competencia;
        this.equipo = equipo;
    }
    
    private List<Socio> nomina = new ArrayList<>();

    public void registrarEnNomina(Socio socio) {
        if (socio == null) throw new IllegalArgumentException("El socio es obligatorio.");
        if (equipo instanceof Equipo) {
            boolean pertenece = false;
            for (InscripcionEquipo i : ((Equipo) equipo).getInscripcionesActivas()) if (i.getSocio() == socio) pertenece = true;
            if (!pertenece) throw new IllegalArgumentException("El socio no pertenece al equipo.");
        }
        if (!nomina.contains(socio)) nomina.add(socio);
    }

    public boolean enNomina(Socio socio) { return nomina.contains(socio); }
    public List<Socio> getNomina() { return nomina; }

    public void retirar() { activa = false; }
    public Competencia getCompetencia() { return competencia; }
    public EquipoCompetencia getEquipo() { return equipo; }
    public LocalDate getFechaInscripcion() { return fechaInscripcion; }
    public boolean isActiva() { return activa; }
}
