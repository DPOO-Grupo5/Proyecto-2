package club.modelo.entrenamiento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import club.modelo.instalaciones.Instalacion;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Entrenador;

public class SesionEntrenamiento implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDateTime fechaHora;
    private int duracionMinutos;
    private String tipo;

    private Instalacion instalacion;
    private Entrenador entrenador;
    private List<Socio> convocados;
    private List<Asistencia> asistencias;

    public SesionEntrenamiento(LocalDateTime fechaHora, int duracionMinutos, String tipo, Instalacion instalacion, Entrenador entrenador) {
        if (fechaHora == null || duracionMinutos <= 0 || instalacion == null || entrenador == null) {
            throw new IllegalArgumentException("La fecha, duración, instalación y entrenador son obligatorios.");
        }
        this.fechaHora = fechaHora;
        this.duracionMinutos = duracionMinutos;
        this.tipo = tipo;
        this.instalacion = instalacion;
        this.entrenador = entrenador;
        this.convocados = new ArrayList<>();
        this.asistencias = new ArrayList<>();
    }

    // --- MÉTODOS DE LÓGICA ---

    public void agregarConvocado(Socio s) {
        validarConvocado(s);
        if (s != null && !convocados.contains(s)) {
            convocados.add(s);
            s.registrarSesionConvocado(this);
        }
    }

    public void validarConvocados(List<Socio> socios) {
        if (socios == null || socios.isEmpty()) throw new IllegalArgumentException("La sesión debe tener convocados.");
        Set<Socio> unicos = new HashSet<>();
        for (Socio socio : socios) {
            validarConvocado(socio);
            if (!unicos.add(socio)) throw new IllegalArgumentException("Un socio no puede aparecer dos veces en la convocatoria.");
        }
    }

    private void validarConvocado(Socio socio) {
        if (socio == null) throw new IllegalArgumentException("El convocado es obligatorio.");
        LocalDateTime fin = fechaHora.plusMinutes(duracionMinutos);
        for (club.modelo.instalaciones.Reserva reserva : socio.getReservas()) {
            if (reserva.seTraslapaCon(fechaHora, fin)) throw new IllegalStateException("El convocado ya tiene una reserva en ese horario.");
        }
        for (SesionEntrenamiento otra : socio.getSesionesConvocado()) {
            if (otra != this && otra.seTraslapaCon(fechaHora, fin)) throw new IllegalStateException("El convocado ya tiene otra sesión en ese horario.");
        }
    }

    public void registrarAsistencia(Asistencia a) {
        if (a == null || a.getSesion() != this || a.getSocio() == null || !convocados.contains(a.getSocio())) {
            throw new IllegalArgumentException("La asistencia debe corresponder a un jugador convocado.");
        }
        for (Asistencia existente : asistencias) {
            if (existente.getSocio() == a.getSocio()) throw new IllegalStateException("Ya se registró la asistencia de este convocado.");
        }
        asistencias.add(a);
    }

    public boolean seTraslapaCon(LocalDateTime ini, LocalDateTime fin) {
        if (ini == null || fin == null || fechaHora == null) return false;
        LocalDateTime finSesion = this.fechaHora.plusMinutes(duracionMinutos);
        // Se traslapan si el inicio de uno es antes del fin del otro
        return this.fechaHora.isBefore(fin) && finSesion.isAfter(ini);
    }

    public void validarDisponibilidad() {
        LocalDateTime fin = fechaHora.plusMinutes(duracionMinutos);
        if (!instalacion.estaDisponible(fechaHora, fin, instalacion.getReservas())) {
            throw new IllegalStateException("La instalación ya está ocupada en ese horario.");
        }
        if (instalacion.getDisciplina() != null) {
            if (!instalacion.getDisciplina().hayCupoSimultaneo(fechaHora, fin, instalacion.getReservas())) throw new IllegalStateException("Se alcanzó el máximo de instalaciones simultáneas para la disciplina.");
        }
    }

    public boolean haFinalizado(LocalDateTime ahora) {
        return ahora != null && !fechaHora.plusMinutes(duracionMinutos).isAfter(ahora);
    }

    public boolean estaVigente(LocalDateTime instante) {
        if (instante == null) return false;
        LocalDateTime fin = fechaHora.plusMinutes(duracionMinutos);
        return !instante.isBefore(fechaHora) && instante.isBefore(fin);
    }

    public boolean incluye(Socio s) {
        return convocados != null && convocados.contains(s);
    }

    // --- GETTERS (Encapsulamiento protegido) ---

    public LocalDateTime getFechaHora() { return fechaHora; }

    public int getDuracionMinutos() { return duracionMinutos; }

    public String getTipo() { return tipo; }

    public void setTipo(String tipo) { this.tipo = tipo; }

    public Instalacion getInstalacion() { return instalacion; }

    public Entrenador getEntrenador() { return entrenador; }

    public List<Socio> getConvocados() { return convocados; }

    public List<Asistencia> getAsistencias() { return asistencias; }
}
