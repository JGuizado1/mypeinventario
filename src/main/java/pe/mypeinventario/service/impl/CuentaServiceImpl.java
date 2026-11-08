package pe.mypeinventario.service.impl;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.UsuarioSesion;
import pe.mypeinventario.domain.Comerciante;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.repository.ComercianteRepository;
import pe.mypeinventario.repository.UsuarioRepository;
import pe.mypeinventario.seguridad.CodificadorContrasena;
import pe.mypeinventario.service.CuentaService;
import pe.mypeinventario.service.dto.SolicitudRegistro;

/** Implementación de {@link CuentaService}. */
@Service
@Transactional
public class CuentaServiceImpl implements CuentaService {

    private static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos";

    private final ComercianteRepository comercianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CodificadorContrasena codificador;

    public CuentaServiceImpl(ComercianteRepository comercianteRepository,
                             UsuarioRepository usuarioRepository,
                             CodificadorContrasena codificador) {
        this.comercianteRepository = comercianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.codificador = codificador;
    }

    @Override
    public UsuarioSesion registrarComerciante(SolicitudRegistro solicitud) {
        if (usuarioRepository.existsByCorreo(solicitud.correo())) {
            throw new OperacionInvalidaException("Ya existe una cuenta con ese correo");
        }
        Comerciante comerciante = comercianteRepository.save(new Comerciante(null, solicitud.nombre(),
                solicitud.correo(), solicitud.celular(), codificador.codificar(solicitud.contrasena()),
                LocalDate.now()));
        return aSesion(comerciante);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioSesion autenticar(String correo, String contrasena) {
        if (correo == null || contrasena == null) {
            throw new OperacionInvalidaException(CREDENCIALES_INVALIDAS);
        }
        return comercianteRepository.findByCorreo(correo.trim().toLowerCase())
                .filter(comerciante -> codificador.coincide(contrasena, comerciante.getContrasenaHash()))
                .map(CuentaServiceImpl::aSesion)
                .orElseThrow(() -> new OperacionInvalidaException(CREDENCIALES_INVALIDAS));
    }

    private static UsuarioSesion aSesion(Comerciante comerciante) {
        return new UsuarioSesion(comerciante.getId(), comerciante.getNombre(), comerciante.getCorreo());
    }
}
