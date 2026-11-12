package club.modelo.competencias;

public class Voleibol extends DeporteConjunto implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public Voleibol(String nombre, int minJugadores) {
        super(nombre, minJugadores);
    }
    
    @Override
    public void validarMarcador(int local, int visitante) {
        boolean validoLocal = local == 3 && visitante >= 0 && visitante <= 2;
        boolean validoVisitante = visitante == 3 && local >= 0 && local <= 2;
        if (!validoLocal && !validoVisitante)
            throw new IllegalArgumentException("En voleibol el marcador es por sets: el ganador llega a 3 y el rival tiene 0, 1 o 2.");
    }
}
