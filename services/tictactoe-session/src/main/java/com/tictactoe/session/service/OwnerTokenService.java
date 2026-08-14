package com.tictactoe.session.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class OwnerTokenService {

    // SecureRandom, not the injected Random used by MoveStrategy implementations: session
    // ownership tokens are a security credential, so they need a cryptographically strong source
    // rather than the seeded/deterministic Random the rest of this codebase deliberately favors.
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public boolean matches(String presented, String storedHash) {
        if (presented == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(presented).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}