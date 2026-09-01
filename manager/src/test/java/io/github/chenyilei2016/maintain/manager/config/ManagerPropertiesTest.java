package io.github.chenyilei2016.maintain.manager.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagerPropertiesTest {
    @Test
    void rejectsUnknownIdentityMode() {
        Binder binder = new Binder(new MapConfigurationPropertySource(
                Map.of("maintain.manager.identity.mode", "UNKNOWN")));

        assertThrows(RuntimeException.class,
                () -> binder.bind("maintain.manager", Bindable.of(ManagerProperties.class)).get());
    }
}
