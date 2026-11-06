package pe.mypeinventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.mypeinventario.consulta.UsuarioSesion;
import pe.mypeinventario.domain.Comerciante;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.repository.ComercianteRepository;
import pe.mypeinventario.repository.UsuarioRepository;
import pe.mypeinventario.seguridad.CodificadorContrasena;
import pe.mypeinventario.service.dto.SolicitudRegistro;
import pe.mypeinventario.service.impl.CuentaServiceImpl;

/** Prueba unitaria con mocks: no levanta Spring ni la base de datos. */
@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock private ComercianteRepository comercianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CodificadorContrasena codificador;

    private CuentaServiceImpl servicio() {
        return new CuentaServiceImpl(comercianteRepository, usuarioRepository, codificador);
    }

    private static SolicitudRegistro solicitud() {
        return new SolicitudRegistro("Rosa Quispe", "Rosa@Correo.PE", "987654321", "clave-segura-1");
    }

    @Test
    void registraAlComercianteConLaContrasenaCifrada() {
        when(usuarioRepository.existsByCorreo("rosa@correo.pe")).thenReturn(false);
        when(codificador.codificar("clave-segura-1")).thenReturn("HASH");
        when(comercianteRepository.save(any(Comerciante.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioSesion sesion = servicio().registrarComerciante(solicitud());

        ArgumentCaptor<Comerciante> guardado = ArgumentCaptor.forClass(Comerciante.class);
        verify(comercianteRepository).save(guardado.capture());
        assertThat(guardado.getValue().getContrasenaHash()).isEqualTo("HASH");
        assertThat(guardado.getValue().getCorreo()).isEqualTo("rosa@correo.pe");
        assertThat(sesion.correo()).isEqualTo("rosa@correo.pe");
    }

    @Test
    void noRegistraSiElCorreoYaExiste() {
        when(usuarioRepository.existsByCorreo("rosa@correo.pe")).thenReturn(true);

        assertThatThrownBy(() -> servicio().registrarComerciante(solicitud()))
                .isInstanceOf(OperacionInvalidaException.class);
        verify(comercianteRepository, never()).save(any());
    }

    @Test
    void ingresaConLaContrasenaCorrecta() {
        Comerciante rosa = new Comerciante(7L, "Rosa Quispe", "rosa@correo.pe", null, "HASH", null);
        when(comercianteRepository.findByCorreo("rosa@correo.pe")).thenReturn(Optional.of(rosa));
        when(codificador.coincide("clave-segura-1", "HASH")).thenReturn(true);

        UsuarioSesion sesion = servicio().autenticar("  Rosa@Correo.PE ", "clave-segura-1");

        assertThat(sesion.id()).isEqualTo(7L);
    }

    @Test
    void laContrasenaIncorrectaYElCorreoDesconocidoDanElMismoMensaje() {
        Comerciante rosa = new Comerciante(7L, "Rosa Quispe", "rosa@correo.pe", null, "HASH", null);
        when(comercianteRepository.findByCorreo("rosa@correo.pe")).thenReturn(Optional.of(rosa));
        when(codificador.coincide("mala", "HASH")).thenReturn(false);
        when(comercianteRepository.findByCorreo("nadie@correo.pe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio().autenticar("rosa@correo.pe", "mala"))
                .hasMessage("Correo o contraseña incorrectos");
        assertThatThrownBy(() -> servicio().autenticar("nadie@correo.pe", "cualquiera"))
                .hasMessage("Correo o contraseña incorrectos");
        assertThatThrownBy(() -> servicio().autenticar(null, "x")).hasMessage("Correo o contraseña incorrectos");
        assertThatThrownBy(() -> servicio().autenticar("rosa@correo.pe", null))
                .hasMessage("Correo o contraseña incorrectos");
    }
}
