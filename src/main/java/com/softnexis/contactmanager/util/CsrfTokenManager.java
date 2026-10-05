package com.softnexis.contactmanager.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles CSRF token generation and validation to safeguard contact submissions
 * and prevent cross-site request forgery and duplicate submissions.
 */
public final class CsrfTokenManager {
    public static final String CSRF_TOKEN_SESSION_ATTR = "CSRF_TOKEN";
    public static final String CSRF_TOKEN_PARAM = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokenManager() {}

    /**
     * Generates a new cryptographically strong CSRF token or returns the existing session token.
     */
    public static String getToken(HttpSession session) {
        if (session == null) {
            return "";
        }
        String token = (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
        if (token == null || token.isEmpty()) {
            token = generateNewToken();
            session.setAttribute(CSRF_TOKEN_SESSION_ATTR, token);
        }
        return token;
    }

    /**
     * Regenerates a new CSRF token in the session after state changes to avoid duplicate submissions.
     */
    public static String regenerateToken(HttpSession session) {
        if (session == null) {
            return "";
        }
        String token = generateNewToken();
        session.setAttribute(CSRF_TOKEN_SESSION_ATTR, token);
        return token;
    }

    /**
     * Verifies whether the request parameter token matches the session token.
     */
    public static boolean isValidToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
        String requestToken = request.getParameter(CSRF_TOKEN_PARAM);

        if (sessionToken == null || requestToken == null) {
            return false;
        }
        return sessionToken.equals(requestToken.trim());
    }

    private static String generateNewToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
