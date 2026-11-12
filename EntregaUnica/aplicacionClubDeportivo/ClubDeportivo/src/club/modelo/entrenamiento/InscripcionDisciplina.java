package club.modelo.entrenamiento;

import java.time.LocalDate;
import club.modelo.usuarios.Socio;

public class InscripcionDisciplina implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String nivel;
    private int ranking;

    private Socio socio;
    private DisciplinaIndividual disciplina;
    private LocalDate fechaInscripcion = LocalDate.now();
    private LocalDate fechaRetiro;

    public InscripcionDisciplina(String nivel, int ranking, Socio socio, DisciplinaIndividual disciplina) {
        cambiarNivel(nivel);
        if (ranking < 0) throw new IllegalArgumentException("El ranking no puede ser negativo.");
        this.ranking = ranking;
        if (socio == null || disciplina == null) throw new IllegalArgumentException("El socio y la disciplina son obligatorios.");
        this.socio = socio;
        this.disciplina = disciplina;
    }


    public void cambiarNivel(String nuevoNivel) {
        if (nivelOrden(nuevoNivel) < 0) throw new IllegalArgumentException("El nivel debe ser Principiante, Intermedio o Avanzado.");
        this.nivel = nuevoNivel.trim();
    }

    public void actualizarRanking(boolean gano, int rankingRival) {
        // Lógica básica de ajuste de ranking basada en el resultado
        actualizarRanking(gano ? 1.0 : 0.0, rankingRival);
    }

    public void actualizarRanking(double resultado, int rankingRival) {
        if (resultado < 0.0 || resultado > 1.0) throw new IllegalArgumentException("El resultado debe estar entre 0 y 1.");
        if (rankingRival < 0) throw new IllegalArgumentException("El ranking rival no puede ser negativo.");
        double esperado = 1.0 / (1.0 + Math.pow(10.0, (rankingRival - ranking) / 400.0));
        ranking = Math.max(0, (int) Math.round(ranking + 32.0 * (resultado - esperado)));
    }

    public boolean puedeJugarCon(InscripcionDisciplina otra) {
        if (otra == null) {
            return false;
        }
        // Dos inscripciones pueden jugar si tienen el mismo nivel o niveles adyacentes
        int propio = nivelOrden(this.nivel);
        int rival = nivelOrden(otra.getNivel());
        return propio >= 0 && rival >= 0 && Math.abs(propio - rival) <= 1;
    }

    public boolean esNivelAdyacente(String otroNivel) {
        if (this.nivel == null || otroNivel == null) {
            return false;
        }
        // Definición sencilla de adyacencia basada en categorías comunes (Principiante, Intermedio, Avanzado, Profesional)
        int a = nivelOrden(this.nivel), b = nivelOrden(otroNivel);
        return a >= 0 && b >= 0 && Math.abs(a - b) == 1;
    }

    private static int nivelOrden(String nivel) {
        if (nivel == null) return -1;
        switch (nivel.trim().toLowerCase()) {
            case "principiante": return 0;
            case "intermedio": return 1;
            case "avanzado": return 2;
            default: return -1;
        }
    }

    public boolean isActiva() { return fechaRetiro == null; }
    public LocalDate getFechaInscripcion() { return fechaInscripcion; }
    public LocalDate getFechaRetiro() { return fechaRetiro; }
    public void retirar() { if (fechaRetiro == null) fechaRetiro = LocalDate.now(); }

    // --- GETTERS Y SETTERS ---

    public String getNivel() { return nivel; }

    public int getRanking() { return ranking; }

    public Socio getSocio() { return socio; }

    public DisciplinaIndividual getDisciplina() { return disciplina; }
}
