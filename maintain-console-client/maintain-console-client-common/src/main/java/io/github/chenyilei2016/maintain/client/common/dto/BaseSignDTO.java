package io.github.chenyilei2016.maintain.client.common.dto;

import io.github.chenyilei2016.maintain.client.common.utils.RSAUtil;
import lombok.Data;

import java.io.Serializable;

/**
 * @author chenyilei
 * @since 2024/05/20 11:12
 */
@Data
public abstract class BaseSignDTO implements Serializable {

    private static final long serialVersionUID = -704359603561324839L;

    protected long timestamp = System.currentTimeMillis();

    //如果有的话进行校验
    protected String sign;

    protected Integer signVersion;

    protected String keyId;

    protected String nonce;

    /**
     * 用于作为签名的数据
     */
    public abstract String signData();

    public String signDataV2() {
        return signData();
    }

    public void grantSign(String privateKey) {
        this.timestamp = System.currentTimeMillis();
        this.signVersion = null;
        this.keyId = null;
        this.nonce = null;
        this.sign = RSAUtil.encryptByPrivateKey(signData() + "_" + this.timestamp, privateKey);
    }

    public void grantSignV2(String keyId, String nonce, String privateKey) {
        this.timestamp = System.currentTimeMillis();
        this.signVersion = 2;
        this.keyId = keyId;
        this.nonce = nonce;
        this.sign = RSAUtil.signSha256(signaturePayloadV2(), privateKey);
    }

    public String signaturePayloadV2() {
        return signVersion + "\n" + keyId + "\n" + timestamp + "\n" + nonce + "\n" + signDataV2();
    }

}
