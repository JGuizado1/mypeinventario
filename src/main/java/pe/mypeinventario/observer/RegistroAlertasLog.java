package pe.mypeinventario.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Observador que deja constancia de cada alerta en el registro de la aplicación. */
public class RegistroAlertasLog implements ObservadorAlerta {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroAlertasLog.class);

    @Override
    public void alertaGenerada(EventoAlerta evento) {
        LOG.warn("Alerta de reabastecimiento #{} en la tienda {}: {}",
                evento.alertaId(), evento.tiendaId(), evento.mensaje());
    }
}
