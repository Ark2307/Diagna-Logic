package com.meetingiq.platform.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Shared SHA-256 hex digest, used both as {@code LlmResponseCache}'s cache
 * key and as {@code EmbeddingIndexService}'s chunk content-hash — the same
 * primitive, so it lives once rather than being copied into each caller.
 */
public final class Sha256 {

    private Sha256() {
    }

    public static String hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JDK distribution; this is unreachable.
            throw new IllegalStateException("SHA-256 MessageDigest unavailable", e);
        }
    }
}
