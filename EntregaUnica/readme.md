# Club Deportivo Séneca · Proyecto 2 (Entrega única)

Diseño y Programación Orientada a Objetos, Universidad de los Andes.
Aplicación Java del Club Deportivo Séneca con tres interfaces de consola (socio, empleado y
administrador), persistencia en archivos CSV y pruebas automáticas con JUnit.

**Integrantes:** Yury Dayana Velasquez Alvarez, Nelson Fabian Reyes Marquez y Juan José Sarmiento Bello.

> **Estado:** en construcción. La base es el código final de la Entrega 2 del Proyecto 1
> (commit `6c22a14` del repositorio `DPOO-Grupo5/Proyecto-1`). El avance frente al banco oficial de
> historias de usuario está en `aplicacionClubDeportivo/ClubDeportivo/docs/MATRIZ_TRAZABILIDAD_HU.md`.

## Contenido de esta carpeta

| Carpeta o archivo | Qué contiene |
|---|---|
| `aplicacionClubDeportivo/ClubDeportivo` | Proyecto de Eclipse (sin Maven ni Gradle) |
| `.../ClubDeportivo/src` | Código fuente de la aplicación |
| `.../ClubDeportivo/test` | Pruebas JUnit (unitarias y de integración por historia de usuario) |
| `.../ClubDeportivo/datos_seneca` | Datos persistidos que cargan y guardan las tres aplicaciones |
| `.../ClubDeportivo/docs` | Documento de diseño y matriz de trazabilidad |
| `Diagramas/` | Diagramas de clases (alto nivel y detallado) y de secuencia, con fuente `.drawio` e imagen `.png` |
| `Prompts-IAG.md` | Anexo de uso de inteligencia artificial generativa |

### Paquetes

| Paquete | Responsabilidad |
|---|---|
| `club.consola` | Interfaz de consola. `MainSocio`, `MainEmpleado` y `MainAdministrador` (un `main` por tipo de usuario) heredan de `ConsolaBase`, que se encarga de cargar y guardar los datos, el inicio de sesión por rol y el ciclo del menú. `EntradaConsola` lee y valida las entradas; `Menu` define las opciones. |
| `club.model` | `SistemaClub`: registro central de usuarios, equipos, competencias, productos, instalaciones y disciplinas |
| `club.modelo.*` | Lógica del club: usuarios, competencias, entrenamiento, instalaciones y tienda |
| `club.persistencia` | `ArchivoPlano` (lectura y escritura de los CSV) y `DatosIniciales` (datos de demostración) |

La consola no contiene reglas del club: pide los datos, llama a la lógica y muestra el resultado o el
motivo del rechazo (las reglas se comunican mediante excepciones).

## Cómo ejecutarlo en Eclipse

Se necesita un JDK 17 o superior.

1. *File → Import → General → Existing Projects into Workspace* y seleccionar la carpeta
   `aplicacionClubDeportivo/ClubDeportivo`.
2. Ejecutar la aplicación del rol deseado con *Run As → Java Application*:
   - `src/club/consola/MainSocio.java`
   - `src/club/consola/MainEmpleado.java` (entrenadores y fisioterapeutas)
   - `src/club/consola/MainAdministrador.java`

Cada aplicación solo admite usuarios de su rol. Los datos se cargan automáticamente de
`datos_seneca/` al iniciar y se guardan en esa misma carpeta después de cada operación y al salir.
Eclipse ejecuta desde la carpeta del proyecto, que es donde está `datos_seneca/`.

En cualquier pregunta se puede escribir `cancelar` para abandonar la operación en curso. Si una
entrada tiene un formato inválido (número, fecha u opción), la aplicación explica el error y vuelve a
preguntar.

### Datos de demostración

`datos_seneca/` trae un estado coherente con las reglas implementadas, para recorrer las historias sin preparar
datos a mano. Si se borra la carpeta, la siguiente ejecución lo vuelve a crear (`persistencia/DatosIniciales`);
la prueba `DatosInicialesTest` verifica que estos datos cumplen las reglas.

