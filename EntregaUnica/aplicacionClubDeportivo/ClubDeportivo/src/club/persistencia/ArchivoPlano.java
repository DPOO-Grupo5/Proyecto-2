package club.persistencia;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import club.model.SistemaClub;

public class ArchivoPlano {
    private static final String CARPETA_PREDETERMINADA = "datos_seneca";
    private static final String INDICE = "indice.csv";
    private static final char SEPARADOR = ';';
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private final File carpeta;

    public ArchivoPlano() { this(CARPETA_PREDETERMINADA); }

    public ArchivoPlano(String carpeta) {
        if (carpeta == null || carpeta.trim().isEmpty()) throw new IllegalArgumentException("La carpeta es obligatoria.");
        this.carpeta = new File(carpeta);
    }

    public File getCarpeta() { return carpeta; }

    public boolean existeEstado() { return new File(carpeta, INDICE).isFile(); }

    // ===================== GUARDAR =====================
    public void guardarEstado(SistemaClub sistema) {
        if (sistema == null) throw new IllegalArgumentException("El estado del club es obligatorio.");
        if (!carpeta.exists() && !carpeta.mkdirs()) throw new IllegalStateException("No se pudo crear la carpeta " + carpeta.getPath());

        Map<Object, Integer> ids = new IdentityHashMap<>();
        List<Object> orden = new ArrayList<>();
        registrar(sistema, ids, orden);
        Map<Class<?>, List<Object>> porClase = new LinkedHashMap<>();
        for (int i = 0; i < orden.size(); i++) {
            Object objeto = orden.get(i);
            List<Object> grupo = porClase.get(objeto.getClass());
            if (grupo == null) { grupo = new ArrayList<>(); porClase.put(objeto.getClass(), grupo); }
            grupo.add(objeto);
            for (Field campo : campos(objeto.getClass())) descubrir(leer(campo, objeto), ids, orden);
        }

        List<String> indice = new ArrayList<>();
        indice.add("clase;archivo;filas");
        List<Path[]> movimientos = new ArrayList<>();
        Set<String> nombresNuevos = new HashSet<>();
        try {
            for (Map.Entry<Class<?>, List<Object>> entrada : porClase.entrySet()) {
                Class<?> clase = entrada.getKey();
                String archivo = clase.getSimpleName() + ".csv";
                if (!nombresNuevos.add(archivo)) throw new IllegalStateException("Dos clases comparten el nombre " + archivo);
                List<Field> campos = campos(clase);
                List<String> lineas = new ArrayList<>();
                StringBuilder cabecera = new StringBuilder("id");
                for (Field campo : campos) cabecera.append(SEPARADOR).append(campo.getDeclaringClass().getSimpleName()).append('.').append(campo.getName());
                lineas.add(cabecera.toString());
                for (Object objeto : entrada.getValue()) {
                    StringBuilder fila = new StringBuilder().append(ids.get(objeto));
                    for (Field campo : campos) fila.append(SEPARADOR).append(codificar(leer(campo, objeto), ids));
                    lineas.add(fila.toString());
                }
                Path temporal = new File(carpeta, archivo + ".tmp").toPath();
                Files.write(temporal, lineas, UTF8);
                movimientos.add(new Path[] { temporal, new File(carpeta, archivo).toPath() });
                indice.add(clase.getName() + SEPARADOR + archivo + SEPARADOR + entrada.getValue().size());
            }
            Path temporalIndice = new File(carpeta, INDICE + ".tmp").toPath();
            Files.write(temporalIndice, indice, UTF8);
            movimientos.add(new Path[] { temporalIndice, new File(carpeta, INDICE).toPath() });

            Set<String> anteriores = archivosDelIndiceAnterior();
            for (Path[] mover : movimientos) Files.move(mover[0], mover[1], StandardCopyOption.REPLACE_EXISTING);
            for (String viejo : anteriores) if (!nombresNuevos.contains(viejo)) new File(carpeta, viejo).delete();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el estado en archivos planos.", e);
        }
    }

