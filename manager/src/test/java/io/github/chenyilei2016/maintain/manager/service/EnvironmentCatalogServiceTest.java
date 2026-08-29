package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentCatalogServiceTest {
    @Test
    void resolvesConfiguredNamespaceAndProductionBoundary() {
        ManagerProperties properties = new ManagerProperties();
        ManagerProperties.TargetEnvironment production = new ManagerProperties.TargetEnvironment();
        production.setValue("prod-cn");
        production.setName("生产华东");
        production.setNamespace("prod-namespace");
        production.setProduction(true);
        properties.setTargetEnvironments(List.of(production));

        EnvironmentCatalogService catalog = new EnvironmentCatalogService(properties, new MockEnvironment());
        catalog.validateConfiguration();

        assertEquals("prod-namespace", catalog.namespace("prod-cn"));
        assertTrue(catalog.isProduction("prod-cn"));
    }

    @Test
    void keepsLegacyRandomFallbackWhenNoCatalogIsConfigured() {
        EnvironmentCatalogService catalog = new EnvironmentCatalogService(
                new ManagerProperties(), new MockEnvironment().withProperty("spring.profiles.active", "local"));

        assertEquals("random", catalog.list().getFirst().getValue());
        assertNull(catalog.namespace("random"));
    }
}
