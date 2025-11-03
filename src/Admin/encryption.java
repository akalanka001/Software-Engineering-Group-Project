package Admin;

import java.security.MessageDigest;

/**
 * Utility class used to hash passwords before storing or comparing.
 * Replace or extend the algorithm to match what your database already uses
 * (commonly SHA‑256 or SHA‑512).
 */
public class encryption {

    public static String hashpassword(String plainText) throws Exception {
        // --- SHA‑256 example ---
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashed = md.digest(plainText.getBytes("UTF-8"));

        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}