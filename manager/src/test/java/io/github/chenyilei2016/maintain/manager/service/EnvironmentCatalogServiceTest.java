package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.discovery.RegistryDiscoveryMode;
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

    @Test
    void multiNacosRequiresEveryEnvironmentToReferenceAKnownConnection() {
        ManagerProperties properties = new ManagerProperties();
        properties.getDiscovery().setMode(RegistryDiscoveryMode.MULTI_NACOS);
        ManagerProperties.NacosConnection connection = new ManagerProperties.NacosConnection();
        connection.setId("test-registry");
        connection.setServerAddr("test-nacos:8848");
        properties.getDiscovery().setNacosConnections(List.of(connection));
        ManagerProperties.TargetEnvironment target = new ManagerProperties.TargetEnvironment();
        target.setValue("test");
        target.setName("测试环境");
        target.setRegistryId("missing");
        properties.setTargetEnvironments(List.of(target));

        EnvironmentCatalogService catalog = new EnvironmentCatalogService(properties, new MockEnvironment());
        assertThrows(IllegalStateException.class, catalog::validateConfiguration);
        target.setRegistryId("test-registry");
        assertDoesNotThrow(catalog::validateConfiguration);
    }
}
