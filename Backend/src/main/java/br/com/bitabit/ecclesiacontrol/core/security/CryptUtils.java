package br.com.bitabit.ecclesiacontrol.core.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public final class CryptUtils {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;    // bytes
    private static final int GCM_TAG_LENGTH = 128;  // bits

    // Uma única instância: é segura para gerar IV, não bloqueia e é barata.
    // (getInstanceStrong() a cada chamada pode bloquear em contêiner com pouca entropia.)
    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptUtils() {
    }

    /**
     * A chave só é lida na primeira vez que é usada. Para falhar na SUBIDA e não no primeiro CPF salvo,
     * a aplicação chama {@link #assertConfigured()} (ver EncryptionKeyStartupCheck).
     */
    private static final class KeyHolder {
        private static final SecretKey KEY = parseKey(System.getenv("ENCRYPT_KEY"));
    }

    /** Força a leitura e a validação da ENCRYPT_KEY, com uma mensagem clara quando ela está errada. */
    public static void assertConfigured() {
        try {
            KeyHolder.KEY.getEncoded();
        } catch (ExceptionInInitializerError e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException(cause.getMessage(), cause);
        }
    }

    /** Visível ao pacote para ser testada sem depender da variável de ambiente. */
    static SecretKey parseKey(String keyString) {
        if (keyString == null || keyString.isBlank()) {
            throw new IllegalStateException(
                    "ENCRYPT_KEY não definida. Configure a variável de ambiente antes de iniciar a aplicação "
                            + "(nunca use uma chave fixa no código, mesmo para testes). "
                            + "Gere uma com: openssl rand -base64 32");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(keyString.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "ENCRYPT_KEY não é um Base64 válido. Gere uma com: openssl rand -base64 32", e);
        }
        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
            throw new IllegalStateException(
                    "ENCRYPT_KEY deve ser Base64 de 16, 24 ou 32 bytes, mas decodifica para "
                            + decoded.length + " bytes. Gere uma com: openssl rand -base64 32");
        }
        return new SecretKeySpec(decoded, "AES");
    }

    public static String encrypt(String value) {
        if (value == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, KeyHolder.KEY, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            // Concatena IV + ciphertext: o IV é necessário para decriptar depois
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv).put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Error encrypting value", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String encryptedValue) {
        if (encryptedValue == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedValue);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, KeyHolder.KEY, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting value", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // Índice determinístico para busca por igualdade (ex.: checar CPF duplicado)
    // sem expor o valor em texto puro. HMAC-SHA256 é one-way: não dá para reverter.
    // Limitação conhecida: a mesma chave serve à criptografia e ao HMAC. Antes de produção, derivar
    // subchaves separadas (HKDF) e recalcular os cpf_hash existentes.
    public static String hashForLookup(String value) {
        if (value == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(KeyHolder.KEY.getEncoded(), "HmacSHA256"));
            byte[] hash = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}