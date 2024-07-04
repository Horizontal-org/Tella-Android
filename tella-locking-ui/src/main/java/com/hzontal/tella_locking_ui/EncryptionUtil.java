package com.hzontal.tella_locking_ui;

import javax.crypto.spec.SecretKeySpec;

import android.os.Build;
import android.util.Log;

import javax.crypto.spec.SecretKeySpec;
import android.util.Log;

import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptionUtil {

    private static final int ITERATION_COUNT = 10000;
    private static final int KEY_LENGTH = 128; // 128-bit key
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String CIPHER_ALGORITHM = "AES";

    public static SecretKeySpec createKeyFromPassphrase(String passphrase, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, CIPHER_ALGORITHM);
    }

    public static void main(String[] args) {
        try {
            // Example passphrase
            String passphrase = "your_passphrase_here";
            // Example salt (you need to use the actual salt used in your Android app)
            byte[] salt = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                    0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10};

            // Generate the SecretKeySpec from the passphrase
            SecretKeySpec keySpec = createKeyFromPassphrase(passphrase, salt);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                System.out.println("Key: " + Base64.getEncoder().encodeToString(keySpec.getEncoded()));
            }

            // Logging the hexadecimal key (for verification purposes)
            byte[] keyBytes = keySpec.getEncoded();
            StringBuilder sb = new StringBuilder();
            for (byte b : keyBytes) {
                sb.append(String.format("%02x", b));
            }
            System.out.println("Hex Key: " + sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
