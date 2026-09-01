package io.github.chenyilei2016.maintain.manager.discovery;

import com.alibaba.nacos.api.naming.pojo.Instance;
import io.github.chenyilei2016.maintain.manager.constant.ManagerConstants;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ServiceInstanceDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiNacosRegistryClientDiscoveryTest {
    @Test
    void sameNativeInstanceInDifferentRegistriesNeverSharesAnId() {
        Instance instance = new Instance();
        instance.setInstanceId("same-id");
        instance.setIp("10.0.0.8");
        instance.setPort(8080);
        instance.setMetadata(Map.of("maintain-console-enabled", "true"));

        var test = MultiNacosRegistryClientDiscovery.toServiceInstance("test", "DEFAULT_GROUP", "orders", false, instance);
        var production = MultiNacosRegistryClientDiscovery.toServiceInstance("production", "DEFAULT_GROUP", "orders", true, instance);

        assertNotEquals(test.getInstanceId(), production.getInstanceId());
        assertEquals("test:same-id", test.getInstanceId());
        assertTrue(production.isSecure());
        assertEquals("test", ServiceInstanceDTO.from(test).metadata()
                .get(ManagerConstants.METADATA_REGISTRY_ID));
    }
}