    private Set<String> archivosDelIndiceAnterior() throws IOException {
        Set<String> nombres = new HashSet<>();
        File indice = new File(carpeta, INDICE);
        if (!indice.isFile()) return nombres;
        List<String> lineas = Files.readAllLines(indice.toPath(), UTF8);
        for (int i = 1; i < lineas.size(); i++) {
            String[] partes = lineas.get(i).split(String.valueOf(SEPARADOR));
            if (partes.length >= 2) nombres.add(partes[1]);
        }
        return nombres;
    }

    private static void registrar(Object objeto, Map<Object, Integer> ids, List<Object> orden) {
        if (ids.containsKey(objeto)) return;
        ids.put(objeto, ids.size() + 1);
        orden.add(objeto);
    }

    private static void descubrir(Object valor, Map<Object, Integer> ids, List<Object> orden) {
        if (valor == null) return;
        if (valor instanceof Collection) {
            for (Object elemento : (Collection<?>) valor) descubrir(elemento, ids, orden);
        } else if (valor instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) valor).entrySet()) { descubrir(e.getKey(), ids, orden); descubrir(e.getValue(), ids, orden); }
        } else if (esModelo(valor)) {
            registrar(valor, ids, orden);
        }
    }

    /** Objetos del modelo que se guardan con identificador. Los enum se guardan por nombre, no como objetos. */
    private static boolean esModelo(Object valor) {
        return !(valor instanceof Enum) && valor.getClass().getName().startsWith("club.");
    }

    private static String codificar(Object valor, Map<Object, Integer> ids) {
        if (valor == null) return "";
        if (valor instanceof Collection) {
            Collection<?> coleccion = (Collection<?>) valor;
            if (coleccion.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder();
            for (Object elemento : coleccion) {
                if (sb.length() > 0) sb.append('|');
                sb.append(elemento(elemento, ids));
            }
            return sb.toString();
        }
        if (valor instanceof Map) {
            Map<?, ?> mapa = (Map<?, ?>) valor;
            if (mapa.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                if (sb.length() > 0) sb.append('|');
                sb.append(elemento(e.getKey(), ids)).append('=').append(elemento(e.getValue(), ids));
            }
            return sb.toString();
        }
        return elemento(valor, ids);
    }

    private static String elemento(Object valor, Map<Object, Integer> ids) {
        if (valor == null) return "";
        if (esModelo(valor)) return "@" + ids.get(valor);
        if (valor instanceof Enum) return escapar(((Enum<?>) valor).name());
        if (valor instanceof String || valor instanceof Number || valor instanceof Boolean || valor instanceof Character
                || valor instanceof LocalDate || valor instanceof LocalDateTime || valor instanceof LocalTime) {
            return escapar(valor.toString());
        }
        throw new IllegalStateException("Tipo no soportado en el archivo plano: " + valor.getClass().getName());
    }

    // ===================== CARGAR =====================
    public SistemaClub cargarEstado() {
        File indice = new File(carpeta, INDICE);
        if (!indice.isFile()) throw new IllegalStateException("No hay estado guardado en " + carpeta.getPath());
        Map<Integer, Object> objetos = new HashMap<>();
        List<Object[]> pendientes = new ArrayList<>();
        try {
            List<String> lineasIndice = Files.readAllLines(indice.toPath(), UTF8);
            for (int i = 1; i < lineasIndice.size(); i++) {
                if (lineasIndice.get(i).trim().isEmpty()) continue;
                String[] partes = lineasIndice.get(i).split(String.valueOf(SEPARADOR));
                Class<?> clase = Class.forName(partes[0]);
                File archivo = new File(carpeta, partes[1]);
                if (!archivo.isFile()) throw new IllegalStateException("Falta el archivo " + archivo.getName());
                List<String> lineas = Files.readAllLines(archivo.toPath(), UTF8);
                List<String> cabecera = partir(lineas.get(0), SEPARADOR);
                for (int j = 1; j < lineas.size(); j++) {
                    if (lineas.get(j).isEmpty()) continue;
                    List<String> celdas = partir(lineas.get(j), SEPARADOR);
                    Object objeto = crearVacio(clase);
                    objetos.put(Integer.valueOf(celdas.get(0)), objeto);
                    pendientes.add(new Object[] { objeto, cabecera, celdas });
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("No se pudo leer el estado de los archivos planos.", e);
        }
        Map<Class<?>, Map<String, Field>> cache = new HashMap<>();
        for (Object[] pendiente : pendientes) {
            Object objeto = pendiente[0];
            @SuppressWarnings("unchecked") List<String> cabecera = (List<String>) pendiente[1];
            @SuppressWarnings("unchecked") List<String> celdas = (List<String>) pendiente[2];
            Map<String, Field> porNombre = cache.get(objeto.getClass());
            if (porNombre == null) {
                porNombre = new HashMap<>();
                for (Field campo : campos(objeto.getClass())) porNombre.put(campo.getDeclaringClass().getSimpleName() + "." + campo.getName(), campo);
                cache.put(objeto.getClass(), porNombre);
            }
            for (int k = 1; k < cabecera.size() && k < celdas.size(); k++) {
                Field campo = porNombre.get(cabecera.get(k));
                if (campo == null) continue;
                Object valor = decodificar(campo.getType(), celdas.get(k), objetos);
                if (valor == null && campo.getType().isPrimitive()) continue;
                try { campo.set(objeto, valor); }
                catch (IllegalAccessException e) { throw new IllegalStateException("No se pudo asignar " + campo.getName(), e); }
            }
        }
        for (Object objeto : objetos.values()) inicializarColeccionesFaltantes(objeto);
        for (Object objeto : objetos.values()) if (objeto instanceof SistemaClub) return (SistemaClub) objeto;
        throw new IllegalStateException("Los archivos no contienen el estado del club (falta SistemaClub.csv).");
    }

    /**
     * Los objetos se crean sin ejecutar constructores ni inicializadores. Si una colección no viene en el archivo
     * (por ejemplo, un atributo agregado después de guardar los datos) queda en null; aquí se deja vacía.
     */
    private static void inicializarColeccionesFaltantes(Object objeto) {
        for (Field campo : campos(objeto.getClass())) {
            if (leer(campo, objeto) != null) continue;
            Object vacio = null;
            Class<?> tipo = campo.getType();
            if (tipo.isAssignableFrom(ArrayList.class)) vacio = new ArrayList<>();
            else if (tipo.isAssignableFrom(LinkedHashSet.class)) vacio = new LinkedHashSet<>();
            else if (tipo.isAssignableFrom(HashMap.class)) vacio = new HashMap<>();
            if (vacio == null) continue;
            try { campo.set(objeto, vacio); }
            catch (IllegalAccessException e) { throw new IllegalStateException("No se pudo inicializar " + campo.getName(), e); }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object decodificar(Class<?> tipo, String celda, Map<Integer, Object> objetos) {
        if (celda.isEmpty()) return null;
        if (Collection.class.isAssignableFrom(tipo)) {
            Collection<Object> coleccion = Set.class.isAssignableFrom(tipo) ? new LinkedHashSet<>() : new ArrayList<>();
            if (!celda.equals("[]")) for (String parte : partir(celda, '|')) coleccion.add(valorDe(parte, objetos));
            return coleccion;
        }
        if (Map.class.isAssignableFrom(tipo)) {
            Map<Object, Object> mapa = new LinkedHashMap<>();
            if (!celda.equals("[]")) {
                for (String par : partir(celda, '|')) {
                    List<String> kv = partir(par, '=');
                    mapa.put(valorDe(kv.get(0), objetos), valorDe(kv.get(1), objetos));
                }
            }
            return mapa;
        }
        if (tipo == String.class) return desescapar(celda);
        if (tipo == int.class || tipo == Integer.class) return Integer.valueOf(desescapar(celda));
        if (tipo == long.class || tipo == Long.class) return Long.valueOf(desescapar(celda));
        if (tipo == double.class || tipo == Double.class) return Double.valueOf(desescapar(celda));
        if (tipo == float.class || tipo == Float.class) return Float.valueOf(desescapar(celda));
        if (tipo == short.class || tipo == Short.class) return Short.valueOf(desescapar(celda));
        if (tipo == byte.class || tipo == Byte.class) return Byte.valueOf(desescapar(celda));
        if (tipo == boolean.class || tipo == Boolean.class) return Boolean.valueOf(desescapar(celda));
        if (tipo == char.class || tipo == Character.class) return Character.valueOf(desescapar(celda).charAt(0));
        if (tipo == LocalDate.class) return LocalDate.parse(desescapar(celda));
        if (tipo == LocalDateTime.class) return LocalDateTime.parse(desescapar(celda));
        if (tipo == LocalTime.class) return LocalTime.parse(desescapar(celda));
        if (tipo.isEnum()) return Enum.valueOf((Class<Enum>) tipo, desescapar(celda));
        return valorDe(celda, objetos);
    }

    private static Object valorDe(String texto, Map<Integer, Object> objetos) {
        if (texto.isEmpty()) return null;
        if (texto.charAt(0) == '@') {
            Object objeto = objetos.get(Integer.valueOf(texto.substring(1)));
            if (objeto == null) throw new IllegalStateException("Referencia sin objeto: " + texto);
            return objeto;
        }
        return desescapar(texto);
    }

    // ===================== UTILIDADES =====================
    private static List<Field> campos(Class<?> clase) {
        Deque<Class<?>> jerarquia = new ArrayDeque<>();
        for (Class<?> k = clase; k != null && k != Object.class; k = k.getSuperclass()) jerarquia.addFirst(k);
        List<Field> campos = new ArrayList<>();
        for (Class<?> k : jerarquia) {
            for (Field campo : k.getDeclaredFields()) {
                int m = campo.getModifiers();
                if (Modifier.isStatic(m) || Modifier.isTransient(m) || campo.isSynthetic()) continue;
                campo.setAccessible(true);
                campos.add(campo);
            }
        }
        return campos;
    }

    private static Object leer(Field campo, Object objeto) {
        try { return campo.get(objeto); }
        catch (IllegalAccessException e) { throw new IllegalStateException("No se pudo leer " + campo.getName(), e); }
    }

    private static Object crearVacio(Class<?> clase) {
        try {
            Field campo = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            campo.setAccessible(true);
            Object unsafe = campo.get(null);
            Method metodo = unsafe.getClass().getMethod("allocateInstance", Class.class);
            return metodo.invoke(unsafe, clase);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear una instancia de " + clase.getName(), e);
        }
    }

    private static String escapar(String texto) {
        if (texto.isEmpty()) return "\\e";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '\\' || c == ';' || c == '|' || c == '=' || (c == '@' && i == 0)) sb.append('\\').append(c);
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String desescapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '\\' && i + 1 < texto.length()) {
                char siguiente = texto.charAt(++i);
                if (siguiente == 'n') sb.append('\n');
                else if (siguiente == 'r') sb.append('\r');
                else if (siguiente != 'e') sb.append(siguiente);
            } else sb.append(c);
        }
        return sb.toString();
    }

    private static List<String> partir(String texto, char separador) {
        List<String> partes = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '\\' && i + 1 < texto.length()) actual.append(c).append(texto.charAt(++i));
            else if (c == separador) { partes.add(actual.toString()); actual.setLength(0); }
            else actual.append(c);
        }
        partes.add(actual.toString());
        return partes;
    }
}