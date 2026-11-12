package club.modelo.tienda;

/**
 * Promociones que puede tener una compra. Regla del club: no se acumulan, una compra admite a lo sumo una.
 */
public enum TipoPromocion {
    /** Un empleado usa su propio código: 15 % sobre el subtotal antes de impuestos (HU-E06). */
    CODIGO_PROPIO,
    /** Código de un empleado usado por otra persona: 8 % sobre el subtotal antes de impuestos (HU-S17, HU-E06). */
    CODIGO_COMPARTIDO,
    /** Redención de puntos de fidelidad: 1 punto = $1 sobre el total (HU-S16). */
    PUNTOS,
    /** Bono de descuento ganado en un torneo recreativo (requerimiento de torneos). */
    BONO_TORNEO
}
