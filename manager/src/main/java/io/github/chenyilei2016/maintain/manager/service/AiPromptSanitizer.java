package io.github.chenyilei2016.maintain.manager.service;

import java.util.regex.Pattern;

final class AiPromptSanitizer {
    private static final Pattern PRIVATE_KEY = Pattern.compile(
            "-----BEGIN [^-]*PRIVATE KEY-----.*?-----END [^-]*PRIVATE KEY-----", Pattern.DOTALL);
    private static final Pattern QUOTED_SECRET = Pattern.compile(
            "(?i)(\\b(?:password|passwd|token|secret|api[_-]?key)\\b\\s*[:=]\\s*)(['\"])(.*?)\\2");
    private static final Pattern PLAIN_SECRET = Pattern.compile(
            "(?i)(\\b(?:password|passwd|token|secret|api[_-]?key)\\b\\s*[:=]\\s*)([^'\"\\s,;}\\]]+)");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(Authorization\\s*:\\s*Bearer\\s+)[^\\s]+");

    private AiPromptSanitizer() {
    }

    static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String sanitized = PRIVATE_KEY.matcher(value).replaceAll("[REDACTED_PRIVATE_KEY]");
        sanitized = QUOTED_SECRET.matcher(sanitized).replaceAll("$1$2[REDACTED]$2");
        sanitized = PLAIN_SECRET.matcher(sanitized).replaceAll("$1[REDACTED]");
        return BEARER_TOKEN.matcher(sanitized).replaceAll("$1[REDACTED]");
    }
}
