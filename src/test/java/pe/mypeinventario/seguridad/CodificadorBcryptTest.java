package pe.mypeinventario.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodificadorBcryptTest {

    private final CodificadorContrasena codificador = new CodificadorBcrypt();

    @Test
    void elHashNoEsLaContrasenaYCadaVezEsDistinto() {
        String primero = codificador.codificar("clave-segura-1");
        String segundo = codificador.codificar("clave-segura-1");

        assertThat(primero).isNotEqualTo("clave-segura-1").isNotEqualTo(segundo);
        assertThat(primero).startsWith("$2");
    }

    @Test
    void soloCoincideLaContrasenaCorrecta() {
        String hash = codificador.codificar("clave-segura-1");

        assertThat(codificador.coincide("clave-segura-1", hash)).isTrue();
        assertThat(codificador.coincide("otra-clave", hash)).isFalse();
        assertThat(codificador.coincide(null, hash)).isFalse();
        assertThat(codificador.coincide("clave-segura-1", null)).isFalse();
    }
}
