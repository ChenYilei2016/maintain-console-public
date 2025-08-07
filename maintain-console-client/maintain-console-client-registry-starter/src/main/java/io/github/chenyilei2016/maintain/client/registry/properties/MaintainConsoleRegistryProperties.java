package io.github.chenyilei2016.maintain.client.registry.properties;

import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientCommonConst;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

import static io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientCommonConst.KEY_DEFAULT_NAMESPACE;

/**
 * @author chenyilei
 * @since 2024/05/16 17:47
 */
@Data
@ConfigurationProperties(prefix = MaintainConsoleClientCommonConst.KEY_PROPERTY_PREFIX)
public class MaintainConsoleRegistryProperties implements EnvironmentAware {
    private Environment environment;

    public static final String KEY_REGISTRY_ENABLED = MaintainConsoleClientCommonConst.KEY_REGISTRY_ENABLED;

    public static final String KEY_REGISTRY_VERSION = MaintainConsoleClientCommonConst.KEY_REGISTRY_VERSION;

    public static final String KEY_NAMESPACE = MaintainConsoleClientCommonConst.KEY_NAMESPACE;

    private Boolean enabled = false;

    private Integer version = 1;

    /**
     * 决定用户选择的环境 能否调用到自己, 目前会优先读取 server.port 用端口来判断路由
     */
    private String namespace = KEY_DEFAULT_NAMESPACE;

    public Map<String, String> getUploadMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(KEY_REGISTRY_ENABLED, enabled.toString());
        metadata.put(KEY_REGISTRY_VERSION, version.toString());


        String serverPort = environment.getProperty("server.port");
        String inputNameSpace = namespace;
        if (serverPort != null) {
            inputNameSpace = serverPort;
        }
        metadata.put(KEY_NAMESPACE, inputNameSpace);
        return metadata;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
