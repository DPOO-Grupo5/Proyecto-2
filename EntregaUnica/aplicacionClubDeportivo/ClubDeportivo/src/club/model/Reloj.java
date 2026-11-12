package club.model;

import java.time.LocalDateTime;

/**
 * Hora actual del sistema. Las reglas que dependen del momento presente (fecha pasada, sesión finalizada,
 * partido ya disputado, reserva activa) la consultan aquí, de modo que las pruebas puedan fijarla.
 */
public final class Reloj {
    private static LocalDateTime fijo;

    private Reloj() { }

    public static LocalDateTime ahora() {
        return fijo != null ? fijo : LocalDateTime.now();
    }

    /** Solo para pruebas: congela la hora en el instante indicado. */
    public static void fijar(LocalDateTime instante) { fijo = instante; }

    public static void usarRelojDelSistema() { fijo = null; }
}
