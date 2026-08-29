package io.github.chenyilei2016.maintain.client.registry.properties;

import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientCommonConst;
import org.junit.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class MaintainConsoleRegistryPropertiesTest {
    @Test
    public void explicitNamespaceCanReplaceLegacyPortRouting() {
        MaintainConsoleRegistryProperties properties = new MaintainConsoleRegistryProperties();
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test", Collections.<String, Object>singletonMap("server.port", "8080")));
        properties.setEnvironment(environment);
        properties.setNamespace("orders-prod");
        properties.setUseServerPortAsNamespace(false);

        assertEquals(Integer.valueOf(2), properties.getVersion());
        assertEquals("orders-prod", properties.getUploadMetadata()
                .get(MaintainConsoleClientCommonConst.KEY_NAMESPACE));
    }
}
