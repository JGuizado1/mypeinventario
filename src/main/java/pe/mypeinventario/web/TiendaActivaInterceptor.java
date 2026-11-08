package pe.mypeinventario.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.servlet.HandlerInterceptor;

/** Las pantallas que trabajan sobre una tienda exigen haber elegido una; si no, envía a elegirla. */
public class TiendaActivaInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (SesionWeb.desde(request).tienda().isPresent()) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/tiendas");
        return false;
    }
}
