package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.client.common.utils.RSAUtil;
import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionTaskCreateWebRequest;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ExecutionApprovalBinding {
    private ExecutionApprovalBinding() {
    }

    public static String digest(
            ExecutionTaskCreateWebRequest request,
            ExecutionRequestResolver.ResolvedExecution execution
    ) {
        String canonical = String.join("\n",
                request.getService(), request.getEnv(), request.getScriptId(),
                execution.resolvedScript().executableContent(), execution.selectionMode().name(),
                Objects.toString(request.getInstanceId(), ""), String.valueOf(execution.timeoutSeconds()));
        return RSAUtil.SHA256(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public static String confirmationText(String serviceName, String scriptName) {
        return "PRODUCTION:" + serviceName + ':' + scriptName;
    }
}
