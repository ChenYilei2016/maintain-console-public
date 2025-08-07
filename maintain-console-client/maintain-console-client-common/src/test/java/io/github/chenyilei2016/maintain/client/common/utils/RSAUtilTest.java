package io.github.chenyilei2016.maintain.client.common.utils;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.github.chenyilei2016.maintain.client.common.utils.RSAUtil.*;

/**
 * @author chenyilei
 * @since 2024/05/20 13:48
 */
public class RSAUtilTest {

    @Test
    public void rsa() {
//首先生成一对公钥&私钥，记得保存下来
        Map<String, String> map = generateKey();
        String privateKey = map.get("privateKey");
        String publicKey = map.get("publicKey");
        System.out.println("私钥 privateKey=" + privateKey);
        System.out.println("公钥 publicKey=" + publicKey);

        //Data must not be longer than 53 bytes
        String str = "测试字符测试字符测试字符测试字符测2";
        System.out.println("测试字符串=" + str);
        System.out.println("测试字符串=" + str.length());
        System.out.println("测试字符串 byte =" + str.getBytes().length);

        System.out.println("*********************** 【方式一：私钥加密 & 公钥解密】 *************************");
        String encrypt = encryptByPrivateKey(str, privateKey);
        System.out.println("【方式一】私钥加密结果 encrypt=" + encrypt);

        String decrypt = decryptByPublicKey(encrypt, publicKey);
        System.out.println("【方式一】公钥解密结果 decrypt=" + decrypt);

        System.out.println("*********************** 【方式二：公钥加密 & 私钥解密】 *************************");
        String encrypt222 = encryptByPublicKey(str, publicKey);
        System.out.println("【方式二】公钥加密结果 encrypt222=" + encrypt222);

        String decrypt222 = decryptByPrivateKey(encrypt222, privateKey);
        System.out.println("【方式二】私钥解密结果 decrypt222=" + decrypt222);
    }


    @Test
    public void signEncrypt() {
        Map<String, String> map = generateKey();
        String privateKey = map.get("privateKey");
        String publicKey = map.get("publicKey");
        System.out.println("私钥 privateKey=" + privateKey);
        System.out.println("公钥 publicKey=" + publicKey);


        System.err.println(null + "23");
        String md5 = MD5("6546456".getBytes(StandardCharsets.UTF_8));
        String x = md5 + "_" + System.currentTimeMillis();
        System.err.println(x); //3B4531574A3CE1A18ACF558C509BD2C9_1716193054596
        System.err.println(x.getBytes(StandardCharsets.UTF_8).length);

        String encryptByPrivateKey = encryptByPrivateKey(x, privateKey);
        System.err.println("encryptByPrivateKey : " + encryptByPrivateKey);

        System.err.println(RSAUtil.decryptByPublicKey(encryptByPrivateKey, publicKey));

    }
}