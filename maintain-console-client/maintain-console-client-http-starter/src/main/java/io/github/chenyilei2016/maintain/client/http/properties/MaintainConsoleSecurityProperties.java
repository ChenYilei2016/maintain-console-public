package io.github.chenyilei2016.maintain.client.http.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "maintain.console.security")
public class MaintainConsoleSecurityProperties {
    private boolean allowLegacySignatures = true;
    private long timestampToleranceMillis = 300_000L;
    private int replayCacheSize = 10_000;
    private Map<String, String> publicKeys = new HashMap<>();
}
