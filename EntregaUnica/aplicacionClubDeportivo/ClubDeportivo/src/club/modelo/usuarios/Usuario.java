package club.modelo.usuarios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import club.modelo.tienda.Producto;
import club.modelo.tienda.Transaccion;

public abstract class Usuario implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected String login;
    protected String nombre;
    protected int puntosFidelidad;
    protected String password;
    protected double descuento;

    private List<Transaccion> transacciones = new ArrayList<>();
    private static final int ITERACIONES_PASSWORD = 120000;
    private static final int BITS_PASSWORD = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public Usuario(String login, String password, String nombre) {
        setLogin(login);
        setPassword(password);
        setNombre(nombre);
        this.puntosFidelidad = 0; 
    }

    public boolean validarPassword(String passwordIngresado) {
        if (this.password == null || passwordIngresado == null) return false;
        if (!this.password.startsWith("pbkdf2$")) {
            if (!this.password.equals(passwordIngresado)) return false;
            setPassword(passwordIngresado); // Migra credenciales legadas al primer login correcto.
            return true;
        }
        String[] partes = this.password.split("\\$", -1);
        if (partes.length != 4) return false;
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] obtenido = derivarPassword(passwordIngresado, salt, iteraciones, esperado.length * 8);
            return MessageDigest.isEqual(esperado, obtenido);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void actualizarPassword(String actual, String nueva) { 
        if (validarPassword(actual)) {
            setPassword(nueva);
        } else {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }
    }

    
    public Transaccion iniciarTransaccion(Map<Producto, Integer> productos, String rubro) {
        Transaccion t = new Transaccion(java.time.LocalDateTime.now(), rubro);
        t.setComprador(this);
        if (productos != null) {
            for (Map.Entry<Producto, Integer> item : productos.entrySet()) {
                if (item.getValue() == null) throw new IllegalArgumentException("La cantidad del producto es obligatoria.");
                t.agregarDetalle(item.getKey(), item.getValue());
            }
        }
        this.transacciones.add(t);
        return t; 
    }

    public List<Transaccion> consultarHistorialCompras() { 
        return this.transacciones; 
    }


    public void acumularPuntos(int puntos) {
        if (puntos < 0) throw new IllegalArgumentException("Los puntos a acumular no pueden ser negativos.");
        this.puntosFidelidad += puntos;
    }

    public void redimirPuntos(int puntos) {
        if (puntos <= 0 || this.puntosFidelidad < puntos) throw new IllegalArgumentException("El saldo de puntos es insuficiente.");
        this.puntosFidelidad -= puntos;
    }

   

    public String getLogin() { return login; }

    public void setLogin(String login) { if (login == null || login.trim().isEmpty()) throw new IllegalArgumentException("El login es obligatorio."); this.login = login.trim(); }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) { if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio."); this.nombre = nombre.trim(); }

    public int getPuntosFidelidad() { return puntosFidelidad; }

    public void setPuntosFidelidad(int puntosFidelidad) { if (puntosFidelidad < 0) throw new IllegalArgumentException("Los puntos no pueden ser negativos."); this.puntosFidelidad = puntosFidelidad; }

    /** Devuelve el valor almacenado por compatibilidad; las credenciales son hashes PBKDF2. */
    public String getPassword() { return password; }

    public void setPassword(String password) {
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("La contraseña es obligatoria.");
        byte[] salt = new byte[16];
        ALEATORIO.nextBytes(salt);
        byte[] hash = derivarPassword(password, salt, ITERACIONES_PASSWORD, BITS_PASSWORD);
        this.password = "pbkdf2$" + ITERACIONES_PASSWORD + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] derivarPassword(String password, byte[] salt, int iteraciones, int bits) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iteraciones, bits);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo proteger la contraseña.", e);
        } finally {
            spec.clearPassword();
        }
    }

    public double getDescuento() { return descuento; }

    public void setDescuento(double descuento) { this.descuento = descuento; }

    public List<Transaccion> getTransacciones() { return transacciones; }

    public void setTransacciones(List<Transaccion> transacciones) { this.transacciones = transacciones; }
}
