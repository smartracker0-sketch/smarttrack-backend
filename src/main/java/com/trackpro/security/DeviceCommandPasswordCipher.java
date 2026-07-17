package com.trackpro.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeviceCommandPasswordCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final String encodedKey;
    private final SecureRandom random = new SecureRandom();

    public DeviceCommandPasswordCipher(@Value("${TRACKPRO_DEVICE_SECRET_KEY:}") String encodedKey) {
        this.encodedKey = encodedKey;
    }

    public String encrypt(String value) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt device command password", ex);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue);
            if (payload.length <= IV_LENGTH) throw new IllegalArgumentException("Invalid encrypted device command password");
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, iv);
            return new String(cipher.doFinal(payload, iv.length, payload.length - iv.length), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to decrypt device command password", ex);
        }
    }

    private Cipher cipher(int mode, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        return cipher;
    }

    private SecretKeySpec key() {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("TRACKPRO_DEVICE_SECRET_KEY must be set to use device command passwords");
        }
        byte[] decoded = Base64.getDecoder().decode(encodedKey);
        if (decoded.length != 32) throw new IllegalStateException("TRACKPRO_DEVICE_SECRET_KEY must be a base64-encoded 32-byte key");
        return new SecretKeySpec(decoded, "AES");
    }
}
