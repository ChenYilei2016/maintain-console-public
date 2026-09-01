package io.github.chenyilei2016.maintain.manager.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalLoginAttemptGuardTest {
    @Test
    void boundsAccountFailuresAndClearsThemAfterSuccess() {
        LocalLoginAttemptGuard guard = new LocalLoginAttemptGuard();
        for (int i = 0; i < 10; i++) {
            guard.beforeAuthentication("developer", "127.0.0.1");
            guard.failed("developer");
        }
        assertThrows(RuntimeException.class,
                () -> guard.beforeAuthentication("developer", "127.0.0.1"));

        guard.succeeded("developer");
        assertDoesNotThrow(() -> guard.beforeAuthentication("developer", "127.0.0.1"));
    }
}
