package io.github.chenyilei2016.maintain.client.http.security;

import io.github.chenyilei2016.maintain.client.common.dto.BaseSignDTO;
import io.github.chenyilei2016.maintain.client.common.utils.RSAUtil;
import io.github.chenyilei2016.maintain.client.http.properties.MaintainConsoleSecurityProperties;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestSignatureVerifier {
    private final MaintainConsoleSecurityProperties properties;
    private final Map<String, Long> acceptedNonces = new ConcurrentHashMap<>();

    public RequestSignatureVerifier(MaintainConsoleSecurityProperties properties) {
        this.properties = properties;
    }

    public void verify(BaseSignDTO request) {
        if (!Integer.valueOf(2).equals(request.getSignVersion())) {
            if (!properties.isAllowLegacySignatures()) {
                throw new IllegalArgumentException("legacy request signatures are disabled");
            }
            RSAUtil.checkSignValid(request, properties.getTimestampToleranceMillis());
            return;
        }

        long now = System.currentTimeMillis();
        if (Math.abs(now - request.getTimestamp()) > properties.getTimestampToleranceMillis()) {
            throw new IllegalArgumentException("request timestamp is outside the allowed window");
        }
        if (!StringUtils.hasText(request.getNonce()) || request.getNonce().length() < 16
                || request.getNonce().length() > 128) {
            throw new IllegalArgumentException("request nonce is invalid");
        }
        if (!StringUtils.hasText(request.getSign()) || !StringUtils.hasText(request.getKeyId())) {
            throw new IllegalArgumentException("request signature metadata is incomplete");
        }
        String publicKey = properties.getPublicKeys().get(request.getKeyId());
        if (!StringUtils.hasText(publicKey)) {
            throw new IllegalArgumentException("request signing key is unknown");
        }
        if (!RSAUtil.verifySha256(request.signaturePayloadV2(), request.getSign(), publicKey)) {
            throw new IllegalArgumentException("request signature is invalid");
        }

        if (acceptedNonces.size() >= properties.getReplayCacheSize()) {
            long expiredBefore = now - properties.getTimestampToleranceMillis();
            acceptedNonces.entrySet().removeIf(entry -> entry.getValue() < expiredBefore);
        }
        if (acceptedNonces.size() >= properties.getReplayCacheSize()) {
            throw new IllegalStateException("request replay cache is full");
        }
        String replayKey = request.getKeyId() + ':' + request.getNonce();
        if (acceptedNonces.putIfAbsent(replayKey, request.getTimestamp()) != null) {
            throw new IllegalArgumentException("request replay detected");
        }
    }
}
