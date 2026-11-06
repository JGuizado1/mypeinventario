package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.ProveedorResumen;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.service.dto.SolicitudProveedor;

@SpringBootTest
@Transactional
class ProveedorServiceTest {

    @Autowired private ProveedorService proveedorService;

    private static SolicitudProveedor proveedor(String razon, String ruc, String correo) {
        return new SolicitudProveedor("Luis Mamani", correo, "987654321", razon, ruc);
    }

    @Test
    void registraYListaProveedoresOrdenadosPorRazonSocial() {
        proveedorService.registrarProveedor(proveedor("Textiles Gamarra SAC", "20123456789", "a@gamarra.pe"));
        ProveedorResumen creado = proveedorService.registrarProveedor(
                proveedor("Calzados Lima SRL", "20987654321", "b@lima.pe"));

        assertThat(creado.id()).isNotNull();
        assertThat(creado.contacto()).isEqualTo("Luis Mamani");
        assertThat(proveedorService.listarProveedores()).extracting(ProveedorResumen::razonSocial)
                .containsExactly("Calzados Lima SRL", "Textiles Gamarra SAC");
    }

    @Test
    void noSeRepiteElRucNiElCorreo() {
        proveedorService.registrarProveedor(proveedor("Textiles Gamarra SAC", "20123456789", "a@gamarra.pe"));

        assertThatThrownBy(() -> proveedorService.registrarProveedor(
                proveedor("Otra SAC", "20123456789", "otro@gamarra.pe")))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> proveedorService.registrarProveedor(
                proveedor("Otra SAC", "20999999999", "A@gamarra.pe")))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void lasSolicitudesInvalidasSeRechazan() {
        assertThatThrownBy(() -> proveedor("Textiles SAC", "123", "a@gamarra.pe"))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> proveedor(" ", "20123456789", "a@gamarra.pe"))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> proveedor("Textiles SAC", "20123456789", "sin-arroba"))
                .isInstanceOf(OperacionInvalidaException.class);
    }
}
