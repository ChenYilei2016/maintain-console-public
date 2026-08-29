package io.github.chenyilei2016.maintain.manager.security;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TrustedIdentityVerifier {
    private final byte[] sharedSecret;
    private final long timestampToleranceMillis;
    private final int replayCacheSize;
    private final Map<String, Long> acceptedNonces = new ConcurrentHashMap<>();

    public TrustedIdentityVerifier(ManagerProperties.Security security) {
        String secret = security.getIdentitySharedSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("非本地环境必须配置至少 32 字符的身份签名密钥");
        }
        this.sharedSecret = secret.getBytes(StandardCharsets.UTF_8);
        this.timestampToleranceMillis = security.getIdentityTimestampToleranceMillis();
        this.replayCacheSize = security.getIdentityReplayCacheSize();
        if (timestampToleranceMillis <= 0 || replayCacheSize <= 0) {
            throw new IllegalStateException("身份签名时间窗口和防重放容量必须大于 0");
        }
    }

    public LocalLoginUser verify(
            String userId,
            String userName,
            String roles,
            String timestamp,
            String nonce,
            String signature,
            String method,
            String requestUri
    ) {
        if (userId == null || !userId.matches("[A-Za-z0-9._@-]{1,128}") || userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("身份头不完整");
        }
        if (nonce == null || nonce.length() < 16 || nonce.length() > 128 || signature == null) {
            throw new IllegalArgumentException("身份签名元数据不完整");
        }
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("身份时间戳无效");
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - requestTime) > timestampToleranceMillis) {
            throw new IllegalArgumentException("身份签名已过期");
        }
        String payload = String.join("\n", userId, userName, roles == null ? "" : roles,
                timestamp, nonce, method, requestUri);
        if (!MessageDigest.isEqual(hmac(payload), decode(signature))) {
            throw new IllegalArgumentException("身份签名无效");
        }
        if (acceptedNonces.size() >= replayCacheSize) {
            long expiredBefore = now - timestampToleranceMillis;
            acceptedNonces.entrySet().removeIf(entry -> entry.getValue() < expiredBefore);
        }
        if (acceptedNonces.size() >= replayCacheSize || acceptedNonces.putIfAbsent(nonce, requestTime) != null) {
            throw new IllegalArgumentException("身份签名重放");
        }

        LocalLoginUser user = new LocalLoginUser();
        user.setEmployeeNo(userId);
        user.setEmployeeName(userName);
        user.setRoles(parseRoles(roles));
        return user;
    }

    byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sharedSecret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is unavailable", e);
        }
    }

    private byte[] decode(String signature) {
        try {
            return Base64.getUrlDecoder().decode(signature);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }

    private Set<String> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> role.matches("[A-Za-z0-9_-]{1,64}"))
                .map(role -> role.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
