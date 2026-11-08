package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.ClienteResumen;
import pe.mypeinventario.domain.Tienda;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.exception.RecursoNoEncontradoException;
import pe.mypeinventario.repository.TiendaRepository;
import pe.mypeinventario.service.dto.SolicitudCliente;

@SpringBootTest
@Transactional
class ClienteServiceTest {

    @Autowired private ClienteService clienteService;
    @Autowired private TiendaRepository tiendaRepository;

    private Long tienda1Id;
    private Long tienda2Id;

    @BeforeEach
    void prepararDatos() {
        tienda1Id = tiendaRepository.save(new Tienda(null, "Tienda Centro", null, null)).getId();
        tienda2Id = tiendaRepository.save(new Tienda(null, "Tienda Norte", null, null)).getId();
    }

    @Test
    void registraYListaClientesOrdenadosPorNombre() {
        clienteService.registrarCliente(new SolicitudCliente(tienda1Id, "Zoila Paz", "987654321", "12345678"));
        clienteService.registrarCliente(new SolicitudCliente(tienda1Id, "Ana Soto", null, null));

        assertThat(clienteService.listarClientes(tienda1Id)).extracting(ClienteResumen::nombre)
                .containsExactly("Ana Soto", "Zoila Paz");
        assertThat(clienteService.listarClientes(tienda2Id)).isEmpty();
    }

    @Test
    void elDocumentoNoSeRepiteEnLaTiendaPeroSiEntreTiendas() {
        clienteService.registrarCliente(new SolicitudCliente(tienda1Id, "Ana Soto", null, "12345678"));

        assertThatThrownBy(() -> clienteService.registrarCliente(
                new SolicitudCliente(tienda1Id, "Otra Ana", null, "12345678")))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThat(clienteService.registrarCliente(
                new SolicitudCliente(tienda2Id, "Ana Soto", null, "12345678")).id()).isNotNull();
    }

    @Test
    void variosClientesSinDocumentoSonValidos() {
        clienteService.registrarCliente(new SolicitudCliente(tienda1Id, "Ana Soto", null, null));
        clienteService.registrarCliente(new SolicitudCliente(tienda1Id, "Beto Cruz", null, "  "));

        assertThat(clienteService.listarClientes(tienda1Id)).hasSize(2);
    }

    @Test
    void lasSolicitudesInvalidasSeRechazan() {
        assertThatThrownBy(() -> clienteService.registrarCliente(new SolicitudCliente(999_999L, "Ana", null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> clienteService.listarClientes(999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> new SolicitudCliente(tienda1Id, "Ana", null, "123"))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new SolicitudCliente(tienda1Id, "Ana", "12", null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new SolicitudCliente(tienda1Id, " ", null, null))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThatThrownBy(() -> new SolicitudCliente(null, "Ana", null, null))
                .isInstanceOf(OperacionInvalidaException.class);
    }
}
