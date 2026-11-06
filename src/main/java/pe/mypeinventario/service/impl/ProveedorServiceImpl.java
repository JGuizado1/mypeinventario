package pe.mypeinventario.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.mypeinventario.consulta.ProveedorResumen;
import pe.mypeinventario.domain.ProveedorMayorista;
import pe.mypeinventario.exception.OperacionInvalidaException;
import pe.mypeinventario.repository.ProveedorMayoristaRepository;
import pe.mypeinventario.repository.UsuarioRepository;
import pe.mypeinventario.seguridad.CodificadorContrasena;
import pe.mypeinventario.service.ProveedorService;
import pe.mypeinventario.service.dto.SolicitudProveedor;

/** Implementación de {@link ProveedorService}. */
@Service
@Transactional
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorMayoristaRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final CodificadorContrasena codificador;

    public ProveedorServiceImpl(ProveedorMayoristaRepository proveedorRepository,
                                UsuarioRepository usuarioRepository,
                                CodificadorContrasena codificador) {
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.codificador = codificador;
    }

    @Override
    public ProveedorResumen registrarProveedor(SolicitudProveedor solicitud) {
        if (proveedorRepository.existsByRuc(solicitud.ruc())) {
            throw new OperacionInvalidaException("Ya existe un proveedor con el RUC " + solicitud.ruc());
        }
        if (usuarioRepository.existsByCorreo(solicitud.correo())) {
            throw new OperacionInvalidaException("Ya existe una cuenta con ese correo");
        }
        // Todo usuario guarda un hash. El proveedor no ingresa al sistema, así que su hash es el
        // de una contraseña aleatoria que nadie conoce: no hay forma de usarlo para entrar.
        String hashInutilizable = codificador.codificar(UUID.randomUUID().toString());
        return aResumen(proveedorRepository.save(new ProveedorMayorista(null, solicitud.contacto(),
                solicitud.correo(), solicitud.celular(), hashInutilizable,
                solicitud.razonSocial(), solicitud.ruc())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResumen> listarProveedores() {
        return proveedorRepository.findAll().stream()
                .sorted(Comparator.comparing(ProveedorMayorista::getRazonSocial, String.CASE_INSENSITIVE_ORDER))
                .map(ProveedorServiceImpl::aResumen)
                .toList();
    }

    private static ProveedorResumen aResumen(ProveedorMayorista proveedor) {
        return new ProveedorResumen(proveedor.getId(), proveedor.getRazonSocial(), proveedor.getRuc(),
                proveedor.getNombre(), proveedor.getCorreo(), proveedor.getCelular());
    }
}
