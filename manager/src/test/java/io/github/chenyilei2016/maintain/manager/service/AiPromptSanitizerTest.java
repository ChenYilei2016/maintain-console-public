package io.github.chenyilei2016.maintain.manager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiPromptSanitizerTest {
    @Test
    void masksCommonSecretsBeforeCallingModel() {
        String input = "password='p@ss' token=abc Authorization: Bearer xyz\n"
                + "-----BEGIN PRIVATE KEY-----\nsecret-key\n-----END PRIVATE KEY-----";

        String sanitized = AiPromptSanitizer.sanitize(input);

        assertEquals(4, sanitized.split("REDACTED", -1).length - 1);
        assertFalse(sanitized.contains("p@ss"));
        assertFalse(sanitized.contains("abc"));
        assertFalse(sanitized.contains("xyz"));
        assertFalse(sanitized.contains("secret-key"));
    }
}
