package br.com.bitabit.ecclesiacontrol.core.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptUtilsKeyTest {

    private static String base64OfBytes(int size) {
        return Base64.getEncoder().encodeToString(new byte[size]);
    }

    @Test
    void acceptsTheThreeAesKeySizes() {
        for (int size : new int[]{16, 24, 32}) {
            assertThat(CryptUtils.parseKey(base64OfBytes(size)).getEncoded()).hasSize(size);
        }
    }

    // Regressão do CI: a chave antiga decodificava para 41 bytes e só falhava ao salvar o primeiro CPF.
    @Test
    void rejectsAKeyWhoseDecodedSizeIsNotAnAesSize() {
        assertThatThrownBy(() -> CryptUtils.parseKey(base64OfBytes(41)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("41 bytes");
    }

    @Test
    void rejectsMissingBlankOrInvalidBase64() {
        assertThatThrownBy(() -> CryptUtils.parseKey(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CryptUtils.parseKey("   ")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CryptUtils.parseKey("%%%nao-e-base64%%%")).isInstanceOf(IllegalStateException.class);
    }
}