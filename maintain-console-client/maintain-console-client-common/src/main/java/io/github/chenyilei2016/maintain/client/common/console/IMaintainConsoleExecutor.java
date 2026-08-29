package io.github.chenyilei2016.maintain.client.common.console;

import io.github.chenyilei2016.maintain.client.common.dto.RuntimeMetadataDTO;

/**
 * @author chenyilei
 * @since 2024/05/20 14:52
 */
public interface IMaintainConsoleExecutor {

    Object execute(String script);

    default RuntimeMetadataDTO runtimeMetadata() {
        return new RuntimeMetadataDTO();
    }
}
