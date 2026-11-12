package club.modelo.usuarios;

import java.util.ArrayList;
import java.util.List;

import club.modelo.tienda.Producto;
import club.modelo.tienda.SugerenciaProducto;

public abstract class Empleado extends Usuario implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected static final double DESCUENTO = 0.15;
    protected static final double DESCUENTOCOMPARTIDO = 0.08;
    protected String codigoDescuento;

    private List<Turno> turnos = new ArrayList<>();
    private List<SolicitudCambioTurno> solicitudes = new ArrayList<>();
    private List<SugerenciaProducto> sugerencias = new ArrayList<>();
    /** Falso por defecto: el código está activo (también al cargar datos guardados antes de existir este atributo). */
    private boolean codigoDesactivado;

    public Empleado(String login, String password, String nombre, String codigoDescuento) {
        super(login, password, nombre);
        this.codigoDescuento = codigoDescuento;
    }

    // --- MÉTODOS DE LÓGICA Y REGLAS DE NEGOCIO AGREGADOS ---

    public double getPorcentajeDescuentoPropio() {
        return DESCUENTO;
    }

    public double getPorcentajeDescuentoCompartido() {
        return DESCUENTOCOMPARTIDO;
    }

    public boolean validarCodigo(String codigo) {
        return this.codigoDescuento != null && this.codigoDescuento.equals(codigo);
    }

    public boolean isCodigoActivo() { return codigoDescuento != null && !codigoDescuento.trim().isEmpty() && !codigoDesactivado; }
    public void desactivarCodigo() { codigoDesactivado = true; }
    public void activarCodigo() { codigoDesactivado = false; }

    /**
     * Solicita cambiar un turno propio (HU-E03). El nuevo horario debe ser distinto del actual y estar dentro del
     * horario de atención del club, y el turno no puede tener otra solicitud pendiente. El turno original sigue vigente.
     */
    public SolicitudCambioTurno solicitarCambioTurno(Turno actual, Turno deseado) {
        if (actual == null || deseado == null || !turnos.contains(actual)) throw new IllegalArgumentException("El turno actual debe pertenecer al empleado.");
        if (actual.mismoHorario(deseado)) throw new IllegalArgumentException("El horario propuesto es igual al actual.");
        if (!deseado.estaDentroDelHorarioDeAtencion())
            throw new IllegalArgumentException("El horario propuesto está fuera del horario de atención (" + Turno.descripcionHorarioDeAtencion() + ").");
        for (SolicitudCambioTurno s : solicitudes)
            if (s.getTurnoActual() == actual && "Pendiente".equals(s.getEstado()))
                throw new IllegalStateException("Ese turno ya tiene una solicitud pendiente; espere a que se resuelva.");
        SolicitudCambioTurno solicitud = new SolicitudCambioTurno(this, actual, deseado);
        this.solicitudes.add(solicitud);
        return solicitud;
    }

    public void agregarTurno(Turno turno) {
        if (turno == null) throw new IllegalArgumentException("El turno es obligatorio.");
        for (Turno existente : turnos) if (existente.seTraslapaCon(turno)) throw new IllegalArgumentException("Los turnos del empleado no pueden traslaparse.");
        if (!turnos.contains(turno)) turnos.add(turno);
    }

    public void retirarTurno(Turno turno) {
        if (!turnos.remove(turno)) throw new IllegalArgumentException("El turno no pertenece al empleado.");
    }

    public SugerenciaProducto sugerirProducto(Producto p) {
        if (p == null) throw new IllegalArgumentException("El producto es obligatorio.");
        SugerenciaProducto sugerencia = new SugerenciaProducto("Pendiente"); 
        sugerencia.setProducto(p);
        this.sugerencias.add(sugerencia);
        return sugerencia;
     
    }

    /** Reemplaza todos los turnos; lo usa la publicación de la programación semanal (HU-A11). */
    void reemplazarTurnos(List<Turno> nuevos) {
        turnos.clear();
        turnos.addAll(nuevos);
    }

    public List<Turno> consultarTurnos() {
        return this.turnos;
    }

   
    public String getCodigoDescuento() { return codigoDescuento; }

    public void setCodigoDescuento(String codigoDescuento) { this.codigoDescuento = codigoDescuento; }

    public List<Turno> getTurnos() { return turnos; }

    public List<SolicitudCambioTurno> getSolicitudes() { return solicitudes; }

    public List<SugerenciaProducto> getSugerencias() { return sugerencias; }

}
