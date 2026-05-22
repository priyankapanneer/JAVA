package com.examportal.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {
    private static final Map<String, Integer> sessions = new HashMap<>(); // token -> userId
    private static final Map<String, Long> sessionExpiry = new HashMap<>();
    private static final long SESSION_DURATION = 24 * 60 * 60 * 1000L; // 24 hours

    public static String createSession(int userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);
        sessionExpiry.put(token, System.currentTimeMillis() + SESSION_DURATION);
        return token;
    }

    public static Integer getUserIdFromToken(String token) {
        if (token == null || !sessions.containsKey(token)) return null;
        Long expiry = sessionExpiry.get(token);
        if (expiry == null || System.currentTimeMillis() > expiry) {
            sessions.remove(token);
            sessionExpiry.remove(token);
            return null;
        }
        return sessions.get(token);
    }

    public static void invalidateSession(String token) {
        sessions.remove(token);
        sessionExpiry.remove(token);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
