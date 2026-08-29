package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionTaskCreateWebRequest;
import io.github.chenyilei2016.maintain.manager.pojo.entity.DirectoryNode;
import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptParameterSchema;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptTargetSelectionMode;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ExecutionApprovalBindingTest {

    @Test
    void bindsApprovalToExecutionTargetAndConfirmationText() {
        ExecutionTaskCreateWebRequest request = new ExecutionTaskCreateWebRequest();
        request.setService("service");
        request.setEnv("prod");
        request.setScriptId("script-1");
        request.setSelectionMode(ScriptTargetSelectionMode.SPECIFIC);
        request.setInstanceId("node-1");
        ExecutionRequestResolver.ResolvedExecution resolved = new ExecutionRequestResolver.ResolvedExecution(
                new ScriptVO().setScript(new Script().setId("script-1"))
                        .setDirectoryNode(new DirectoryNode().setName("dangerous-script")),
                new ScriptParameterSchema.ResolvedScript(
                        "return 'secret'", "return '******'", "{}", List.of("secret")),
                ScriptTargetSelectionMode.SPECIFIC, List.of(), 30);
        String originalDigest = ExecutionApprovalBinding.digest(request, resolved);

        request.setInstanceId("node-2");

        assertNotEquals(originalDigest, ExecutionApprovalBinding.digest(request, resolved));
        assertEquals("PRODUCTION:service:dangerous-script",
                ExecutionApprovalBinding.confirmationText("service", "dangerous-script"));
    }
}
