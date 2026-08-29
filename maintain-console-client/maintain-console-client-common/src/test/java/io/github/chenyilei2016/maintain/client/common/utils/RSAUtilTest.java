package io.github.chenyilei2016.maintain.client.common.utils;

import org.junit.Test;

import java.util.Map;

import static io.github.chenyilei2016.maintain.client.common.utils.RSAUtil.*;
import static org.junit.Assert.assertEquals;

/**
 * @author chenyilei
 * @since 2024/05/20 13:48
 */
public class RSAUtilTest {

    @Test
    public void rsa() {
        Map<String, String> map = generateKey();
        String privateKey = map.get("privateKey");
        String publicKey = map.get("publicKey");
        String str = "测试字符测试字符测试字符测试字符测2";

        assertEquals(str, decryptByPublicKey(encryptByPrivateKey(str, privateKey), publicKey));
        assertEquals(str, decryptByPrivateKey(encryptByPublicKey(str, publicKey), privateKey));
    }


    @Test
    public void signEncrypt() {
        Map<String, String> map = generateKey();
        String privateKey = map.get("privateKey");
        String publicKey = map.get("publicKey");
        String value = "request-digest_" + System.currentTimeMillis();

        assertEquals(value, RSAUtil.decryptByPublicKey(encryptByPrivateKey(value, privateKey), publicKey));
    }
}
