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

    /**
     * 用于作为签名的数据
     */
    public abstract String signData();

    public void grantSign(String privateKey) {
        this.sign = RSAUtil.encryptByPrivateKey(signData() + "_" + this.timestamp, privateKey);
    }

}
