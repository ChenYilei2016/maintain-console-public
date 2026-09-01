package io.github.chenyilei2016.maintain.manager.identity;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 可运行的 SDK 替身；真实 SDK 接入后再抽取两个实现共同需要的 Interface。
 */
@Component
@Profile({"local", "demo"})
public class MockLoginProvider {
    public ExternalIdentity authenticate(String accountId) {
        return MockLoginAccount.require(accountId).verifiedIdentity();
    }
}
