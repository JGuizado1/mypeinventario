package pe.mypeinventario.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import pe.mypeinventario.consulta.TiendaResumen;
import pe.mypeinventario.consulta.UsuarioSesion;

/**
 * Lo que la web recuerda de quien ingresó: su usuario y la tienda con la que trabaja.
 * Concentra los nombres de los atributos de la sesión HTTP en un solo lugar.
 */
public final class SesionWeb {

    private static final String USUARIO = "usuario";
    private static final String TIENDA = "tienda";

    private final HttpSession session;

    private SesionWeb(HttpSession session) {
        this.session = session;
    }

    /** @param session sesión HTTP; puede ser {@code null} si la persona aún no tiene sesión */
    public static SesionWeb de(HttpSession session) {
        return new SesionWeb(session);
    }

    /** Usa la sesión existente, sin crear una nueva. */
    public static SesionWeb desde(HttpServletRequest request) {
        return new SesionWeb(request.getSession(false));
    }

    public Optional<UsuarioSesion> usuario() {
        return atributo(USUARIO, UsuarioSesion.class);
    }

    public UsuarioSesion exigirUsuario() {
        return usuario().orElseThrow(() -> new IllegalStateException("No hay una sesión iniciada"));
    }

    public Optional<TiendaResumen> tienda() {
        return atributo(TIENDA, TiendaResumen.class);
    }

    public TiendaResumen exigirTienda() {
        return tienda().orElseThrow(() -> new IllegalStateException("No hay una tienda activa"));
    }

    /** Guarda al usuario que ingresó y olvida la tienda de una sesión anterior. */
    public void iniciar(UsuarioSesion usuario) {
        session.setAttribute(USUARIO, usuario);
        session.removeAttribute(TIENDA);
    }

    public void elegirTienda(TiendaResumen tienda) {
        session.setAttribute(TIENDA, tienda);
    }

    public void cerrar() {
        if (session != null) {
            session.invalidate();
        }
    }

    private <T> Optional<T> atributo(String nombre, Class<T> tipo) {
        if (session == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(session.getAttribute(nombre)).map(tipo::cast);
    }
}
