/*
 * Ayudas de las pantallas de venta y de compra. El servidor sigue siendo quien calcula y
 * valida: esto solo filtra la tabla y muestra un total estimado mientras se escribe.
 */
(function () {
    "use strict";

    function numero(texto) {
        var valor = parseFloat(String(texto).replace(",", "."));
        return isNaN(valor) ? 0 : valor;
    }

    var filtro = document.querySelector("[data-filtro]");
    if (filtro) {
        var tabla = document.getElementById(filtro.getAttribute("data-filtro"));
        filtro.addEventListener("input", function () {
            var texto = filtro.value.trim().toLowerCase();
            tabla.querySelectorAll("tbody tr").forEach(function (fila) {
                fila.hidden = texto !== "" && fila.textContent.toLowerCase().indexOf(texto) === -1;
            });
        });
    }

    var formulario = document.getElementById("form-venta") || document.getElementById("form-compra");
    if (!formulario) {
        return;
    }
    var campos = formulario.querySelectorAll(".cantidad-linea");
    var unidades = document.getElementById("resumen-unidades");
    var total = document.getElementById("resumen-total");
    var porcentaje = formulario.elements.descuentoPorcentaje;
    var monto = formulario.elements.descuentoMonto;

    /** El precio de la fila: el costo escrito (compras) o el precio del producto (ventas). */
    function precioDe(campo) {
        var costo = campo.closest("tr").querySelector(".costo-linea");
        return costo ? numero(costo.value) : numero(campo.getAttribute("data-precio"));
    }

    function recalcular() {
        var cantidadTotal = 0;
        var importe = 0;
        campos.forEach(function (campo) {
            var cantidad = parseInt(campo.value, 10);
            if (isNaN(cantidad) || cantidad < 0) {
                cantidad = 0;
            }
            cantidadTotal += cantidad;
            importe += cantidad * precioDe(campo);
        });
        if (porcentaje) {
            importe -= importe * Math.min(Math.max(numero(porcentaje.value), 0), 100) / 100;
        }
        if (monto) {
            importe -= Math.max(numero(monto.value), 0);
        }
        unidades.textContent = String(cantidadTotal);
        total.textContent = Math.max(importe, 0).toFixed(2);
    }

    formulario.addEventListener("input", recalcular);
    recalcular();
})();
