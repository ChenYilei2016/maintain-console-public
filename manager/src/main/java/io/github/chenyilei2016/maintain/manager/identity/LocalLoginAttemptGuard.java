package io.github.chenyilei2016.maintain.manager.identity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对昂贵的 BCrypt 校验做进程内有界保护；多实例部署时应在网关补充共享限流。
 */
@Component
public class LocalLoginAttemptGuard {
    private static final int MAX_ACCOUNT_FAILURES = 10;
    private static final int MAX_SOURCE_ATTEMPTS = 100;
    private static final String LOGIN_FAILURE = "账号或密码错误";

    private final Cache<String, AtomicInteger> accountFailures = CacheBuilder.newBuilder()
            .maximumSize(10_000).expireAfterWrite(10, TimeUnit.MINUTES).build();
    private final Cache<String, AtomicInteger> sourceAttempts = CacheBuilder.newBuilder()
            .maximumSize(10_000).expireAfterWrite(5, TimeUnit.MINUTES).build();

    public void beforeAuthentication(String username, String sourceAddress) {
        AtomicInteger failures = accountFailures.getIfPresent(username);
        if (failures != null && failures.get() >= MAX_ACCOUNT_FAILURES) reject();
        String source = sourceAddress == null || sourceAddress.isBlank() ? "unknown" : sourceAddress;
        if (counter(sourceAttempts, source).incrementAndGet() > MAX_SOURCE_ATTEMPTS) reject();
    }

    public void failed(String username) {
        counter(accountFailures, username).incrementAndGet();
    }

    public void succeeded(String username) {
        accountFailures.invalidate(username);
    }

    private AtomicInteger counter(Cache<String, AtomicInteger> cache, String key) {
        return cache.asMap().computeIfAbsent(key, ignored -> new AtomicInteger());
    }

    private void reject() {
        throw CommonException.createReminderException(LOGIN_FAILURE);
    }
}
