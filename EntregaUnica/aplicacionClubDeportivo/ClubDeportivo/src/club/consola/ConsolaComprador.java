package club.consola;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import club.modelo.competencias.*;
import club.modelo.entrenamiento.*;
import club.modelo.instalaciones.*;
import club.modelo.tienda.*;
import club.modelo.usuarios.*;

/** Base de las aplicaciones cuyos usuarios compran en la tienda y la cafetería (socios y empleados). */
public abstract class ConsolaComprador extends ConsolaBase {

    // ===================================================================== COMPRAS (tienda y cafetería)
    protected void comprar(Usuario usuario, String rubro) {
        List<Producto> catalogo = new ArrayList<>();
        for (Producto p : sistema.getProductos()) {
            boolean delRubro = "tienda".equals(rubro) ? p instanceof ProductoTienda : p instanceof ProductoCafeteria;
            if (delRubro && p.getCantidadTotal() > 0) catalogo.add(p);
        }
        if (catalogo.isEmpty()) throw new IllegalStateException("No hay productos disponibles en " + rubro + ".");
        Transaccion t = usuario.iniciarTransaccion(null, rubro);
        try {
            while (true) {
                System.out.println("--- Productos de " + rubro + " ---");
                for (int i = 0; i < catalogo.size(); i++) System.out.printf("  %d) %s%n", i + 1, Formatos.describirProducto(catalogo.get(i)));
                int n = entrada.enteroEnRango("Número de producto (0 para terminar): ", 0, catalogo.size());
                if (n == 0) break;
                t.agregarDetalle(catalogo.get(n - 1), entrada.enteroEnRango("Cantidad: ", 1, Integer.MAX_VALUE));
            }
            if (t.getDetalles().isEmpty()) throw new IllegalStateException("No agregó productos.");

            if ("cafeteria".equals(rubro)) {
                List<String> alergenos = t.consultarAlergenos();
                if (!alergenos.isEmpty()) {
                    System.out.println("ATENCIÓN - alérgenos de su pedido: " + String.join(", ", alergenos));
                    if (!confirmar("¿Desea continuar con la compra?")) throw new IllegalStateException("Compra cancelada por alérgenos.");
                }
            }
            elegirPromocion(t, usuario);
            if ("cafeteria".equals(rubro)) {
                String pct = textoOpcional(String.format("Propina sugerida 10%% ($%.0f). Porcentaje (Enter para 10): ", t.getPropinaSugerida()));
                t.calcularPropina(pct.isEmpty() ? 10 : porcentaje(pct));
            }
            System.out.printf("Subtotal: $%.0f | descuento por código: $%.0f | impuesto: $%.0f | puntos: -$%d | propina: $%.0f | TOTAL: $%.0f%n",
                    t.calcularSubtotal(), t.getDescuento(), t.calcularImpuestos(), t.getPuntosRedimidos(), t.getPropina(), t.getTotal());
            if (!confirmar("¿Confirmar la compra?")) throw new IllegalStateException("Compra cancelada.");
            t.confirmar();
            System.out.printf("Compra confirmada. Total: $%.0f | puntos ganados: %d | saldo de puntos: %d%n",
                    t.getTotal(), t.getPuntosFidelidad(), usuario.getPuntosFidelidad());
        } catch (RuntimeException e) {
            usuario.getTransacciones().remove(t);
            throw e;
        }
    }

    /**
     * Ofrece una sola promoción (no se acumulan): un código de descuento de empleado o, si no se usa código,
     * puntos de fidelidad. Un código o una cantidad inválida se informa y se vuelve a preguntar.
     */
    private void elegirPromocion(Transaccion t, Usuario usuario) {
        // Un empleado tiene su propio código y un socio puede haber guardado el que le compartieron: no se escribe de nuevo.
        String predeterminado = usuario.getCodigoGuardado();
        if (usuario instanceof Empleado && ((Empleado) usuario).isCodigoActivo()) predeterminado = ((Empleado) usuario).getCodigoDescuento();
        if (predeterminado != null && !predeterminado.isEmpty() && confirmar("¿Aplicar su código de descuento " + predeterminado + "?")) {
            try {
                t.aplicarCodigoDescuento(predeterminado, sistema.getEmpleados());
                System.out.printf("Código aplicado: descuento de $%.0f.%n", t.getDescuento());
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
        while (true) {
            String codigo = textoOpcional("Código de descuento de empleado (Enter para omitir): ");
            if (codigo.isEmpty()) break;
            try {
                t.aplicarCodigoDescuento(codigo, sistema.getEmpleados());
                System.out.printf("Código aplicado: descuento de $%.0f.%n", t.getDescuento());
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
        if (usuario.getPuntosFidelidad() <= 0) return;
        while (true) {
            int puntos = entrada.enteroEnRango("Puntos a redimir, 1 punto = $1 (tiene " + usuario.getPuntosFidelidad() + ", 0 para no usar): ", 0, Integer.MAX_VALUE);
            if (puntos == 0) return;
            try {
                t.redimirPuntos(puntos);
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private static double porcentaje(String texto) {
        Double valor = EntradaConsola.parsearDecimal(texto);
        if (valor == null) throw new IllegalArgumentException("El porcentaje de propina debe ser un número.");
        return valor;
    }

    protected void verCompras(Usuario usuario) {
        System.out.println("--- Historial de compras ---");
        int cuantas = 0;
        for (Transaccion t : usuario.consultarHistorialCompras()) {
            if (!t.isConfirmada()) continue;
            cuantas++;
            System.out.printf("%s | %s | total: $%.0f | puntos ganados: %d | puntos usados: %d%n", t.getFechaHora(), t.getRubro(),
                    t.getTotal(), t.getPuntosFidelidad(), t.getPuntosRedimidos());
            for (DetalleTransaccion d : t.getDetalles())
                System.out.printf("     %d x %s%n", d.getCantidad(), d.getProducto().getNombre());
        }
        if (cuantas == 0) System.out.println("Sin compras.");
        System.out.println("Saldo de puntos de fidelidad: " + usuario.getPuntosFidelidad() + " (1 punto = $1)");
    }
}
