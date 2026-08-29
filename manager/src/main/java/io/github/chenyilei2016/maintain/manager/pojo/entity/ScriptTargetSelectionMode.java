package io.github.chenyilei2016.maintain.manager.pojo.entity;

import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public enum ScriptTargetSelectionMode {
    RANDOM {
        @Override
        public List<ServiceInstance> select(List<ServiceInstance> instances, String instanceId, int maxTargets) {
            requireAvailable(instances);
            return List.of(instances.get(ThreadLocalRandom.current().nextInt(instances.size())));
        }
    },
    SPECIFIC {
        @Override
        public List<ServiceInstance> select(List<ServiceInstance> instances, String instanceId, int maxTargets) {
            requireAvailable(instances);
            if (instanceId == null || instanceId.isBlank()) {
                throw CommonException.createReminderException("指定实例执行时 instanceId 不能为空");
            }
            return instances.stream()
                    .filter(instance -> Objects.equals(ServiceInstanceDTO.idOf(instance), instanceId))
                    .findFirst()
                    .map(List::of)
                    .orElseThrow(() -> CommonException.createReminderException("指定的服务实例不存在或已下线"));
        }
    },
    ALL {
        @Override
        public List<ServiceInstance> select(List<ServiceInstance> instances, String instanceId, int maxTargets) {
            requireAvailable(instances);
            if (instances.size() > maxTargets) {
                throw CommonException.createReminderException("可用实例数 {} 超过单次执行上限 {}", instances.size(), maxTargets);
            }
            return List.copyOf(instances);
        }
    };

    public abstract List<ServiceInstance> select(List<ServiceInstance> instances, String instanceId, int maxTargets);

    private static void requireAvailable(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            throw CommonException.createReminderException("无可用的服务实例");
        }
    }
}
