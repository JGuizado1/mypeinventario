package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.UsuarioSesion;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.repository.ComercianteRepository;
import pe.mypeinventario.seguridad.CodificadorContrasena;
import pe.mypeinventario.service.dto.SolicitudProveedor;
import pe.mypeinventario.service.dto.SolicitudRegistro;

/** Registro e ingreso con la base de datos real (H2) y BCrypt. */
@SpringBootTest
@Transactional
class CuentaServiceTest {

    @Autowired private CuentaService cuentaService;
    @Autowired private ProveedorService proveedorService;
    @Autowired private ComercianteRepository comercianteRepository;
    @Autowired private CodificadorContrasena codificador;

    private static SolicitudRegistro registro(String correo) {
        return new SolicitudRegistro("Rosa Quispe", correo, "987654321", "clave-segura-1");
    }

    @Test
    void laContrasenaSeGuardaCifradaYElCorreoEnMinusculas() {
        UsuarioSesion sesion = cuentaService.registrarComerciante(registro("Rosa@Correo.PE"));

        assertThat(sesion.id()).isNotNull();
        assertThat(sesion.correo()).isEqualTo("rosa@correo.pe");
        String hash = comercianteRepository.findByCorreo("rosa@correo.pe").orElseThrow().getContrasenaHash();
        assertThat(hash).isNotEqualTo("clave-segura-1");
        assertThat(codificador.coincide("clave-segura-1", hash)).isTrue();
    }

    @Test
    void noSePuedeRepetirElCorreoSinImportarMayusculas() {
        cuentaService.registrarComerciante(registro("rosa@correo.pe"));

        assertThatThrownBy(() -> cuentaService.registrarComerciante(registro("ROSA@correo.pe")))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void unCorreoDeProveedorTampocoPuedeSerDeUnComerciante() {
        proveedorService.registrarProveedor(new SolicitudProveedor(
                "Luis Mamani", "ventas@gamarra.pe", null, "Textiles Gamarra SAC", "20123456789"));

        assertThatThrownBy(() -> cuentaService.registrarComerciante(registro("ventas@gamarra.pe")))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void ingresaConLasCredencialesCorrectas() {
        UsuarioSesion creada = cuentaService.registrarComerciante(registro("rosa@correo.pe"));

        UsuarioSesion sesion = cuentaService.autenticar("Rosa@Correo.pe", "clave-segura-1");

        assertThat(sesion.id()).isEqualTo(creada.id());
        assertThat(sesion.nombre()).isEqualTo("Rosa Quispe");
    }

    @Test
    void rechazaLasCredencialesIncorrectas() {
        cuentaService.registrarComerciante(registro("rosa@correo.pe"));

        assertThatThrownBy(() -> cuentaService.autenticar("rosa@correo.pe", "clave-equivocada"))
                .isInstanceOf(OperacionInvalidaException.class).hasMessage("Correo o contraseña incorrectos");
        assertThatThrownBy(() -> cuentaService.autenticar("nadie@correo.pe", "clave-segura-1"))
                .isInstanceOf(OperacionInvalidaException.class).hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void unProveedorNoPuedeIngresarAlSistema() {
        proveedorService.registrarProveedor(new SolicitudProveedor(
                "Luis Mamani", "ventas@gamarra.pe", null, "Textiles Gamarra SAC", "20123456789"));

        assertThatThrownBy(() -> cuentaService.autenticar("ventas@gamarra.pe", "cualquier-clave"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void lasSolicitudesInvalidasSeRechazan() {
        assertThatThrownBy(() -> new SolicitudRegistro("Rosa", "no-es-correo", null, "clave-segura-1"))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new SolicitudRegistro("Rosa", "rosa@correo.pe", null, "corta"))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new SolicitudRegistro(" ", "rosa@correo.pe", null, "clave-segura-1"))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new SolicitudRegistro("Rosa", "rosa@correo.pe", "123", "clave-segura-1"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void laSolicitudNoMuestraLaContrasenaAlImprimirse() {
        assertThat(registro("rosa@correo.pe").toString()).doesNotContain("clave-segura-1");
    }
}
