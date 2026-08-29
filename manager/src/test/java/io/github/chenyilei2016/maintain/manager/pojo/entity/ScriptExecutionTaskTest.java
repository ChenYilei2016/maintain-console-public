package io.github.chenyilei2016.maintain.manager.pojo.entity;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScriptExecutionTaskTest {

    @Test
    void selectsSpecificInstanceAndCapsAllInstanceExecution() {
        List<ServiceInstance> instances = List.of(
                new DefaultServiceInstance("node-1", "service", "127.0.0.1", 8080, false),
                new DefaultServiceInstance("node-2", "service", "127.0.0.1", 8081, false));

        assertEquals("node-2", ScriptTargetSelectionMode.SPECIFIC.select(instances, "node-2", 20)
                .getFirst().getInstanceId());
        assertThrows(RuntimeException.class, () -> ScriptTargetSelectionMode.ALL.select(instances, null, 1));
    }

    @Test
    void aggregatesPartialSuccess() {
        List<ScriptExecutionTargetResult> targets = List.of(
                new ScriptExecutionTargetResult().setStatus(ScriptExecutionTaskStatus.SUCCESS),
                new ScriptExecutionTargetResult().setStatus(ScriptExecutionTaskStatus.FAILED));

        assertEquals(ScriptExecutionTaskStatus.PARTIAL_SUCCESS, ScriptExecutionTaskStatus.aggregate(targets));
    }
}