| Login | Contraseña | Aplicación | Qué tiene |
|---|---|---|---|
| `admin` | `admin123` | MainAdministrador | Administrador |
| `entrenador.demo` … `entrenador5.demo` | `entre123` | MainEmpleado | Entrenadores con turnos de lunes a sábado que cumplen la cobertura; `entrenador.demo` (código `ENTRE15`) es responsable del partido oficial programado |
| `fisio.demo`, `fisio2.demo` | `fisio123` | MainEmpleado | Fisioterapeutas (mañana y tarde) |
| `socio.demo` | `demo123` | MainSocio | Ana Torres: equipo Séneca Fútbol A, tenis (Intermedio) y pádel |
| `socio.demo2` | `demo123` | MainSocio | Juan Medina: equipo Séneca Fútbol B y tenis (Avanzado) |
| `jugador01` … `jugador14` | `demo123` | MainSocio | Integrantes de los equipos A y B |

También hay instalaciones de tenis, pádel, tenis de mesa (techada y exterior) y fútbol; productos de tienda y
cafetería (con alérgenos); y la competencia **Copa Universitaria (ASCUN 2026)**: el equipo A contra dos
universidades, con un partido ya programado y dos propuestos que esperan el visto bueno del administrador.

## Pruebas (JUnit 4)

El proyecto ya tiene configurada la carpeta `test` y la biblioteca JUnit 4 de Eclipse.
Para ejecutar todas las pruebas: clic derecho sobre `test/club/TodasLasPruebas.java` →
*Run As → JUnit Test*.

| Clase | Tipo | Qué verifica |
|---|---|---|
| `club.model.SistemaClubTest` | Unitaria | Autenticación, logins únicos, consultas por tipo, deportes y categorías |
| `club.persistencia.ArchivoPlanoTest` | Unitaria / integración | Ciclo completo de persistencia: guardar, cargar, conservar relaciones y seguir operando |
| `club.historias.Historias*Test` | Integración | Criterios de aceptación de las historias del banco oficial; cada método lleva el ID de la historia (p. ej. `s03_rechazoPorCupo_...`) |

Las pruebas no usan la carpeta `datos_seneca`: trabajan en memoria o en carpetas temporales.

### Desde la terminal (opcional)

Desde `aplicacionClubDeportivo/ClubDeportivo`, con `junit-4.13.2.jar` y `hamcrest-core-1.3.jar` en `lib/`:

```powershell
$src = Get-ChildItem src -Recurse -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -d bin $src
$tst = Get-ChildItem test -Recurse -Filter *.java | ForEach-Object FullName
javac -encoding UTF-8 -cp "bin;lib/*" -d bin-test $tst
java -cp "bin;bin-test;lib/*" org.junit.runner.JUnitCore club.TodasLasPruebas
java -Dfile.encoding=UTF-8 -cp bin club.consola.MainSocio
```

## Persistencia

- `ArchivoPlano` guarda el estado en un CSV por clase, más un `indice.csv`. Las referencias entre
  objetos se guardan por identificador, de modo que al cargar se recuperan las mismas relaciones.
- La carpeta es `datos_seneca`, relativa al directorio de trabajo. Se puede cambiar con
  `-Dclub.datos=RUTA` en los argumentos de la VM (las pruebas no lo necesitan).
- Las contraseñas se guardan con hash PBKDF2 con sal.

## Diagramas

- [Diagrama de alto nivel](Diagramas/Diagrama_alto_nivel.drawio.png)
- [Diagrama de diseño con métodos](Diagramas/Diagrama_de_dise%C3%B1o_con_m%C3%A9todos.drawio.png)
- [Diagrama detallado adicional](Diagramas/Proyecto_DPOO_Entrega2.drawio.png)
- [Diagramas de secuencia](Diagramas/Diagramas%20de%20secuencia/PNG/)

Los diagramas todavía corresponden a la Entrega 2 y se actualizarán con los cambios del Proyecto 2
(consola por rol, torneos y ajustes al banco oficial).
