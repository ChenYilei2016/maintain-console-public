package io.github.chenyilei2016.maintain.manager.config;

import io.github.chenyilei2016.maintain.manager.discovery.RegistryDiscoveryMode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author chenyilei
 * @since 2025/08/08 11:28
 */
@Getter
@Setter
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "maintain.manager")
public class ManagerProperties {

    /**
     * 全局白名单
     */
    private Set<String> globalWhiteEmployeeNoList = new HashSet<>(Set.of("0", "1"));

    /**
     * 仅授予制作新工具能力，不等于全局管理员。
     */
    private Set<String> developerEmployeeNoList = new HashSet<>();

    private Execution execution = new Execution();

    private Security security = new Security();

    private Ai ai = new Ai();

    private Discovery discovery = new Discovery();

    private List<TargetEnvironment> targetEnvironments = new ArrayList<>();

    @Getter
    @Setter
    public static class Execution {
        private int targetCorePoolSize = 4;
        private int targetMaxPoolSize = 8;
        private int targetQueueCapacity = 100;
        private int maxTargets = 20;
        private int defaultTimeoutSeconds = 180;
        private int maxTimeoutSeconds = 900;
    }

    @Getter
    @Setter
    public static class Security {
        private String keyId = "default";
        private String privateKey;
        private boolean allowLegacyClients;
        private String legacyPrivateKey;
        private String identitySharedSecret;
        private long identityTimestampToleranceMillis = 300_000L;
        private int identityReplayCacheSize = 10_000;
    }

    @Getter
    @Setter
    public static class Ai {
        private boolean enabled;
        private String endpoint;
        private String apiKey;
        private String model;
        private boolean allowInsecureEndpoint;
        private int connectTimeoutSeconds = 5;
        private int requestTimeoutSeconds = 45;
        private int maxInputCharacters = 100_000;
        private int maxOutputCharacters = 50_000;
    }

    @Getter
    @Setter
    public static class Discovery {
        private RegistryDiscoveryMode mode = RegistryDiscoveryMode.SPRING_CLOUD;
        private int maxServices = 500;
        private List<NacosConnection> nacosConnections = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class NacosConnection {
        private String id;
        private String name;
        private String serverAddr;
        private String namespaceId;
        private String username;
        private String password;
        private String defaultGroup = "DEFAULT_GROUP";
        private boolean secure;
    }

    @Getter
    @Setter
    public static class TargetEnvironment {
        private String value;
        private String name;
        private String cluster = "default";
        private String namespace;
        private String registryId;
        private String groupName;
        private List<String> instanceClusters = new ArrayList<>();
        private String description;
        private boolean allNamespaces;
        private boolean production;
    }

}
