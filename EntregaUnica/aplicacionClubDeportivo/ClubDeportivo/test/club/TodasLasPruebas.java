package club;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/** Ejecuta todas las pruebas del proyecto (en Eclipse: Run As → JUnit Test sobre esta clase). */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    club.model.SistemaClubTest.class,
    club.persistencia.ArchivoPlanoTest.class,
    club.persistencia.DatosInicialesTest.class,
    club.historias.HistoriasSocioTest.class,
    club.historias.HistoriasEmpleadoTest.class,
    club.historias.HistoriasAdministradorTest.class,
    club.historias.HistoriasEntrenadorTest.class,
    club.historias.HistoriasComprasTest.class,
    club.historias.HistoriasTurnosAdministradorTest.class,
    club.historias.HistoriasPracticaTest.class,
    club.historias.HistoriasPartidoInternoTest.class,
    club.historias.HistoriasSesionesTest.class,
})
public class TodasLasPruebas { }
