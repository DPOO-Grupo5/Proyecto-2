package club.model;

import java.text.Normalizer;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import club.modelo.competencias.Baloncesto;
import club.modelo.competencias.Categoria;
import club.modelo.competencias.Competencia;
import club.modelo.competencias.DeporteConjunto;
import club.modelo.competencias.Equipo;
import club.modelo.competencias.Futbol;
import club.modelo.competencias.Voleibol;
import club.modelo.entrenamiento.DisciplinaIndividual;
import club.modelo.entrenamiento.InscripcionDisciplina;
import club.modelo.instalaciones.Instalacion;
import club.modelo.tienda.PagoMensualidad;
import club.modelo.tienda.Producto;
import club.modelo.usuarios.Empleado;
import club.modelo.usuarios.Entrenador;
import club.modelo.usuarios.Socio;
import club.modelo.usuarios.Usuario;

public class SistemaClub implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Usuario> usuarios = new HashMap<>();
    private List<Equipo> equipos = new ArrayList<>();
    private List<Competencia> competencias = new ArrayList<>();
    private List<Producto> productos = new ArrayList<>();
    private List<Instalacion> instalaciones = new ArrayList<>();
    private List<DisciplinaIndividual> disciplinas = new ArrayList<>();
    private List<PagoMensualidad> pagosMensualidad = new ArrayList<>();

    public void registrarUsuario(Usuario usuario) {
        if (usuario == null || usuario.getLogin() == null || usuario.getLogin().trim().isEmpty()) throw new IllegalArgumentException("El usuario debe tener login.");
        String login = usuario.getLogin().trim();
        usuario.setLogin(login);
        for (String registrado : usuarios.keySet()) if (registrado.equalsIgnoreCase(login)) throw new IllegalArgumentException("El login ya está registrado.");
        if (usuario instanceof club.modelo.usuarios.Socio) {
            String doc = ((club.modelo.usuarios.Socio) usuario).getDocumento();
            if (doc == null || doc.trim().isEmpty()) throw new IllegalArgumentException("El documento es obligatorio.");
            for (Usuario u : usuarios.values())
                if (u instanceof club.modelo.usuarios.Socio && doc.equalsIgnoreCase(((club.modelo.usuarios.Socio) u).getDocumento()))
                    throw new IllegalArgumentException("El documento ya está registrado.");
        }
        usuarios.put(login, usuario);
    }

    public Usuario autenticar(String login, String password) {
        if (login == null || password == null) return null;
        Usuario usuario = buscarUsuario(login);
        return usuario != null && usuario.validarPassword(password) ? usuario : null;
    }
    public void completarDatosFaltantes() {
        for (Usuario u : usuarios.values())
            if (u instanceof club.modelo.usuarios.Socio) {
                club.modelo.usuarios.Socio s = (club.modelo.usuarios.Socio) u;
                if (s.getDocumento() == null || s.getDocumento().trim().isEmpty()) s.setDocumento("SIN-DOC-" + s.getLogin());
            }
        for (Producto p : productos)
            if (p instanceof club.modelo.tienda.Snack && ((club.modelo.tienda.Snack) p).getAlergenos().isEmpty())
                ((club.modelo.tienda.Snack) p).setAlergenos(new ArrayList<>(java.util.Arrays.asList("Ninguno")));
    }

    public Usuario buscarUsuario(String login) {
        if (login == null) return null;
        for (Map.Entry<String, Usuario> entry : usuarios.entrySet()) if (entry.getKey().equalsIgnoreCase(login)) return entry.getValue();
        return null;
    }

    public void darDeBajaUsuario(String login) {
        Usuario usuario = buscarUsuario(login);
        if (usuario == null) throw new IllegalArgumentException("El usuario no está registrado.");
        usuarios.remove(usuario.getLogin());
    }

    public void registrarEquipo(Equipo equipo) {
        validarNoRegistrado(equipos, equipo, "equipo");
        for (Equipo e : equipos)
            if (e.getNombre().equalsIgnoreCase(equipo.getNombre()) && e.getDeporte() == equipo.getDeporte() && e.getCategoria() == equipo.getCategoria())
                throw new IllegalArgumentException("Ya existe un equipo con ese nombre, deporte y categoría.");
        equipos.add(equipo);
    }
    public void registrarCompetencia(Competencia competencia) {
        validarNoRegistrado(competencias, competencia, "competencia");
        for (Competencia existente : competencias) if (existente.getNombre().equalsIgnoreCase(competencia.getNombre()) && existente.getTemporada().equalsIgnoreCase(competencia.getTemporada())) throw new IllegalArgumentException("Ya hay una competencia con ese nombre en esa temporada.");
        competencias.add(competencia);
    }
    public void registrarProducto(Producto producto) {
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio.");
        for (Producto existente : productos) if (existente.getNombre().equalsIgnoreCase(producto.getNombre())) throw new IllegalArgumentException("Ya hay un producto con ese nombre.");
        agregarUnico(productos, producto, "producto");
    }
    public void registrarInstalacion(Instalacion instalacion) {
        validarNoRegistrado(instalaciones, instalacion, "instalación");
        DisciplinaIndividual disciplina = instalacion.getDisciplina();
        if (disciplina != null && !disciplinas.contains(disciplina)) disciplinas.add(disciplina);
        instalaciones.add(instalacion);
    }
    public void registrarDisciplina(DisciplinaIndividual disciplina) {
        validarNoRegistrado(disciplinas, disciplina, "disciplina");
        for (Instalacion instalacion : disciplina.getInstalaciones()) {
            if (instalacion.getDisciplina() != null && instalacion.getDisciplina() != disciplina) throw new IllegalStateException("Una instalación asociada pertenece a otra disciplina.");
        }
        disciplinas.add(disciplina);
        for (Instalacion instalacion : disciplina.getInstalaciones()) if (!instalaciones.contains(instalacion)) instalaciones.add(instalacion);
    }

    private <T> void agregarUnico(List<T> lista, T elemento, String tipo) {
        if (elemento == null) throw new IllegalArgumentException("El " + tipo + " es obligatorio.");
        if (lista.contains(elemento)) throw new IllegalArgumentException("El " + tipo + " ya está registrado.");
        lista.add(elemento);
    }

    private <T> void validarNoRegistrado(List<T> lista, T elemento, String tipo) {
        if (elemento == null) throw new IllegalArgumentException("El " + tipo + " es obligatorio.");
        if (lista.contains(elemento)) throw new IllegalArgumentException("El " + tipo + " ya está registrado.");
    }

    // ===================== MENSUALIDADES (HU-A16) =====================
    /** Registra el pago del mes para un socio del club; rechaza socios no registrados y meses ya pagados. */
    public PagoMensualidad registrarPagoMensualidad(Socio socio, YearMonth mes, double valor) {
        if (socio == null || buscarUsuario(socio.getLogin()) != socio) throw new IllegalArgumentException("El socio no está registrado en el club.");
        for (PagoMensualidad pago : pagosMensualidad)
            if (pago.corresponde(socio, mes)) throw new IllegalStateException("El socio ya tiene registrado el pago de " + mes + ".");
        PagoMensualidad pago = new PagoMensualidad(socio, mes, valor);
        pagosMensualidad.add(pago);
        return pago;
    }

    public List<PagoMensualidad> getPagosMensualidad() { return pagosMensualidad; }

    // ===================== CONSULTAS POR TIPO =====================
    public List<Socio> getSocios() { return usuariosDeTipo(Socio.class); }
    public List<Empleado> getEmpleados() { return usuariosDeTipo(Empleado.class); }
    public List<Entrenador> getEntrenadores() { return usuariosDeTipo(Entrenador.class); }

    private <T extends Usuario> List<T> usuariosDeTipo(Class<T> tipo) {
        List<T> lista = new ArrayList<>();
        for (Usuario u : usuarios.values()) if (tipo.isInstance(u)) lista.add(tipo.cast(u));
        lista.sort((a, b) -> a.getLogin().compareToIgnoreCase(b.getLogin()));
        return lista;
    }

    public List<Equipo> getEquiposActivos() {
        List<Equipo> activos = new ArrayList<>();
        for (Equipo e : equipos) if (e.isActivo()) activos.add(e);
        return activos;
    }

    // ===================== DEPORTES Y CATEGORÍAS =====================
    /**
     * Devuelve el deporte de conjunto con ese nombre que ya usa algún equipo o competencia, o uno nuevo.
     * Los equipos y competencias comparan el deporte por identidad, por eso se reutiliza la misma instancia.
     */
    public DeporteConjunto obtenerDeporte(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El deporte es obligatorio.");
        String clave = sinTildes(nombre);
        for (Equipo e : equipos) if (e.getDeporte() != null && sinTildes(e.getDeporte().getNombre()).equals(clave)) return e.getDeporte();
        for (Competencia c : competencias) if (sinTildes(c.getDeporte().getNombre()).equals(clave)) return c.getDeporte();
        switch (clave) {
            case "futbol": return new Futbol("Fútbol", 7);
            case "baloncesto": return new Baloncesto("Baloncesto", 5);
            case "voleibol": return new Voleibol("Voleibol", 6);
            default: throw new IllegalArgumentException("Deporte no reconocido: " + nombre);
        }
    }

    /** Igual que {@link #obtenerDeporte(String)}, para las categorías por edad. */
    public Categoria obtenerCategoria(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("La categoría es obligatoria.");
        for (Equipo e : equipos) if (e.getCategoria() != null && e.getCategoria().getNombre().equalsIgnoreCase(nombre.trim())) return e.getCategoria();
        for (Competencia c : competencias) if (c.getCategoria().getNombre().equalsIgnoreCase(nombre.trim())) return c.getCategoria();
        switch (nombre.trim()) {
            case "Sub-13": return new Categoria("Sub-13", 0, 12);
            case "Sub-15": return new Categoria("Sub-15", 13, 14);
            case "Sub-17": return new Categoria("Sub-17", 15, 17);
            case "Mayores": return new Categoria("Mayores", 18, 150);
            default: throw new IllegalArgumentException("Categoría no reconocida: " + nombre);
        }
    }
    
    public int posicionRanking(DisciplinaIndividual d, club.modelo.usuarios.Socio socio) {
        InscripcionDisciplina mia = socio.inscripcionEn(d);
        if (mia == null || !mia.isActiva()) throw new IllegalStateException("No estás inscrito en esa disciplina.");
        int pos = 1;
        for (Usuario u : usuarios.values())
            if (u instanceof club.modelo.usuarios.Socio && u != socio) {
                InscripcionDisciplina o = ((club.modelo.usuarios.Socio) u).inscripcionEn(d);
                if (o != null && o.isActiva() && o.getRanking() > mia.getRanking()) pos++;
            }
        return pos;
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public Map<String, Usuario> getUsuarios() { return usuarios; }
    public List<Equipo> getEquipos() { return equipos; }
    public List<Competencia> getCompetencias() { return competencias; }
    public List<Producto> getProductos() { return productos; }
    public List<Instalacion> getInstalaciones() { return instalaciones; }
    public List<DisciplinaIndividual> getDisciplinas() { return disciplinas; }
}
