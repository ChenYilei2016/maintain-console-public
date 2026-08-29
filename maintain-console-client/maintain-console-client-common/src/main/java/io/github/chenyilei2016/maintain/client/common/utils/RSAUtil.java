package io.github.chenyilei2016.maintain.client.common.utils;

import io.github.chenyilei2016.maintain.client.common.dto.BaseSignDTO;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author chenyilei
 * @since 2024/05/20 11:34
 */
public class RSAUtil {

    private static final String RSA_SIGN_PUBLIC_KEY = "MFwwDQYJKoZIhvcNAQEBBQADSwA" + "wSAJBAPRcsqKmrL3F2sRc3a6XcOn" + "iR5yk1IxAUFFLePQHl/pz9TMxxML4oV+W" + "vefbztXa5v3l7XTX+i2Scy1MsHE/ZxsCAwEAAQ==";

    public static void checkSignValid(Object dto, Long timeElapsedLimitMills) {
        if (!(dto instanceof BaseSignDTO)) {
            return;
        }

        BaseSignDTO baseSignDTO = (BaseSignDTO) dto;
        String sign = baseSignDTO.getSign();
        if (StringUtils.isEmpty(sign)) {
            throw new IllegalArgumentException("request sign is empty");
        }
        long timestamp = 0;
        String checkDataSign;
        try {
            String md5WithTimestamp = RSAUtil.decryptByPublicKey(sign, RSA_SIGN_PUBLIC_KEY);
            if (md5WithTimestamp == null) {
                throw new IllegalArgumentException("request sign is empty");
            }
            String[] split = StringUtils.split(md5WithTimestamp, "_");
            if (split == null || split.length != 2) {
                throw new IllegalArgumentException("request sign verify error, 解密后的数据非法");
            }
            checkDataSign = split[0];
            timestamp = Long.parseLong(split[1]);
        } catch (Throwable e) {
            throw new IllegalArgumentException("request sign verify error, " + e.getMessage());
        }

        if (!baseSignDTO.signData().equals(checkDataSign)) {
            throw new IllegalArgumentException("request sign verify error, dataSign not match");
        }

        if (timeElapsedLimitMills == null) {
            timeElapsedLimitMills = 5 * 60 * 1000L; // 5 minutes
        }
        if (Math.abs(System.currentTimeMillis() - timestamp) > timeElapsedLimitMills) {
            throw new IllegalStateException("request sign time check error");
        }
    }

    public static String signSha256(String data, String privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(readPrivateKey(privateKey));
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("RSA private key or signature is invalid", e);
        }
    }

    public static boolean verifySha256(String data, String sign, String publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(readPublicKey(publicKey));
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private static PrivateKey readPrivateKey(String privateKey) throws GeneralSecurityException {
        byte[] encoded = Base64.getDecoder().decode(privateKey);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    private static PublicKey readPublicKey(String publicKey) throws GeneralSecurityException {
        byte[] encoded = Base64.getDecoder().decode(publicKey);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
    }

    public static String MD5(byte[] data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(data);
            return new BigInteger(messageDigest.digest()).toString(16).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String SHA256(byte[] data) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /**
     * 最大加密长度限制
     */
    public static final int MAX_ENCRYPT_LENGTH = 117;

    /**
     * 最大解密长度限制
     */
    public static final int MAX_DECRYPT_LENGTH = 128;


    /**
     * 生成一对公钥&私钥（仅供测试使用，实际应用上只能生成一次，然后把公钥和私钥保存下来。切忌在业务中每次都调用此方法，否则造成秘钥丢失，数据不可解密！！！）
     *
     * @return
     */
    public static Map<String, String> generateKey() {
        Map<String, String> map = new HashMap<>();
        try {
            // 生成密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicObj = keyPair.getPublic();
            PrivateKey privateObj = keyPair.getPrivate();
            String publicKey = Base64.getEncoder().encodeToString(publicObj.getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(privateObj.getEncoded());
            map.put("publicKey", publicKey);
            map.put("privateKey", privateKey);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("RSA key pair generation failed", e);
        }
        return map;
    }


    /****************************** 【方式一：私钥加密 & 公钥解密】  ***********************************************/
    /**
     * 【方式一】私钥加密
     *
     * @param str        待加密字符串
     * @param privateKey 私钥
     * @return
     */
    public static String encryptByPrivateKey(String str, String privateKey) {
        try {
            byte[] encoded = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateObj = keyFactory.generatePrivate(privateKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, privateObj);

            //加密的字符串长度不能超过 117 个字节，否则报错，需要分段处理
            byte[] resultBytes = getEncryptResult(str, cipher, rsaKeyBytes(privateObj) - 11);
            return Base64.getEncoder().encodeToString(resultBytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("RSA private key encryption failed", e);
        }
    }

    /**
     * 【方式一】公钥解密
     *
     * @param str       待解密字符串
     * @param publicKey 公钥
     * @return
     */
    public static String decryptByPublicKey(String str, String publicKey) {
        try {
            byte[] encoded = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicObj = keyFactory.generatePublic(publicKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicObj);
            byte[] decryptedBytes = getDecryptResult(str, cipher, rsaKeyBytes(publicObj));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("RSA public key decryption failed", e);
        }
    }


    /****************************** 【方式二：公钥加密 & 私钥解密】  ***********************************************/
    /**
     * 【方式二】公钥加密
     *
     * @return
     */
    public static String encryptByPublicKey(String str, String publicKey) {
        try {
            byte[] encoded = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicObj = keyFactory.generatePublic(publicKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicObj);

            //加密的字符串长度不能超过 117 个字节，否则报错，需要分段处理
            byte[] resultBytes = getEncryptResult(str, cipher, rsaKeyBytes(publicObj) - 11);
            return Base64.getEncoder().encodeToString(resultBytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("RSA public key encryption failed", e);
        }
    }


    /**
     * 【方式二】私钥解密
     *
     * @return
     */
    public static String decryptByPrivateKey(String str, String privateKey) {
        try {
            byte[] encoded = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateObj = keyFactory.generatePrivate(privateKeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateObj);
            byte[] decryptedBytes = getDecryptResult(str, cipher, rsaKeyBytes(privateObj));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("RSA private key decryption failed", e);
        }
    }


    /**
     * 加密的字符串长度不能超过 117 个字节，否则报错，需要分段处理
     *
     * @return
     */
    private static byte[] getEncryptResult(String str, Cipher cipher, int maxEncryptLength) throws GeneralSecurityException {
        byte[] originalBytes = str.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        for (int offset = 0; offset < originalBytes.length; offset += maxEncryptLength) {
            int length = Math.min(maxEncryptLength, originalBytes.length - offset);
            byte[] block = cipher.doFinal(originalBytes, offset, length);
            result.write(block, 0, block.length);
        }
        return result.toByteArray();
    }

    /**
     * 解密的字符串长度不能超过 128 个字节，否则报错，需要分段处理
     *
     * @return
     * @throws Exception
     */
    private static byte[] getDecryptResult(String str, Cipher cipher, int maxDecryptLength) throws GeneralSecurityException {
        byte[] decryptedBytes = Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        for (int offset = 0; offset < decryptedBytes.length; offset += maxDecryptLength) {
            int length = Math.min(maxDecryptLength, decryptedBytes.length - offset);
            byte[] block = cipher.doFinal(decryptedBytes, offset, length);
            result.write(block, 0, block.length);
        }
        return result.toByteArray();
    }

    private static int rsaKeyBytes(Key key) {
        return (((RSAKey) key).getModulus().bitLength() + 7) / 8;
    }

}
